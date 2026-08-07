package com.whatsapp_service.controller;

import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppQueueProducer producer;



    @PostMapping("/wuzapi")
    public ResponseEntity<Void> receberMensagem(
            @RequestBody WuzapiWebhookPayload payload) {

        try {

            if (payload.type() == null ||
                    !payload.type().equalsIgnoreCase("Message")) {

                return ResponseEntity.ok().build();
            }


            if (payload.event() == null ||
                    payload.event().info() == null) {

                log.warn("Webhook recebido sem informações do remetente");
                return ResponseEntity.ok().build();
            }


            String telefone = payload.event()
                    .info()
                    .getTelefone();


            if (telefone == null) {
                log.debug("Mensagem ignorada sem telefone válido");
                return ResponseEntity.ok().build();
            }




            if (payload.event().message() == null) {
                return ResponseEntity.ok().build();
            }


            String texto = payload.event()
                    .message()
                    .conversation();


            if (texto != null && !texto.isBlank()) {

                MensagemFilaDTO dto =
                        new MensagemFilaDTO(
                                telefone,
                                "TEXTO",
                                texto
                        );


                producer.enviarParaProcessamento(dto);

                log.info(
                        "Mensagem enviada para fila. Telefone: {}",
                        telefone
                );
            }


        } catch (Exception e) {

            log.error(
                    "Erro ao processar webhook WuzAPI",
                    e
            );
        }


        return ResponseEntity.ok().build();
    }
}