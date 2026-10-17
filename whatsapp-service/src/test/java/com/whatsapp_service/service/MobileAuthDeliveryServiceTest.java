package com.whatsapp_service.service;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.security.mobile.InvalidMobileAuthEnvelopeException;
import com.whatsapp_service.security.mobile.MobileAuthDeliveryCipher;
import com.whatsapp_service.security.mobile.MobileAuthDeliveryEnvelope;
import com.whatsapp_service.security.mobile.MobileAuthDeliverySecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileAuthDeliveryServiceTest {

    private final Instant now = Instant.parse("2026-10-17T20:00:00Z");
    private final MobileAuthDeliveryCipher cipher = mock(MobileAuthDeliveryCipher.class);
    private final WuzApiClient wuzApiClient = mock(WuzApiClient.class);
    private MobileAuthDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new MobileAuthDeliveryService(
                cipher, wuzApiClient, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void deveEnviarTemplateSeguroComCodigo() {
        MobileAuthDeliveryEnvelope envelope = envelope(now.plusSeconds(300));
        when(cipher.decrypt(envelope)).thenReturn(
                new MobileAuthDeliverySecret("5531999998888", "004291"));

        service.deliver(envelope);

        verify(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.eq("5531999998888"),
                contains("004291"));
    }

    @Test
    void deveDescartarEnvelopeExpiradoAntesDeDecifrar() {
        MobileAuthDeliveryEnvelope envelope = envelope(now);

        service.deliver(envelope);

        verify(cipher, never()).decrypt(envelope);
        verify(wuzApiClient, never()).enviarMensagem(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deveClassificarEnvelopeInvalidoComoFalhaPermanente() {
        MobileAuthDeliveryEnvelope envelope = envelope(now.plusSeconds(300));
        when(cipher.decrypt(envelope)).thenThrow(new InvalidMobileAuthEnvelopeException());

        assertThatThrownBy(() -> service.deliver(envelope))
                .isInstanceOf(PermanentMobileAuthDeliveryException.class)
                .hasMessage("INVALID_ENVELOPE");
    }

    @Test
    void deveClassificarErroCincoXXComoTransitorio() {
        MobileAuthDeliveryEnvelope envelope = validEnvelope();
        RestClientResponseException exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
        doThrow(exception).when(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> service.deliver(envelope))
                .isInstanceOf(TransientMobileAuthDeliveryException.class)
                .hasMessage("WUZAPI_503");
    }

    @Test
    void deveClassificarErroQuatroXXComoPermanente() {
        MobileAuthDeliveryEnvelope envelope = validEnvelope();
        RestClientResponseException exception = mock(RestClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        doThrow(exception).when(wuzApiClient).enviarMensagem(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> service.deliver(envelope))
                .isInstanceOf(PermanentMobileAuthDeliveryException.class)
                .hasMessage("WUZAPI_4XX");
    }

    @Test
    void deveClassificarFalhaDeConexaoComoTransitoria() {
        MobileAuthDeliveryEnvelope envelope = validEnvelope();
        doThrow(new ResourceAccessException("detalhe externo"))
                .when(wuzApiClient).enviarMensagem(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> service.deliver(envelope))
                .isInstanceOf(TransientMobileAuthDeliveryException.class)
                .hasMessage("WUZAPI_IO")
                .message().doesNotContain("detalhe externo");
    }

    private MobileAuthDeliveryEnvelope validEnvelope() {
        MobileAuthDeliveryEnvelope envelope = envelope(now.plusSeconds(300));
        when(cipher.decrypt(envelope)).thenReturn(
                new MobileAuthDeliverySecret("5531999998888", "004291"));
        return envelope;
    }

    private MobileAuthDeliveryEnvelope envelope(Instant expiresAt) {
        return new MobileAuthDeliveryEnvelope(
                1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), expiresAt,
                "v1", "aXY=", "Y2lwaGVydGV4dA==");
    }
}
