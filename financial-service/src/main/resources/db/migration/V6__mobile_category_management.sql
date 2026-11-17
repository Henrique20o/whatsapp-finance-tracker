ALTER TABLE tb_categoria ADD COLUMN sistema BOOLEAN NOT NULL DEFAULT false;
UPDATE tb_categoria SET sistema = true WHERE lower(trim(nome)) = 'outros';

WITH duplicates AS (
    SELECT id, usuario_id, lower(regexp_replace(trim(nome), '[[:space:]]+', ' ', 'g')) AS normalized_name,
           row_number() OVER (PARTITION BY usuario_id,
               lower(regexp_replace(trim(nome), '[[:space:]]+', ' ', 'g')) ORDER BY id) AS position
      FROM tb_categoria
     WHERE ativa = true
)
UPDATE tb_categoria c SET ativa = false
  FROM duplicates d
 WHERE c.id = d.id AND d.position > 1;

CREATE UNIQUE INDEX uk_categoria_usuario_nome_ativo_normalizado
    ON tb_categoria (usuario_id, lower(regexp_replace(trim(nome), '[[:space:]]+', ' ', 'g'))) WHERE ativa = true;
