package com.wa.finance.controller.mobile;

import com.wa.finance.dto.mobile.CategoriasMobileResponse;
import com.wa.finance.dto.mobile.CategoriaMobileRequest;
import com.wa.finance.dto.mobile.CategoriaMobileDetalheResponse;
import com.wa.finance.security.MobilePrincipal;
import com.wa.finance.service.MobileCategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/mobile/v1/categorias")
public class MobileCategoryController {
    private final MobileCategoryService service;

    public MobileCategoryController(MobileCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public CategoriasMobileResponse listar(@AuthenticationPrincipal Jwt jwt) {
        return new CategoriasMobileResponse(service.listarAtivas(MobilePrincipal.usuarioId(jwt)));
    }

    @PostMapping
    public ResponseEntity<CategoriaMobileDetalheResponse> criar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CategoriaMobileRequest request
    ) {
        var result = service.criar(MobilePrincipal.usuarioId(jwt), request.nome());
        var response = ResponseEntity.status(result.nova() ? 201 : 200);
        if (result.nova()) response.location(java.net.URI.create("/mobile/v1/categorias/" + result.categoria().id()));
        return response.body(result.categoria());
    }

    @PatchMapping("/{id}")
    public CategoriaMobileDetalheResponse renomear(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody CategoriaMobileRequest request
    ) {
        return service.renomear(MobilePrincipal.usuarioId(jwt), id, request.nome());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.desativar(MobilePrincipal.usuarioId(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
