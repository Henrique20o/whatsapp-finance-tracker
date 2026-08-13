package com.wa.ai_service.service;

import com.wa.ai_service.client.FinancialClient;
import com.wa.ai_service.dto.TransacaoExtraidaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LlmCategorizerService {

    static final String SYSTEM_PROMPT = """
            Você extrai os dados de uma mensagem que registra um gasto e classifica esse gasto em exatamente uma categoria.

            CATEGORIAS ATIVAS PERMITIDAS:
            {categorias_validas}

            REGRAS OBRIGATÓRIAS:
            1. Entenda o significado da compra, não apenas palavras exatas.
            2. Escolha a categoria semanticamente mais próxima entre as categorias ativas permitidas.
            3. Retorne em categoriaNome exatamente um nome apresentado na lista, preservando sua escrita.
            4. Nunca crie, sugira, renomeie ou adapte uma categoria neste fluxo.
            5. Mesmo que a mensagem peça para criar uma categoria, não a crie: criação de categorias pertence a outro fluxo da aplicação.
            6. Use "Outros" somente quando não houver relação razoável com nenhuma outra categoria permitida.
            7. Escolha somente uma categoria.

            EXEMPLOS DE RACIOCÍNIO SEMÂNTICO:
            - "Fui a um jogo de futebol e gastei 50 reais" -> Lazer, se Lazer estiver disponível.
            - "Joguei bola" ou "paguei a pelada" -> Lazer, se Lazer estiver disponível.
            - "Almoço no restaurante Rancho Fundo" -> Alimentação, se Alimentação estiver disponível.
            - "Pedi um lanche" ou "jantar no restaurante" -> Alimentação, se Alimentação estiver disponível.
            - "Paguei a conta de luz ou água" -> Contas domésticas, se Contas domésticas estiver disponível.
            - "Peguei Uber" ou "abasteci o carro" -> Transporte, se Transporte estiver disponível.

            Extraia também o valor e uma descrição curta e fiel ao gasto.
            """;

    private final ChatClient chatClient;
    private final FinancialClient financialClient;

    public LlmCategorizerService(ChatClient.Builder chatClientBuilder, FinancialClient financialClient) {
        this.chatClient = chatClientBuilder.build();
        this.financialClient = financialClient;
    }

    public TransacaoExtraidaDTO extrairTransacao(String telefone, String mensagemUsuario) {
        List<String> categoriasRecebidas = financialClient.buscarCategoriasPorTelefone(telefone);
        List<String> categorias = categoriasRecebidas == null
                ? List.of()
                : categoriasRecebidas.stream()
                        .filter(categoria -> categoria != null && !categoria.isBlank())
                        .toList();

        if (categorias.isEmpty()) {
            throw new IllegalStateException("Nenhuma categoria disponível para classificar a transação");
        }

        String categoriasContexto = categorias.stream()
                .map(categoria -> "- " + categoria)
                .collect(Collectors.joining("\n"));

        log.info("Catálogo carregado para classificação: {} categoria(s)", categorias.size());

        TransacaoExtraidaDTO transacaoExtraida = chatClient.prompt()
                .system(sp -> sp.text(SYSTEM_PROMPT)
                        .param("categorias_validas", categoriasContexto))
                .user(mensagemUsuario)
                .call()
                .entity(TransacaoExtraidaDTO.class);

        if (transacaoExtraida == null) {
            throw new IllegalStateException("A IA não retornou uma transação válida");
        }

        String categoriaValidada = validarCategoria(transacaoExtraida.categoriaNome(), categorias);

        return new TransacaoExtraidaDTO(
                telefone,
                transacaoExtraida.valor(),
                transacaoExtraida.descricao(),
                categoriaValidada
        );
    }

    static String validarCategoria(String categoriaSugerida, List<String> categoriasDisponiveis) {
        if (categoriasDisponiveis == null || categoriasDisponiveis.isEmpty()) {
            throw new IllegalStateException("Nenhuma categoria disponível para validação");
        }

        if (categoriaSugerida != null) {
            String categoriaNormalizada = categoriaSugerida.trim();
            return categoriasDisponiveis.stream()
                    .filter(categoria -> categoria.equalsIgnoreCase(categoriaNormalizada))
                    .findFirst()
                    .orElseGet(() -> buscarCategoriaOutros(categoriasDisponiveis));
        }

        return buscarCategoriaOutros(categoriasDisponiveis);
    }

    private static String buscarCategoriaOutros(List<String> categoriasDisponiveis) {
        return categoriasDisponiveis.stream()
                .filter(categoria -> categoria.equalsIgnoreCase("Outros"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "A categoria retornada pela IA não pertence ao catálogo do usuário"
                ));
    }
}
