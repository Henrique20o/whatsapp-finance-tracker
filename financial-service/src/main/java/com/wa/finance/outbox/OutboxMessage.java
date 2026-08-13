package com.wa.finance.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDENTE;

    @Column(nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private LocalDateTime proximaTentativaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;
}
