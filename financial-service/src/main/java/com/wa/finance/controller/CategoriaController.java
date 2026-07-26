package com.wa.finance.controller;

import com.wa.finance.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}