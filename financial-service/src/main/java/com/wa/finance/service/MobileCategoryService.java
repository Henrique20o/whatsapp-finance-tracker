package com.wa.finance.service;

import com.wa.finance.dto.mobile.CategoriaMobileResponse;
import com.wa.finance.repository.CategoriaRepository;
import com.wa.finance.domain.Categoria;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.UsuarioRepository;
import com.wa.finance.dto.mobile.CategoriaMobileDetalheResponse;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MobileCategoryService {
    private final CategoriaRepository repository;
    private final UsuarioRepository users;

    public MobileCategoryService(CategoriaRepository repository, UsuarioRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CategoriaMobileResponse> listarAtivas(Long usuarioId) {
        return repository.findAllByUsuario_IdAndAtivaTrueOrderByNomeAsc(usuarioId).stream()
                .map(categoria -> new CategoriaMobileResponse(categoria.getId(), categoria.getNome()))
                .toList();
    }

    @Transactional
    public CategoriaMobileCriada criar(Long usuarioId, String rawName) {
        String name = normalize(rawName);
        var matches = repository.findByNormalizedName(usuarioId, name);
        Categoria existing = matches.stream().findFirst().orElse(null);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getAtiva())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já existe");
            }
            existing.setAtiva(true);
            try {
                return new CategoriaMobileCriada(details(repository.saveAndFlush(existing)), false);
            } catch (DataIntegrityViolationException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já existe");
            }
        }
        Usuario user = users.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Categoria category = Categoria.builder().nome(name).ativa(true).sistema(false).usuario(user).build();
        return new CategoriaMobileCriada(details(repository.saveAndFlush(category)), true);
    }

    @Transactional
    public CategoriaMobileDetalheResponse renomear(Long usuarioId, Long id, String rawName) {
        String name = normalize(rawName);
        Categoria category = repository.findByIdAndUsuarioIdForUpdate(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(category.getSistema())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Categoria padrão não pode ser renomeada");
        }
        boolean duplicate = repository.findByNormalizedName(usuarioId, name).stream()
                .anyMatch(item -> !item.getId().equals(id) && Boolean.TRUE.equals(item.getAtiva()));
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria já existe");
        category.setNome(name);
        return details(repository.saveAndFlush(category));
    }

    @Transactional
    public void desativar(Long usuarioId, Long id) {
        Categoria category = repository.findByIdAndUsuarioIdForUpdate(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(category.getSistema())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Categoria padrão não pode ser desativada");
        }
        category.setAtiva(false);
    }

    private String normalize(String value) {
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String name = value.trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return name;
    }

    private CategoriaMobileDetalheResponse details(Categoria category) {
        return new CategoriaMobileDetalheResponse(category.getId(), category.getNome(),
                Boolean.TRUE.equals(category.getAtiva()));
    }
}
