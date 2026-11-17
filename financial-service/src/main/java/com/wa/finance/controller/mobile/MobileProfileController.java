package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.PerfilMobileResponse;
import com.wa.finance.security.MobilePrincipal;
import com.wa.finance.service.MobileProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mobile/v1/me")
public class MobileProfileController {
    private final MobileProfileService service;

    public MobileProfileController(MobileProfileService service) { this.service = service; }

    @GetMapping
    public PerfilMobileResponse obter(@AuthenticationPrincipal Jwt jwt) {
        return service.obter(MobilePrincipal.usuarioId(jwt));
    }
}
