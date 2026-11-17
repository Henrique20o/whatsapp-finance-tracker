package com.wa.finance.controller.mobile;

import com.wa.finance.service.SolicitacaoCodigoCriada;
import com.wa.finance.service.SolicitacaoCodigoService;
import com.wa.finance.dto.mobile.auth.TokenResponse;
import com.wa.finance.service.CodigoConfirmado;
import com.wa.finance.service.ConfirmacaoCodigoResultado;
import com.wa.finance.service.ConfirmacaoCodigoService;
import com.wa.finance.service.RateLimitExceededException;
import com.wa.finance.service.RateLimitRule;
import com.wa.finance.service.RateLimitScope;
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
    private static final String VERIFY_PATH = PATH + "/verify";
    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private TestSolicitacaoCodigoService service;
    private TestConfirmacaoCodigoService confirmacaoService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        service = new TestSolicitacaoCodigoService();
        confirmacaoService = new TestConfirmacaoCodigoService();
        MobileAuthController controller = new MobileAuthController(
                service, confirmacaoService, new ClientIpResolver());
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

    @Test
    void deveResponderTooManyRequestsComTempoDeEspera() throws Exception {
        service.falha = new RateLimitExceededException(
                61,
                RateLimitScope.PHONE,
                RateLimitRule.HOURLY
        );

        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"5531999998888\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "61"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string(
                        MobileCorrelationIdFilter.HEADER_NAME,
                        matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Aguarde antes de tentar novamente"))
                .andExpect(jsonPath("$.path").value(PATH))
                .andExpect(jsonPath("$.challengeId").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("HOURLY")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("5531999998888")
                )));
    }

    @Test
    void deveConfirmarCodigoComRespostaSemConteudoESemCache() throws Exception {
        UUID challengeId = UUID.randomUUID();
        confirmacaoService.resultado = ConfirmacaoCodigoResultado.confirmado(
                new CodigoConfirmado(42L, "Galaxy S24",
                        new TokenResponse("jwt-access", "Bearer", 900, "opaque-refresh", 2592000)));

        mockMvc.perform(post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeId":"%s",
                                  "codigo":"482913",
                                  "nomeDispositivo":"Galaxy S24"
                                }
                                """.formatted(challengeId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string(MobileCorrelationIdFilter.HEADER_NAME, matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.accessToken").value("jwt-access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessTokenExpiraEmSegundos").value(900))
                .andExpect(jsonPath("$.refreshToken").value("opaque-refresh"))
                .andExpect(jsonPath("$.refreshTokenExpiraEmSegundos").value(2592000));

        org.assertj.core.api.Assertions.assertThat(confirmacaoService.challengeId).isEqualTo(challengeId);
        org.assertj.core.api.Assertions.assertThat(confirmacaoService.codigo).isEqualTo("482913");
        org.assertj.core.api.Assertions.assertThat(confirmacaoService.nomeDispositivo).isEqualTo("Galaxy S24");
        org.assertj.core.api.Assertions.assertThat(confirmacaoService.correlationId).matches(UUID_PATTERN);
    }

    @Test
    void deveResponderErroGenericoQuandoConfirmacaoForRejeitada() throws Exception {
        UUID challengeId = UUID.randomUUID();
        confirmacaoService.resultado = ConfirmacaoCodigoResultado.rejeitado();

        mockMvc.perform(post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"" + challengeId + "\",\"codigo\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_CODE"))
                .andExpect(jsonPath("$.message").value("O código é inválido ou expirou"))
                .andExpect(jsonPath("$.path").value(VERIFY_PATH));
    }

    @Test
    void deveRejeitarContratoInvalidoNaConfirmacao() throws Exception {
        assertInvalidVerify("{}");
        assertInvalidVerify("{\"challengeId\":\"invalido\",\"codigo\":\"123456\"}");
        assertInvalidVerify("{\"challengeId\":\"" + UUID.randomUUID() + "\",\"codigo\":\"12345\"}");
        assertInvalidVerify("{\"challengeId\":\"" + UUID.randomUUID()
                + "\",\"codigo\":\"123456\",\"nomeDispositivo\":\"   \"}");
        assertInvalidVerify("{\"challengeId\":\"" + UUID.randomUUID()
                + "\",\"codigo\":\"123456\",\"telefone\":\"5531999998888\"}");
    }

    private void assertInvalid(String json) throws Exception {
        mockMvc.perform(post(PATH)
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void assertInvalidVerify(String json) throws Exception {
        mockMvc.perform(post(VERIFY_PATH)
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

    private static final class TestConfirmacaoCodigoService extends ConfirmacaoCodigoService {

        private ConfirmacaoCodigoResultado resultado = ConfirmacaoCodigoResultado.rejeitado();
        private UUID challengeId;
        private String codigo;
        private String nomeDispositivo;
        private String correlationId;

        private TestConfirmacaoCodigoService() {
            super(null, null, null, null, null);
        }

        @Override
        public ConfirmacaoCodigoResultado confirmar(
                UUID challengeId,
                String codigo,
                String nomeDispositivo,
                String correlationId
        ) {
            this.challengeId = challengeId;
            this.codigo = codigo;
            this.nomeDispositivo = nomeDispositivo;
            this.correlationId = correlationId;
            return resultado;
        }
    }
}
