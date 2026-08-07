package com.whatsapp_service.client;

import com.whatsapp_service.dto.SendTextRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
}