package com.wa.finance.service;

import com.wa.finance.dto.mobile.PerfilMobileResponse;
import com.wa.finance.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;

@Service
public class MobileProfileService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final UsuarioRepository users;
    private final UsuarioService usuarioService;

    public MobileProfileService(UsuarioRepository users, UsuarioService usuarioService) {
        this.users = users;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public PerfilMobileResponse obter(Long userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String digits = usuarioService.obterTelefone(user).replaceAll("\\D", "");
        String masked = digits.length() <= 4 ? "••••" : "••••••••" + digits.substring(digits.length() - 4);
        var createdAt = user.getDataCadastro() == null ? null
                : user.getDataCadastro().atZone(BUSINESS_ZONE).toOffsetDateTime();
        return new PerfilMobileResponse(user.getMobilePublicId(), user.getNome(), user.getEmail(), masked, createdAt);
    }
}
