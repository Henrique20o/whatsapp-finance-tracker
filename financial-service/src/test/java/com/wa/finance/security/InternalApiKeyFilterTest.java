package com.wa.finance.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    private static final String API_KEY = "chave-interna-financeiro-de-teste-123456";
    private final InternalApiKeyFilter filter = new InternalApiKeyFilter(API_KEY);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
    void deveRejeitarApiFinanceiraComCredencialIncorreta() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/relatorios/resumo");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "chave-incorreta-com-mais-de-32-caracteres");
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
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("internal-service");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_INTERNAL");
    }

    @Test
    void filtroIsoladoDeveExigirChaveIndependentementeDoCaminho() throws Exception {
        var request = new MockHttpServletRequest("GET", "/mobile/v1/transacoes");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }
}
