# Segurança e proteção de dados

## Chave de proteção dos telefones

O `financial-service` exige `PHONE_ENCRYPTION_KEY`, uma chave Base64 que represente exatamente 32 bytes.

Gere a chave uma única vez:

```bash
openssl rand -base64 32
```

Armazene a chave no gerenciador de segredos do ambiente. Nunca a envie ao Git, coloque em imagens Docker ou compartilhe em logs e mensagens.

## Backup

O backup do PostgreSQL e o backup da chave devem ser guardados separadamente. Um backup do banco sem a chave não permite recuperar os números; banco e chave juntos devem ter acesso fortemente restrito.

Antes de deploys ou migrations:

1. faça backup do banco;
2. confirme que a chave atual está armazenada em local seguro;
3. teste a restauração em ambiente isolado;
4. monitore a inicialização do `financial-service`.

## Rotação da chave

Não substitua `PHONE_ENCRYPTION_KEY` diretamente. A troca direta impede descriptografar os valores existentes e altera os hashes usados nas pesquisas.

Uma rotação deve usar temporariamente a chave antiga e a nova:

1. descriptografar cada telefone com a chave antiga;
2. criptografá-lo e recalcular o HMAC com a chave nova;
3. atualizar ambos os campos na mesma transação;
4. validar todos os registros;
5. somente então remover a chave antiga.

## Modelo utilizado

- `telefone`: AES-256-GCM com IV aleatório, para confidencialidade e autenticação do dado.
- `telefone_hash`: HMAC-SHA-256 determinístico, para pesquisas e unicidade sem descriptografar a coluna.
- A aplicação normaliza os telefones antes de gerar ambos os valores.
