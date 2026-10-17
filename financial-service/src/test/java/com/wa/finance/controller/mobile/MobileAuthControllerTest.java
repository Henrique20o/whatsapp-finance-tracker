package com.wa.finance.controller.mobile;

import com.wa.finance.service.SolicitacaoCodigoCriada;
import com.wa.finance.service.SolicitacaoCodigoService;
import com.wa.finance.web.mobile.ClientIpResolver;
import com.wa.finance.web.mobile.MobileApiExceptionHandler;
import com.wa.finance.web.mobile.MobileCorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MobileAuthControllerTest {

    private static final String PATH = "/mobile/v1/auth/codes";
    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private TestSolicitacaoCodigoService service;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        service = new TestSolicitacaoCodigoService();
        MobileAuthController controller = new MobileAuthController(service, new ClientIpResolver());
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T20:30:00Z"), ZoneOffset.UTC);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MobileApiExceptionHandler(clock))
                .addFilters(new MobileCorrelationIdFilter())
                .build();
    }

    @Test
    void deveAceitarSolicitacaoEExporSomenteMetadadosPublicos() throws Exception {
        UUID challengeId = UUID.randomUUID();
        service.resultado = new SolicitacaoCodigoCriada(challengeId, 300, 60);

        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"5531999998888\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string(MobileCorrelationIdFilter.HEADER_NAME, matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.challengeId").value(challengeId.toString()))
                .andExpect(jsonPath("$.expiraEmSegundos").value(300))
                .andExpect(jsonPath("$.reenviarEmSegundos").value(60))
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.codigo").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(service.telefoneRecebido).isEqualTo("5531999998888");
        org.assertj.core.api.Assertions.assertThat(service.ipRecebido).isEqualTo("192.0.2.10");
        org.assertj.core.api.Assertions.assertThat(service.correlationIdRecebido)
                .matches(UUID_PATTERN);
        org.assertj.core.api.Assertions.assertThat(service.chamadas).isEqualTo(1);
    }

    @Test
    void deveRejeitarTelefoneForaDoContrato() throws Exception {
        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"+55 (31) 99999-8888\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Os dados enviados são inválidos"))
                .andExpect(jsonPath("$.path").value(PATH))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_PATTERN)));
    }

    @Test
    void deveRejeitarJsonMalformadoCampoAusenteEPropriedadeDesconhecida() throws Exception {
        assertInvalid("{\"telefone\":");
        assertInvalid("{}");
        assertInvalid("{\"telefone\":\"5531999998888\",\"codigo\":\"123456\"}");
    }

    @Test
    void deveResponderIndisponivelSemExporDetalhesTecnicos() throws Exception {
        service.falha = new IllegalStateException("segredo técnico que não pode vazar");

        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"5531999998888\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Não foi possível processar a solicitação agora"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("segredo técnico")
                )));
    }

    private void assertInvalid(String json) throws Exception {
        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static final class TestSolicitacaoCodigoService extends SolicitacaoCodigoService {

        private SolicitacaoCodigoCriada resultado;
        private RuntimeException falha;
        private String telefoneRecebido;
        private String ipRecebido;
        private String correlationIdRecebido;
        private int chamadas;

        private TestSolicitacaoCodigoService() {
            super(null, null, null, null);
        }

        @Override
        public SolicitacaoCodigoCriada solicitar(
                String telefone,
                String ipCanonico,
                String correlationId
        ) {
            chamadas++;
            telefoneRecebido = telefone;
            ipRecebido = ipCanonico;
            correlationIdRecebido = correlationId;
            if (falha != null) {
                throw falha;
            }
            return resultado;
        }
    }
}
