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
| `MOBILE_JWT_ACTIVE_KID` | `v1` | Identificador versionado da chave que assina novos tokens |
| `MOBILE_JWT_CLOCK_SKEW` | `30s` | Tolerância explícita de relógio na validação |
| `MOBILE_JWT_V1_PUBLIC_KEY` | chave pública atual | Chave pública aceita para tokens com `kid=v1` |
| `MOBILE_REFRESH_TOKEN_INACTIVITY` | `30d` | Inatividade máxima da sessão renovável |
| `MOBILE_SESSION_ABSOLUTE_TTL` | `90d` | Duração absoluta máxima da sessão |

Os tempos de refresh serão consumidos quando as sessões mobile forem implementadas. Eles já são validados agora para impedir configurações incoerentes.

## Rotacionar chaves sem derrubar sessões

1. Gere um novo par fora do repositório e publique a chave pública no diretório de segredos.
2. Configure `MOBILE_JWT_PRIVATE_KEY` e `MOBILE_JWT_PUBLIC_KEY` para o novo par e mude `MOBILE_JWT_ACTIVE_KID` para um identificador novo, como `v2`.
3. Preserve a chave pública anterior no mapa `app.security.mobile.public-keys.v1` (no Compose, `MOBILE_JWT_V1_PUBLIC_KEY`). O decoder escolhe a chave pelo `kid`, aceita apenas RS256 e mantém a chave antiga válida para os JWTs já emitidos.
4. Depois de pelo menos 15 minutos, mais a tolerância de relógio e a margem operacional, remova a chave pública antiga. A chave privada anterior deixa de assinar imediatamente.
5. Para rollback, volte o `kid` ativo e o par privado/público anterior, mantendo todas as chaves públicas que ainda possam validar tokens não expirados.

Para rotações seguintes, acrescente outra entrada `app.security.mobile.public-keys.<kid>` na configuração externa. Não sobrescreva nem reutilize um `kid` para outra chave. A aplicação falha na inicialização se o par ativo estiver ausente, inválido ou não corresponder.
