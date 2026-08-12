package com.wa.finance.controller;

import com.wa.finance.dto.CategoriaRequestDTO;
import com.wa.finance.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<String>> listarPorTelefone(@RequestParam String telefone) {
        List<String> categorias = categoriaService.buscarNomesCategoriasPorTelefone(telefone);
        return ResponseEntity.ok(categorias);
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.criarOuReativar(request.telefone(), request.nome()));
    }

    @DeleteMapping
    public ResponseEntity<String> desativar(
            @RequestParam String telefone,
            @RequestParam String nome
    ) {
        return ResponseEntity.ok(categoriaService.desativar(telefone, nome));
    }
}
