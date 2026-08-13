package com.wa.finance.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    private static final String API_KEY = "chave-interna-financeiro-de-teste-123456";
    private final InternalApiKeyFilter filter = new InternalApiKeyFilter(API_KEY);

    @Test
    void deveRejeitarApiFinanceiraSemCredencial() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/relatorios/resumo");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void deveAceitarApiFinanceiraComCredencialCorreta() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/relatorios/resumo");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, API_KEY);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void deveManterHealthcheckAcessivelSemCredencial() throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }
}
