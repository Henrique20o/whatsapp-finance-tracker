# Configuração das chaves JWT da API mobile

O `financial-service` assina access tokens com RSA e valida os tokens usando a chave pública correspondente. As chaves de produção não devem ser versionadas.

## Gerar um par de chaves para desenvolvimento

Crie um diretório local `secrets` na raiz do projeto e execute:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/mobile-jwt-private.pem
openssl pkey -in secrets/mobile-jwt-private.pem -pubout -out secrets/mobile-jwt-public.pem
```

O diretório `secrets` é montado como somente leitura em `/run/secrets` pelo Docker Compose. Configure `MOBILE_JWT_KEYS_DIR` com outro caminho absoluto se as chaves estiverem em outro local.

Em uma execução local fora do Docker, informe recursos Spring válidos:

```text
MOBILE_JWT_PRIVATE_KEY=file:C:/caminho/seguro/mobile-jwt-private.pem
MOBILE_JWT_PUBLIC_KEY=file:C:/caminho/seguro/mobile-jwt-public.pem
```

A aplicação interrompe a inicialização se as chaves estiverem ausentes, forem inválidas, tiverem menos de 2048 bits ou não formarem um par.

## Configurações

| Variável | Padrão | Finalidade |
|---|---:|---|
| `MOBILE_JWT_ISSUER` | `https://api.whatsapp-finance-tracker.local` | URI HTTPS que identifica quem emitiu o token |
| `MOBILE_JWT_AUDIENCE` | `finance-tracker-mobile` | Restringe o token ao aplicativo mobile |
| `MOBILE_JWT_ACCESS_TOKEN_TTL` | `15m` | Duração do access token |
| `MOBILE_REFRESH_TOKEN_INACTIVITY` | `30d` | Inatividade máxima da sessão renovável |
| `MOBILE_SESSION_ABSOLUTE_TTL` | `90d` | Duração absoluta máxima da sessão |

Os tempos de refresh serão consumidos quando as sessões mobile forem implementadas. Eles já são validados agora para impedir configurações incoerentes.
