package com.whatsapp_service.controller;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.client.FinancialReportClient;
import com.whatsapp_service.dto.MensagemFilaDTO;
import com.whatsapp_service.dto.ResumoFinanceiroDTO;
import com.whatsapp_service.dto.GastoPorCategoriaDTO;
import com.whatsapp_service.dto.CancelamentoTransacaoDTO;
import com.whatsapp_service.dto.WuzapiWebhookPayload;
import com.whatsapp_service.producer.WhatsAppQueueProducer;
import com.whatsapp_service.service.ConversationStateService;
import com.whatsapp_service.service.PieChartService;
import com.whatsapp_service.flow.WhatsAppActionResolver;
import com.whatsapp_service.flow.WhatsAppFlowRouter;
import com.whatsapp_service.security.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    private static final String HMAC_SECRET = "chave-hmac-de-teste-com-32-caracteres";

    @Mock
    private WhatsAppQueueProducer producer;

    @Mock
    private WuzApiClient wuzApiClient;

    @Mock
    private ConversationStateService conversationStateService;

    @Mock
    private FinancialReportClient financialReportClient;

    @Mock
    private PieChartService pieChartService;

    private ObjectMapper objectMapper;
    private WhatsAppWebhookController controller;

    @BeforeEach
    void configurar() {
        objectMapper = new ObjectMapper();
        WhatsAppFlowRouter flowRouter = new WhatsAppFlowRouter(
                new WhatsAppActionResolver(),
                producer,
                wuzApiClient,
                conversationStateService,
                financialReportClient,
                pieChartService
        );
        controller = new WhatsAppWebhookController(
                objectMapper,
                flowRouter,
                new WebhookSignatureVerifier(HMAC_SECRET)
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

        var resposta = receber(payload);

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
    void deveRejeitarWebhookSemAssinatura() throws Exception {
        byte[] rawBody = objectMapper.writeValueAsBytes(payload("Message", "{}"));

        var resposta = controller.receberMensagem(null, rawBody);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarWebhookComCorpoAlterado() throws Exception {
        byte[] original = objectMapper.writeValueAsBytes(payload("Message", "{}"));
        byte[] alterado = objectMapper.writeValueAsBytes(payload("ReadReceipt", "{}"));

        var resposta = controller.receberMensagem(assinar(original), alterado);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void devePropagarFalhaDePayloadAssinadoParaRetornarErroAoWuzapi() throws Exception {
        byte[] rawBody = "{json-invalido".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> controller.receberMensagem(assinar(rawBody), rawBody))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("500 INTERNAL_SERVER_ERROR");
    }

    @Test
    void deveIgnorarTipoDeEventoQueNaoSejaMensagem() throws Exception {
        WuzapiWebhookPayload payload = payload("ReadReceipt", "\"code\"");

        var resposta = receber(payload);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveIgnorarEventoDeMensagemQueNaoSejaObjeto() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", "\"code\"");

        var resposta = receber(payload);

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

        var resposta = receber(payload);

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

        receber(payload);

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

        receber(payload);

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

        receber(payload);

        verify(conversationStateService).aguardarRegistroDeGasto("5531999998888");
        verify(wuzApiClient).enviarMensagem(
                "5531999998888",
                "Envie agora a descrição do gasto. Exemplo: Gastei 50 reais no futebol."
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveEnviarResumoSemAcionarIaAoClicarEmVerRelatorio() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-report",
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "buttonsResponseMessage": {
                      "selectedButtonId": "ver_relatorio"
                    }
                  }
                }
                """);
        when(financialReportClient.buscarResumo("5531999998888"))
                .thenReturn(new ResumoFinanceiroDTO(
                        new BigDecimal("150.50"),
                        new BigDecimal("620.90"),
                        List.of(
                                new GastoPorCategoriaDTO("Alimentação", new BigDecimal("400.00")),
                                new GastoPorCategoriaDTO("Lazer", new BigDecimal("220.90"))
                        )
                ));
        when(pieChartService.gerarGraficoBase64(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn("data:image/png;base64,imagem");

        receber(payload);

        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("R$ 150,50")
        );
        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("R$ 620,90")
        );
        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("Alimentação")
        );
        verify(wuzApiClient).enviarImagem(
                "5531999998888",
                "Gastos por categoria nos últimos 30 dias",
                "data:image/png;base64,imagem"
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveCancelarTransacaoIndicadaPeloBotao() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {
                    "ID": "message-cancel",
                    "Sender": "5531999998888@s.whatsapp.net"
                  },
                  "Message": {
                    "templateButtonReplyMessage": {
                      "selectedID": "cancelar_transacao_99",
                      "selectedDisplayText": "✖ Cancelar"
                    }
                  }
                }
                """);
        when(financialReportClient.cancelarTransacao(99L, "5531999998888"))
                .thenReturn(new CancelamentoTransacaoDTO(
                        99L,
                        new BigDecimal("50.00"),
                        true
                ));

        receber(payload);

        verify(financialReportClient).cancelarTransacao(99L, "5531999998888");
        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("cancelado")
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveAbrirMenuDeMaisOpcoesSemAcionarIa() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {"ID": "message-more", "Sender": "5531999998888@s.whatsapp.net"},
                  "Message": {"templateButtonReplyMessage": {"selectedID": "mais_opcoes"}}
                }
                """);

        receber(payload);

        verify(wuzApiClient).enviarMenuMaisOpcoes("5531999998888");
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveListarCategoriasSemAcionarIa() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {"ID": "message-categories", "Sender": "5531999998888@s.whatsapp.net"},
                  "Message": {"templateButtonReplyMessage": {"selectedID": "listar_categorias"}}
                }
                """);
        when(financialReportClient.buscarCategorias("5531999998888"))
                .thenReturn(List.of("Alimentação", "Lazer", "Outros"));

        receber(payload);

        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("Lazer")
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveVoltarAoMenuPrincipalSemAcionarIa() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {"ID": "message-back", "Sender": "5531999998888@s.whatsapp.net"},
                  "Message": {"templateButtonReplyMessage": {"selectedID": "voltar_menu"}}
                }
                """);

        receber(payload);

        verify(wuzApiClient).enviarMenuPrincipal("5531999998888");
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveCriarCategoriaEsperadaSemAcionarIa() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {"ID": "message-category-name", "Sender": "5531999998888@s.whatsapp.net"},
                  "Message": {"conversation": "Viagens"}
                }
                """);
        when(conversationStateService.consumirSeAguardandoNomeDaCategoria("5531999998888"))
                .thenReturn(true);
        when(financialReportClient.criarCategoria("5531999998888", "Viagens"))
                .thenReturn("Viagens");

        receber(payload);

        verify(financialReportClient).criarCategoria("5531999998888", "Viagens");
        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("Viagens")
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveConfirmarDesativacaoSemAcionarIa() throws Exception {
        WuzapiWebhookPayload payload = payload("Message", """
                {
                  "Info": {"ID": "message-disable-confirm", "Sender": "5531999998888@s.whatsapp.net"},
                  "Message": {"templateButtonReplyMessage": {"selectedID": "confirmar_desativacao_categoria"}}
                }
                """);
        when(conversationStateService.consumirCategoriaParaConfirmarDesativacao("5531999998888"))
                .thenReturn("Viagens");
        when(financialReportClient.desativarCategoria("5531999998888", "Viagens"))
                .thenReturn("Viagens");

        receber(payload);

        verify(financialReportClient).desativarCategoria("5531999998888", "Viagens");
        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                org.mockito.ArgumentMatchers.contains("desativada")
        );
        verify(producer, never()).enviarParaProcessamento(org.mockito.ArgumentMatchers.any());
    }

    private WuzapiWebhookPayload payload(String type, String eventJson) throws Exception {
        JsonNode event = objectMapper.readTree(eventJson);
        return new WuzapiWebhookPayload(type, event);
    }

    private org.springframework.http.ResponseEntity<Void> receber(WuzapiWebhookPayload payload) throws Exception {
        byte[] rawBody = objectMapper.writeValueAsBytes(payload);
        return controller.receberMensagem(assinar(rawBody), rawBody);
    }

    private String assinar(byte[] rawBody) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(rawBody));
    }
}
