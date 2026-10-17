package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.auth.SolicitarCodigoRequest;
import com.wa.finance.dto.mobile.auth.SolicitarCodigoResponse;
import com.wa.finance.dto.mobile.auth.VerificarCodigoRequest;
import com.wa.finance.service.ConfirmacaoCodigoResultado;
import com.wa.finance.service.ConfirmacaoCodigoService;
import com.wa.finance.service.SolicitacaoCodigoCriada;
import com.wa.finance.service.SolicitacaoCodigoService;
import com.wa.finance.web.mobile.ClientIpResolver;
import com.wa.finance.web.mobile.MobileCorrelationIdFilter;
import com.wa.finance.web.mobile.InvalidVerificationCodeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/v1/auth")
public class MobileAuthController {

    private final SolicitacaoCodigoService solicitacaoCodigoService;
    private final ConfirmacaoCodigoService confirmacaoCodigoService;
    private final ClientIpResolver clientIpResolver;

    public MobileAuthController(
            SolicitacaoCodigoService solicitacaoCodigoService,
            ConfirmacaoCodigoService confirmacaoCodigoService,
            ClientIpResolver clientIpResolver
    ) {
        this.solicitacaoCodigoService = solicitacaoCodigoService;
        this.confirmacaoCodigoService = confirmacaoCodigoService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/codes")
    public ResponseEntity<SolicitarCodigoResponse> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipCanonico = clientIpResolver.resolver(httpRequest);
        String correlationId = MobileCorrelationIdFilter.getOrCreate(httpRequest);
        SolicitacaoCodigoCriada resultado = solicitacaoCodigoService.solicitar(
                request.telefone(), ipCanonico, correlationId);

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(new SolicitarCodigoResponse(
                        resultado.challengeId(),
                        resultado.expiraEmSegundos(),
                        resultado.reenviarEmSegundos()
                ));
    }

    @PostMapping("/codes/verify")
    public ResponseEntity<Void> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = MobileCorrelationIdFilter.getOrCreate(httpRequest);
        ConfirmacaoCodigoResultado resultado = confirmacaoCodigoService.confirmar(
                request.challengeId(),
                request.codigo(),
                request.nomeDispositivo(),
                correlationId
        );
        if (!resultado.foiConfirmado()) {
            throw new InvalidVerificationCodeException();
        }

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(MobileCorrelationIdFilter.HEADER_NAME, correlationId)
                .build();
    }
}
