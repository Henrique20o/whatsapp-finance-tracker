package com.wa.finance.service;

import com.wa.finance.domain.DesafioAutenticacao;
import com.wa.finance.repository.DesafioAutenticacaoRepository;
import com.wa.finance.security.ClientIpProtectionService;
import com.wa.finance.security.PhoneProtectionService;
import com.wa.finance.security.VerificationCodeGenerator;
import com.wa.finance.security.VerificationCodeHasher;
import com.wa.finance.security.VerificationCodeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class DesafioAutenticacaoService {

    private final PhoneProtectionService phoneProtectionService;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationCodeHasher codeHasher;
    private final ClientIpProtectionService ipProtectionService;
    private final DesafioAutenticacaoRepository repository;
    private final MobileAuthRateLimitService rateLimitService;
    private final VerificationCodeProperties properties;
    private final Clock clock;

    public DesafioAutenticacaoService(
            PhoneProtectionService phoneProtectionService,
            VerificationCodeGenerator codeGenerator,
            VerificationCodeHasher codeHasher,
            ClientIpProtectionService ipProtectionService,
            DesafioAutenticacaoRepository repository,
            MobileAuthRateLimitService rateLimitService,
            VerificationCodeProperties properties,
            Clock clock
    ) {
        this.phoneProtectionService = phoneProtectionService;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.ipProtectionService = ipProtectionService;
        this.repository = repository;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public DesafioCriado criar(String telefone, String enderecoIpCanonico) {
        String telefoneNormalizado = phoneProtectionService.normalize(telefone);
        String telefoneHash = phoneProtectionService.lookupHash(telefoneNormalizado);
        String ipHash = ipProtectionService.hash(enderecoIpCanonico);

        Instant agora = clock.instant();
        RateLimitDecision rateLimit = rateLimitService.evaluate(telefoneHash, ipHash, agora);
        if (!rateLimit.allowed()) {
            throw new RateLimitExceededException(
                    rateLimit.retryAfterSeconds(),
                    rateLimit.scope(),
                    rateLimit.rule()
            );
        }
        repository.invalidarNaoFinalizadosPorTelefone(telefoneHash, agora);

        UUID challengeId = UUID.randomUUID();
        String codigo = codeGenerator.generate();
        String codigoHmac = codeHasher.hash(challengeId, telefoneNormalizado, codigo);
        String telefoneCriptografado = phoneProtectionService.encrypt(telefoneNormalizado);
        Instant expiraEm = agora.plus(properties.getTtl());
        Instant reenvioPermitidoEm = agora.plus(properties.getResendInterval());

        repository.save(DesafioAutenticacao.criar(
                challengeId,
                telefoneCriptografado,
                telefoneHash,
                codigoHmac,
                ipHash,
                agora,
                expiraEm,
                reenvioPermitidoEm
        ));

        return new DesafioCriado(
                challengeId,
                telefoneNormalizado,
                codigo,
                expiraEm,
                reenvioPermitidoEm
        );
    }

}
