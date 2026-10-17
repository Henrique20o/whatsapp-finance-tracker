package com.whatsapp_service.service;

import com.whatsapp_service.client.WuzApiClient;
import com.whatsapp_service.security.mobile.InvalidMobileAuthEnvelopeException;
import com.whatsapp_service.security.mobile.MobileAuthDeliveryCipher;
import com.whatsapp_service.security.mobile.MobileAuthDeliveryEnvelope;
import com.whatsapp_service.security.mobile.MobileAuthDeliverySecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MobileAuthDeliveryService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[1-9][0-9]{9,14}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    private final MobileAuthDeliveryCipher cipher;
    private final WuzApiClient wuzApiClient;
    private final Clock clock;

    public MobileAuthDeliveryService(
            MobileAuthDeliveryCipher cipher,
            WuzApiClient wuzApiClient,
            Clock clock
    ) {
        this.cipher = cipher;
        this.wuzApiClient = wuzApiClient;
        this.clock = clock;
    }

    public void deliver(MobileAuthDeliveryEnvelope envelope) {
        if (!envelope.expiresAt().isAfter(clock.instant())) {
            log.info("event=MOBILE_AUTH_DELIVERY_EXPIRED messageId={}", envelope.messageId());
            return;
        }

        final MobileAuthDeliverySecret secret;
        try {
            secret = cipher.decrypt(envelope);
        } catch (InvalidMobileAuthEnvelopeException exception) {
            throw new PermanentMobileAuthDeliveryException("INVALID_ENVELOPE");
        }

        if (secret.telefone() == null
                || secret.codigo() == null
                || !PHONE_PATTERN.matcher(secret.telefone()).matches()
                || !CODE_PATTERN.matcher(secret.codigo()).matches()) {
            throw new PermanentMobileAuthDeliveryException("INVALID_SECRET_FORMAT");
        }

        if (!envelope.expiresAt().isAfter(clock.instant())) {
            log.info("event=MOBILE_AUTH_DELIVERY_EXPIRED messageId={}", envelope.messageId());
            return;
        }

        try {
            wuzApiClient.enviarMensagem(secret.telefone(), message(secret.codigo()));
            log.info(
                    "event=MOBILE_AUTH_DELIVERY_SENT messageId={} challengeId={} correlationId={}",
                    envelope.messageId(), envelope.challengeId(), envelope.correlationId());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 408 || status == 429 || status >= 500) {
                throw new TransientMobileAuthDeliveryException("WUZAPI_" + status);
            }
            throw new PermanentMobileAuthDeliveryException("WUZAPI_4XX");
        } catch (ResourceAccessException exception) {
            throw new TransientMobileAuthDeliveryException("WUZAPI_IO");
        } catch (RuntimeException exception) {
            throw new TransientMobileAuthDeliveryException("WUZAPI_UNKNOWN");
        }
    }

    private String message(String code) {
        return "Seu código de acesso ao Finance Tracker é: " + code
                + "\n\nEle expira em 5 minutos. Não compartilhe este código."
                + "\n\nSe você não solicitou este acesso, ignore esta mensagem.";
    }
}
