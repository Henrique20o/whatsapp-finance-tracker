package com.wa.finance.service;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.domain.Usuario;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.security.VerificationCodeHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoCodigoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-17T18:00:00Z");
    private static final String TELEFONE = "5531999998888";
    private static final String CODIGO = "482913";

    @Mock
    private DesafioAutenticacaoRepository desafioRepository;

    @Mock
    private PhoneProtectionService phoneProtectionService;

    @Mock
    private VerificationCodeHasher codeHasher;

    @Mock
    private UsuarioService usuarioService;

    private ConfirmacaoCodigoService service;

    @BeforeEach
    void configurar() {
        service = new ConfirmacaoCodigoService(
                desafioRepository,
                phoneProtectionService,
                codeHasher,
                usuarioService,
                Clock.fixed(AGORA, ZoneOffset.UTC)
        );
    }

    @Test
    void deveConfirmarCodigoCriarOuLocalizarUsuarioEConsumirDesafio() {
        DesafioAutenticacao desafio = desafio();
        Usuario usuario = Usuario.builder().id(42L).build();
        prepararTelefone(desafio);
        when(desafioRepository.findByIdForUpdate(desafio.getId())).thenReturn(Optional.of(desafio));
        when(codeHasher.matches(desafio.getCodigoHmac(), desafio.getId(), TELEFONE, CODIGO))
                .thenReturn(true);
        when(usuarioService.buscarOuCriarUsuarioPorTelefone(TELEFONE)).thenReturn(usuario);

        ConfirmacaoCodigoResultado resultado = service.confirmar(
                desafio.getId(), CODIGO, "  Galaxy S24  ", UUID.randomUUID().toString());

        assertThat(resultado.foiConfirmado()).isTrue();
        assertThat(resultado.codigoConfirmado()).hasValueSatisfying(confirmado -> {
            assertThat(confirmado.usuarioId()).isEqualTo(42L);
            assertThat(confirmado.nomeDispositivo()).isEqualTo("Galaxy S24");
        });
        assertThat(desafio.getConsumidoEm()).isEqualTo(AGORA);
        assertThat(desafio.getTentativas()).isZero();
    }

    @Test
    void devePersistirTentativaConceitualmenteSemCriarUsuarioQuandoCodigoForIncorreto() {
        DesafioAutenticacao desafio = desafio();
        prepararTelefone(desafio);
        when(desafioRepository.findByIdForUpdate(desafio.getId())).thenReturn(Optional.of(desafio));
        when(codeHasher.matches(desafio.getCodigoHmac(), desafio.getId(), TELEFONE, CODIGO))
                .thenReturn(false);

        ConfirmacaoCodigoResultado resultado = service.confirmar(
                desafio.getId(), CODIGO, null, UUID.randomUUID().toString());

        assertThat(resultado.foiConfirmado()).isFalse();
        assertThat(desafio.getTentativas()).isEqualTo((short) 1);
        assertThat(desafio.getConsumidoEm()).isNull();
        verify(usuarioService, never()).buscarOuCriarUsuarioPorTelefone(TELEFONE);
    }

    @Test
    void deveRejeitarDesafioInexistenteOuIndisponivelAntesDeDecifrarTelefone() {
        UUID inexistente = UUID.randomUUID();
        when(desafioRepository.findByIdForUpdate(inexistente)).thenReturn(Optional.empty());

        assertThat(service.confirmar(inexistente, CODIGO, null, UUID.randomUUID().toString())
                .foiConfirmado()).isFalse();

        DesafioAutenticacao consumido = desafio();
        consumido.consumir(AGORA.minusSeconds(1));
        when(desafioRepository.findByIdForUpdate(consumido.getId())).thenReturn(Optional.of(consumido));

        assertThat(service.confirmar(consumido.getId(), CODIGO, null, UUID.randomUUID().toString())
                .foiConfirmado()).isFalse();
        verify(phoneProtectionService, never()).decrypt(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deveFalharFechadoQuandoTelefoneNaoCorresponderAoHashPersistido() {
        DesafioAutenticacao desafio = desafio();
        when(desafioRepository.findByIdForUpdate(desafio.getId())).thenReturn(Optional.of(desafio));
        when(phoneProtectionService.decrypt(desafio.getTelefoneCriptografado())).thenReturn(TELEFONE);
        when(phoneProtectionService.matchesLookupHash(desafio.getTelefoneHash(), TELEFONE)).thenReturn(false);

        assertThatThrownBy(() -> service.confirmar(
                desafio.getId(), CODIGO, null, UUID.randomUUID().toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Integridade do telefone do desafio inválida");
        verify(codeHasher, never()).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private void prepararTelefone(DesafioAutenticacao desafio) {
        when(phoneProtectionService.decrypt(desafio.getTelefoneCriptografado())).thenReturn(TELEFONE);
        when(phoneProtectionService.matchesLookupHash(desafio.getTelefoneHash(), TELEFONE)).thenReturn(true);
    }

    private DesafioAutenticacao desafio() {
        return DesafioAutenticacao.criar(
                UUID.randomUUID(),
                "telefone-protegido",
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                AGORA.minusSeconds(60),
                AGORA.plusSeconds(240),
                AGORA
        );
    }
}
