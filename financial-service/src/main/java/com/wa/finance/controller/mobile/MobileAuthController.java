package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.auth.SolicitarCodigoRequest;
import com.wa.finance.dto.mobile.auth.SolicitarCodigoResponse;
import com.wa.finance.dto.mobile.auth.RefreshTokenRequest;
import com.wa.finance.dto.mobile.SessoesMobileResponse;
import com.wa.finance.dto.mobile.auth.TokenResponse;
import com.wa.finance.dto.mobile.auth.VerificarCodigoRequest;
import com.wa.finance.service.ConfirmacaoCodigoResultado;
import com.wa.finance.service.ConfirmacaoCodigoService;
import com.wa.finance.service.SolicitacaoCodigoCriada;
import com.wa.finance.service.SolicitacaoCodigoService;
import com.wa.finance.service.MobileSessionService;
import com.wa.finance.web.mobile.ClientIpResolver;
import com.wa.finance.web.mobile.MobileCorrelationIdFilter;
import com.wa.finance.web.mobile.InvalidVerificationCodeException;
import com.wa.finance.web.mobile.InvalidRefreshTokenException;
import com.wa.finance.security.MobilePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/mobile/v1/auth")
public class MobileAuthController {

    private final SolicitacaoCodigoService solicitacaoCodigoService;
    private final ConfirmacaoCodigoService confirmacaoCodigoService;
    private final ClientIpResolver clientIpResolver;
    private final MobileSessionService mobileSessionService;

    @Autowired
    public MobileAuthController(
            SolicitacaoCodigoService solicitacaoCodigoService,
            ConfirmacaoCodigoService confirmacaoCodigoService,
            ClientIpResolver clientIpResolver,
            MobileSessionService mobileSessionService
    ) {
        this.solicitacaoCodigoService = solicitacaoCodigoService;
        this.confirmacaoCodigoService = confirmacaoCodigoService;
        this.clientIpResolver = clientIpResolver;
        this.mobileSessionService = mobileSessionService;
    }

    public MobileAuthController(
            SolicitacaoCodigoService solicitacaoCodigoService,
            ConfirmacaoCodigoService confirmacaoCodigoService,
            ClientIpResolver clientIpResolver
    ) {
        this(solicitacaoCodigoService, confirmacaoCodigoService, clientIpResolver, null);
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
    public ResponseEntity<TokenResponse> verificarCodigo(
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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(MobileCorrelationIdFilter.HEADER_NAME, correlationId)
                .body(resultado.codigoConfirmado().orElseThrow().tokenResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> renovar(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = mobileSessionService.renovar(request.refreshToken());
        if (response == null) throw new InvalidRefreshTokenException();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        mobileSessionService.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<SessoesMobileResponse> listarSessoes(@AuthenticationPrincipal Jwt jwt) {
        UUID currentSessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SessoesMobileResponse(
                        mobileSessionService.listar(MobilePrincipal.usuarioId(jwt), currentSessionId)));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revogarSessao(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id
    ) {
        if (!mobileSessionService.revogar(id, MobilePrincipal.usuarioId(jwt))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revogarTodasSessoes(@AuthenticationPrincipal Jwt jwt) {
        mobileSessionService.revogarTodas(MobilePrincipal.usuarioId(jwt));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
