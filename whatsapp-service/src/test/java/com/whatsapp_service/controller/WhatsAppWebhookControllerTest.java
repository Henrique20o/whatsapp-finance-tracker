package com.whatsapp_service.controller;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import com.whatsapp_service.service.ConversationStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock
    private WhatsAppQueueProducer producer;

    @Mock
    private WuzApiClient wuzApiClient;

    @Mock
    private ConversationStateService conversationStateService;

    private ObjectMapper objectMapper;
    private WhatsAppWebhookController controller;

    @BeforeEach
    void configurar() {
        objectMapper = new ObjectMapper();
        controller = new WhatsAppWebhookController(
                producer,
                objectMapper,
                wuzApiClient,
                conversationStateService
        );
    }

    @Test
    void deveEnviarMensagemDeTextoValidaParaFila() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-123",
                    "Sender": "5531999998888@s.whatsapp.net",
                    "SenderAlt": "5531888887777@s.whatsapp.net"
                  },
                  "Message": {
                    "conversation": "Gastei 50 reais no futebol"
                  }
                }
                """);

        var resposta = controller.receberMensagem(payload);

        ArgumentCaptor<MensagemFilaDTO> mensagem = ArgumentCaptor.forClass(MensagemFilaDTO.class);
        verify(producer).enviarParaProcessamento(mensagem.capture());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mensagem.getValue()).isEqualTo(new MensagemFilaDTO(
                "message-123",
                "5531888887777",
                "TEXTO",
                "Gastei 50 reais no futebol"
        ));
    }

    @Test
    void deveIgnorarTipoDeEventoQueNaoSejaMensagem() throws Exception {
        WuzapiWebhookPayload payload = payload("ReadReceipt", "\"code\"");

        var resposta = controller.receberMensagem(payload);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveIgnorarEventoDeMensagemQueNaoSejaObjeto() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", "\"code\"");

        var resposta = controller.receberMensagem(payload);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveIgnorarMensagemSemIdentificador() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "conversation": "Almocei no restaurante"
                  }
                }
                """);

        var resposta = controller.receberMensagem(payload);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveUsarSenderQuandoSenderAltNaoEstiverDisponivel() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-456",
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "conversation": "Paguei 30 reais de transporte"
                  }
                }
                """);

        controller.receberMensagem(payload);

        ArgumentCaptor<MensagemFilaDTO> mensagem = ArgumentCaptor.forClass(MensagemFilaDTO.class);
        verify(producer).enviarParaProcessamento(mensagem.capture());
        assertThat(mensagem.getValue().telefone()).isEqualTo("5531999998888");
    }

    @Test
    void deveEnviarMenuPrincipalAoReceberSaudacao() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-menu",
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "conversation": "Olá"
                  }
                }
                """);

        controller.receberMensagem(payload);

        verify(wuzApiClient).enviarMenuPrincipal("5531999998888");
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveIniciarFluxoAoClicarEmRegistrarGasto() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-button",
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "templateButtonReplyMessage": {
                      "selectedDisplayText": "Registrar gasto"
                    }
                  }
                }
                """);

        controller.receberMensagem(payload);

        verify(conversationStateService).aguardarRegistroDeGasto("5531999998888");
        verify(wuzApiClient).enviarMensagem(
                "5531999998888",
                "Envie agora a descrição do gasto. Exemplo: Gastei 50 reais no futebol."
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    private WuzapiWebhookPayload payload(String type, String eventJson) throws Exception {
        JsonNode event = objectMapper.readTree(eventJson);
        return new WuzapiWebhookPayload(type, event);
    }
}
