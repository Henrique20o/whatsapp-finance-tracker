package com.wa.finance.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyPhoneMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PhoneProtectionService phoneProtectionService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<StoredPhone> phones = jdbcTemplate.query(
                "select id, telefone, telefone_hash from tb_usuario",
                (resultSet, rowNumber) -> new StoredPhone(
                        resultSet.getLong("id"),
                        resultSet.getString("telefone"),
                        resultSet.getString("telefone_hash")
                )
        );

        int migrated = 0;
        for (StoredPhone stored : phones) {
            if (phoneProtectionService.isEncrypted(stored.value())) {
                String decrypted = phoneProtectionService.decrypt(stored.value());
                String expectedHash = phoneProtectionService.lookupHash(decrypted);
                if (stored.hash() == null || !stored.hash().equals(expectedHash)) {
                    jdbcTemplate.update("update tb_usuario set telefone_hash = ? where id = ?", expectedHash, stored.id());
                }
                continue;
            }

            String normalized = phoneProtectionService.normalize(stored.value());
            jdbcTemplate.update(
                    "update tb_usuario set telefone = ?, telefone_hash = ? where id = ?",
                    phoneProtectionService.encrypt(normalized),
                    phoneProtectionService.lookupHash(normalized),
                    stored.id()
            );
            migrated++;
        }

        if (migrated > 0) {
            log.info("Migração de proteção concluída para {} telefone(s)", migrated);
        }
    }

    private record StoredPhone(long id, String value, String hash) {}
}
