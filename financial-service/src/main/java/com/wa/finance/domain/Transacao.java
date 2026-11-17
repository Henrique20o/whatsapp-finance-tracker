package com.wa.finance.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_transacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transacao_external_message_id",
                columnNames = "external_message_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transacao {

    public enum Origem { WHATSAPP, MOBILE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deletado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    @Builder.Default
    private Origem origem = Origem.WHATSAPP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @jakarta.persistence.PrePersist
    void preencherDataHora() {
        if (dataHora == null) dataHora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    }
}
