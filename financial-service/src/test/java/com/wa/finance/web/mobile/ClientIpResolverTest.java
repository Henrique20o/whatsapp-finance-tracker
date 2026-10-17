package com.wa.finance.web.mobile;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void deveResolverIpv4DaConexaoEIgnorarHeadersForjados() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("X-Forwarded-For", "203.0.113.50");
        request.addHeader("Forwarded", "for=203.0.113.51");
        request.addHeader("X-Real-IP", "203.0.113.52");

        assertThat(resolver.resolver(request)).isEqualTo("192.0.2.10");
    }

    @Test
    void deveCanonicalizarIpv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("2001:0DB8:0:0:0:0:0:1");

        assertThat(resolver.resolver(request)).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void deveRejeitarEnderecoInvalidoSemExpoLoNaMensagem() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("endereco-secreto-invalido");

        assertThatThrownBy(() -> resolver.resolver(request))
                .isInstanceOf(InvalidClientAddressException.class)
                .hasMessageNotContaining("endereco-secreto-invalido");
    }
}
