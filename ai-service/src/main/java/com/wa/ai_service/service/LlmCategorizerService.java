package com.wa.ai_service.service;

import com.wa.ai_service.client.FinancialClient;
import com.wa.ai_service.dto.TransacaoRequestDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LlmCategorizerService {

    private final ChatClient chatClient;
    private final FinancialClient financialClient;

    public LlmCategorizerService(ChatClient.Builder chatClientBuilder, FinancialClient financialClient) {
        this.chatClient = chatClientBuilder.build();
        this.financialClient = financialClient;
    }

    public TransacaoRequestDTO extrairTransacao(String telefone, String mensagemUsuario) {

        List<String> categorias = financialClient.buscarCategoriasPorTelefone(telefone);

        String categoriasContexto = categorias.isEmpty() ?
                "Nenhuma categoria cadastrada ainda." :
                String.join(", ", categorias);

        String systemPrompt = """
            Você é um assistente financeiro inteligente. Sua tarefa é analisar o texto do usuário \
            e extrair os dados do gasto.
            
            REGRAS DE CATEGORIZAÇÃO:
            O usuário já possui as seguintes categorias: {categorias_validas}.
            Tente encaixar o gasto em uma dessas categorias. 
            Se o gasto NÃO se encaixar em nenhuma delas, ou se o usuário pedir explicitamente \
            para usar uma categoria nova, você tem permissão para criar um nome curto e \
            apropriado para uma nova categoria.
            
            O telefone do usuário é {telefone_usuario}. Mantenha este telefone no objeto de retorno.
            """;

        return chatClient.prompt()
                .system(sp -> sp.text(systemPrompt)
                        .param("categorias_validas", categoriasContexto)
                        .param("telefone_usuario", telefone))
                .user(mensagemUsuario)
                .call()
                .entity(TransacaoRequestDTO.class);
    }
}