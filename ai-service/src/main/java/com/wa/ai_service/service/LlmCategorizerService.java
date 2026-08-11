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

        String categoriasContexto = categorias.isEmpty()
                ? "- Nenhuma categoria cadastrada"
                : categorias.stream()
                        .map(categoria -> "- " + categoria)
                        .collect(Collectors.joining("\n"));

        String telefoneMascarado = telefone == null || telefone.length() < 4
                ? "****"
                : "****" + telefone.substring(telefone.length() - 4);

        if (categorias.isEmpty()) {
            log.warn("Nenhuma categoria encontrada para o telefone {}", telefoneMascarado);
        } else {
            log.info("Categorias disponÃ­veis para {}: {}", telefoneMascarado, categorias);
        }

        String systemPrompt = """
            VocÃª extrai dados de gastos e classifica cada gasto em exatamente uma categoria.

            CATEGORIAS DISPONÃVEIS:
            {categorias_validas}

            REGRAS OBRIGATÃ“RIAS:
            1. Entenda o significado da compra, e nÃ£o apenas palavras exatas.
            2. Escolha a categoria semanticamente mais prÃ³xima entre as categorias disponÃ­veis.
            3. Retorne em categoriaNome exatamente o nome apresentado na lista, preservando sua escrita.
            4. Use "Outros" somente quando nÃ£o houver nenhuma relaÃ§Ã£o razoÃ¡vel com uma categoria disponÃ­vel.
            5. NÃ£o crie categoria nova, exceto quando o usuÃ¡rio pedir explicitamente para criar uma.
            6. Escolha somente uma categoria.

            EXEMPLOS DE RACIOCÃNIO SEMÃ‚NTICO:
            - "Fui a um jogo de futebol e gastei 50 reais" -> Lazer, se Lazer estiver disponÃ­vel.
            - "Joguei bola" ou "paguei a pelada" -> Lazer, se Lazer estiver disponÃ­vel.
            - "AlmoÃ§o no restaurante Rancho Fundo" -> AlimentaÃ§Ã£o, se AlimentaÃ§Ã£o estiver disponÃ­vel.
            - "Pedi um lanche" ou "jantar no restaurante" -> AlimentaÃ§Ã£o, se AlimentaÃ§Ã£o estiver disponÃ­vel.
            - "Paguei a conta de luz ou Ã¡gua" -> Moradia, se Moradia estiver disponÃ­vel.
            - "Peguei Uber" ou "abasteci o carro" -> Transporte, se Transporte estiver disponÃ­vel.

            Extraia tambÃ©m valor e descriÃ§Ã£o. O telefone do usuÃ¡rio Ã© {telefone_usuario}.
            """;

        TransacaoExtraidaDTO transacaoExtraida = chatClient.prompt()
                .system(sp -> sp.text(systemPrompt)
                        .param("categorias_validas", categoriasContexto)
                        .param("telefone_usuario", telefone))
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
