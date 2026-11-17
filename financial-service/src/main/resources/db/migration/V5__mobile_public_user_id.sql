ALTER TABLE tb_usuario ADD COLUMN mobile_public_id UUID;

UPDATE tb_usuario SET mobile_public_id = gen_random_uuid() WHERE mobile_public_id IS NULL;

ALTER TABLE tb_usuario ALTER COLUMN mobile_public_id SET DEFAULT gen_random_uuid();
ALTER TABLE tb_usuario ALTER COLUMN mobile_public_id SET NOT NULL;
ALTER TABLE tb_usuario ADD CONSTRAINT uk_usuario_mobile_public_id UNIQUE (mobile_public_id);
