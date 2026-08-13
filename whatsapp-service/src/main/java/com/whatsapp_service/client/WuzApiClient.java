package com.whatsapp_service.client;

import com.whatsapp_service.dto.SendTextRequest;
import com.whatsapp_service.dto.SendTemplateRequest;
import com.whatsapp_service.dto.SendImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WuzApiClient {

    private final RestClient.Builder restClientBuilder;


    @Value("${wuzapi.url}")
    private String url;

    @Value("${wuzapi.token}")
    private String token;


    public void enviarMensagem(String telefone, String mensagem) {

        RestClient client = restClientBuilder
                .baseUrl(url)
                .build();


        client.post()
                .uri("/chat/send/text")
                .header("Token", token)
                .body(
                        new SendTextRequest(
                                telefone,
                                mensagem
                        )
                )
                .retrieve()
                .toBodilessEntity();


        log.info("Mensagem enviada pelo WuzAPI");
    }

    public void enviarMenuPrincipal(String telefone) {
        RestClient client = restClientBuilder
                .baseUrl(url)
                .build();

        SendTemplateRequest menu = new SendTemplateRequest(
                telefone,
                "O que você deseja fazer?",
                "Controle Financeiro",
                "Escolha uma opção",
                List.of(
                        new SendTemplateRequest.Button("reply", "Registrar gasto", "registrar_gasto"),
                        new SendTemplateRequest.Button("reply", "Ver relatório", "ver_relatorio"),
                        new SendTemplateRequest.Button("reply", "Mais opções", "mais_opcoes")
                )
        );

        client.post()
                .uri("/chat/send/buttons")
                .header("Token", token)
                .body(menu)
                .retrieve()
                .toBodilessEntity();

        log.info("Menu principal enviado pelo WuzAPI");
    }

    public void enviarMenuMaisOpcoes(String telefone) {
        RestClient client = restClientBuilder
                .baseUrl(url)
                .build();

        SendTemplateRequest menu = new SendTemplateRequest(
                telefone,
                "Escolha uma opção:",
                "Mais opções",
                "Controle Financeiro",
                List.of(
                        new SendTemplateRequest.Button("reply", "Gerenciar categorias", "gerenciar_categorias"),
                        new SendTemplateRequest.Button("reply", "Ajuda", "ajuda"),
                        new SendTemplateRequest.Button("reply", "Voltar ao menu", "voltar_menu")
                )
        );

        client.post()
                .uri("/chat/send/buttons")
                .header("Token", token)
                .body(menu)
                .retrieve()
                .toBodilessEntity();

        log.info("Menu de opções enviado pelo WuzAPI");
    }

    public void enviarMenuCategorias(String telefone) {
        RestClient client = restClientBuilder.baseUrl(url).build();
        SendTemplateRequest menu = new SendTemplateRequest(
                telefone,
                "O que deseja fazer com suas categorias?",
                "Categorias",
                "Controle Financeiro",
                List.of(
                        new SendTemplateRequest.Button("reply", "Listar categorias", "listar_categorias"),
                        new SendTemplateRequest.Button("reply", "Criar categoria", "criar_categoria"),
                        new SendTemplateRequest.Button("reply", "Desativar categoria", "desativar_categoria"),
                        new SendTemplateRequest.Button("reply", "Voltar ao menu", "voltar_menu")
                )
        );

        client.post()
                .uri("/chat/send/buttons")
                .header("Token", token)
                .body(menu)
                .retrieve()
                .toBodilessEntity();

        log.info("Menu de categorias enviado pelo WuzAPI");
    }

    public void enviarConfirmacaoDesativacaoCategoria(String telefone, String categoria) {
        RestClient client = restClientBuilder.baseUrl(url).build();
        SendTemplateRequest confirmacao = new SendTemplateRequest(
                telefone,
                "Deseja realmente desativar a categoria *" + categoria + "*?",
                "Confirmar desativação",
                "As transações antigas serão preservadas",
                List.of(
                        new SendTemplateRequest.Button(
                                "reply",
                                "Confirmar",
                                "confirmar_desativacao_categoria"
                        ),
                        new SendTemplateRequest.Button("reply", "Cancelar", "cancelar_fluxo")
                )
        );

        client.post()
                .uri("/chat/send/buttons")
                .header("Token", token)
                .body(confirmacao)
                .retrieve()
                .toBodilessEntity();

        log.info("Confirmação de desativação de categoria enviada pelo WuzAPI");
    }

    public void enviarImagem(String telefone, String legenda, String imagemBase64) {
        RestClient client = restClientBuilder
                .baseUrl(url)
                .build();

        client.post()
                .uri("/chat/send/image")
                .header("Token", token)
                .body(new SendImageRequest(telefone, legenda, imagemBase64))
                .retrieve()
                .toBodilessEntity();

        log.info("Imagem enviada pelo WuzAPI");
    }

    public void enviarConfirmacaoComCancelamento(
            String telefone,
            String mensagem,
            Long transacaoId
    ) {
        RestClient client = restClientBuilder
                .baseUrl(url)
                .build();

        SendTemplateRequest confirmacao = new SendTemplateRequest(
                telefone,
                mensagem,
                "Gasto registrado",
                "Use o botão abaixo caso queira desfazer este lançamento",
                List.of(new SendTemplateRequest.Button(
                        "reply",
                        "✖ Cancelar",
                        "cancelar_transacao_" + transacaoId
                ))
        );

        client.post()
                .uri("/chat/send/buttons")
                .header("Token", token)
                .body(confirmacao)
                .retrieve()
                .toBodilessEntity();

        log.info("Confirmação cancelável enviada pelo WuzAPI");
    }
}
