package com.wa.finance.web.mobile;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MobileCorrelationIdFilterTest {

    private final MobileCorrelationIdFilter filter = new MobileCorrelationIdFilter();

    @Test
    void deveGerarCorrelationIdProprioEApagarMdcAoFinal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mobile/v1/auth/codes");
        request.addHeader(MobileCorrelationIdFilter.HEADER_NAME, "id-controlado-pelo-cliente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteARequisicao = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuranteARequisicao.set(MDC.get(MobileCorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader(MobileCorrelationIdFilter.HEADER_NAME);
        assertThat(correlationId)
                .isNotEqualTo("id-controlado-pelo-cliente")
                .isEqualTo(request.getAttribute(MobileCorrelationIdFilter.REQUEST_ATTRIBUTE))
                .isEqualTo(mdcDuranteARequisicao.get());
        assertThat(MDC.get(MobileCorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void naoDeveAlterarRequisicaoForaDaApiMobile() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/transacoes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(MobileCorrelationIdFilter.HEADER_NAME)).isNull();
        assertThat(request.getAttribute(MobileCorrelationIdFilter.REQUEST_ATTRIBUTE)).isNull();
    }
}
