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


        log.info(
                "Mensagem enviada para {}",
                telefone
        );
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

        log.info("Menu principal enviado para {}", telefone);
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

        log.info("Imagem enviada para {}", telefone);
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

        log.info("Confirmação cancelável enviada para {}", telefone);
    }
}
