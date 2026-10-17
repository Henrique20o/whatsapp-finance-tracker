# SDD — Separação das cadeias de segurança interna e mobile

- Cartão: `[05] Separar as cadeias de segurança interna e mobile`
- Status: implementado
- Data: 2026-10-14
- Serviço: `financial-service`

## 1. Objetivo

Separar, por caminho HTTP, os mecanismos de autenticação usados pelo `financial-service`:

- `/v1/**`: autenticação de serviços internos por `X-Internal-Api-Key`;
- `/mobile/v1/**`: autenticação de usuários por access token JWT, com exceções explícitas para os fluxos de código, refresh e logout;
- `/actuator/health`: acesso público para health check;
- qualquer caminho não previsto: acesso negado.

Ao final, uma API key interna não poderá autenticar uma rota mobile e um JWT mobile não poderá autenticar uma rota interna.

## 2. Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)
- [Configuração das chaves JWT](mobile-jwt-configuration.md)

## 3. Contexto atual

Existe uma única `SecurityFilterChain`. Nela, o `InternalApiKeyFilter` é inserido antes do `BearerTokenAuthenticationFilter` e todas as requisições, exceto o health check, precisam terminar autenticadas.

Esse desenho gera conflito porque filtros customizados continuam sendo executados mesmo quando uma rota está marcada com `permitAll()`. Portanto, liberar uma rota mobile na configuração de autorização não impediria o `InternalApiKeyFilter` de exigir a API key.

Exemplo do problema:

```http
GET /mobile/v1/transacoes
Authorization: Bearer <jwt-valido>
```

Antes de o JWT ser validado, o filtro interno procura `X-Internal-Api-Key`. Como o aplicativo não possui essa chave, a requisição recebe `401 Unauthorized`.

Também existe o risco inverso: se ambos os mecanismos permanecerem na mesma cadeia e a regra for apenas `authenticated()`, uma autenticação interna poderá satisfazer uma rota mobile, ou um JWT poderá satisfazer uma rota interna.

## 4. Escopo

### 4.1 Incluído

- criar cadeias de segurança distintas e ordenadas;
- limitar o `InternalApiKeyFilter` à API interna;
- limitar o Resource Server JWT à API mobile;
- declarar explicitamente as rotas mobile que não exigem access token;
- manter a aplicação stateless;
- negar rotas fora das fronteiras conhecidas;
- produzir respostas `401` e `403` previsíveis;
- criar testes positivos, negativos e de não-confusão entre credenciais;
- preservar o comportamento dos consumidores internos existentes.

### 4.2 Fora do escopo

- implementar controllers de código, verificação, refresh ou logout;
- validar de fato código de WhatsApp ou refresh token;
- persistir desafios e sessões;
- emitir access tokens;
- implementar autorização por proprietário dos dados;
- implementar rate limit;
- configurar CORS para uma versão web do aplicativo;
- alterar regras financeiras.

As rotas públicas de autenticação poderão chegar ao MVC e resultar em `404` enquanto seus controllers ainda não existirem. Isso significa que passaram pela camada de segurança, não que o recurso já foi implementado.

## 5. Matriz de segurança

| Caminho | Credencial esperada | Regra |
| --- | --- | --- |
| `/actuator/health` e subcaminhos | nenhuma | permitido |
| `POST /mobile/v1/auth/codes` | nenhuma | permitido pela camada JWT; controles de abuso virão depois |
| `POST /mobile/v1/auth/codes/verify` | nenhuma | permitido pela camada JWT; desafio e código serão validados depois |
| `POST /mobile/v1/auth/refresh` | refresh token no corpo | sem access token; o controller futuro validará o refresh token |
| `POST /mobile/v1/auth/logout` | refresh token no corpo | sem access token; operação futura será idempotente |
| demais `/mobile/v1/**` | `Authorization: Bearer <JWT>` | JWT obrigatório |
| `/v1/**` | `X-Internal-Api-Key` | API key obrigatória e autoridade `ROLE_INTERNAL` |
| qualquer outro caminho | nenhuma credencial aceita | negar por padrão |

`refresh` e `logout` são liberados apenas da exigência de access token JWT. Eles não serão operações sem autenticação: nos cartões futuros, o refresh token opaco será validado pelo serviço de sessão.

## 6. Arquitetura proposta

Serão registrados quatro beans de `SecurityFilterChain`. O Spring Security avalia as cadeias pela ordem e utiliza somente a primeira cujo `securityMatcher` corresponde à requisição.

```text
Requisição
    |
    +-- /actuator/health ----> Health chain ----> permitAll
    |
    +-- /mobile/v1/** -------> Mobile chain ----> JWT ou exceção explícita
    |
    +-- /v1/** --------------> Internal chain --> X-Internal-Api-Key
    |
    +-- qualquer outra ------> Fallback chain --> denyAll
```

### 6.1 Cadeia de health check — `@Order(1)`

Matcher:

```text
/actuator/health
/actuator/health/**
```

Configuração:

- `permitAll()`;
- nenhuma autenticação JWT;
- nenhum `InternalApiKeyFilter`;
- sem criação de sessão HTTP.

Somente o health check ficará público. Outros endpoints do Actuator não serão liberados por essa cadeia.

### 6.2 Cadeia mobile — `@Order(2)`

Matcher:

```text
/mobile/v1/**
```

Configuração:

- sessão `STATELESS`;
- CSRF desabilitado, pois a API não autentica por cookie;
- form login e HTTP Basic desabilitados;
- OAuth2 Resource Server com JWT habilitado;
- `InternalApiKeyFilter` ausente;
- exceções públicas limitadas a método e caminho;
- qualquer outra rota mobile exige autenticação JWT.

Exceções de access token:

```text
POST /mobile/v1/auth/codes
POST /mobile/v1/auth/codes/verify
POST /mobile/v1/auth/refresh
POST /mobile/v1/auth/logout
```

Os matchers devem incluir `HttpMethod.POST`. Isso evita liberar, sem necessidade, outros métodos HTTP no mesmo caminho.

### 6.3 Cadeia interna — `@Order(3)`

Matcher:

```text
/v1/**
```

Configuração:

- sessão `STATELESS`;
- CSRF, form login e HTTP Basic desabilitados;
- `InternalApiKeyFilter` adicionado antes do filtro de autorização;
- acesso exige `ROLE_INTERNAL`;
- Resource Server JWT ausente.

O filtro continuará comparando a chave em tempo constante e, após sucesso, criará uma autenticação com principal `internal-service` e autoridade `ROLE_INTERNAL`.

### 6.4 Cadeia de fallback — menor prioridade

Matcher:

```text
qualquer requisição não atendida anteriormente
```

Configuração:

- `denyAll()`;
- nenhum mecanismo de autenticação;
- sessão `STATELESS`.

Uma API key ou um JWT válido não muda essa decisão. Isso impede que endpoints adicionados acidentalmente fora de `/v1/**` e `/mobile/v1/**` sejam expostos.

## 7. Alterações previstas no código

### 7.1 `SecurityConfiguration`

Substituir a cadeia única por quatro cadeias nomeadas, cada uma com `@Order` e `securityMatcher` próprios.

Estrutura conceitual:

```java
@Bean
@Order(1)
SecurityFilterChain healthSecurity(HttpSecurity http) { ... }

@Bean
@Order(2)
SecurityFilterChain mobileSecurity(HttpSecurity http) { ... }

@Bean
@Order(3)
SecurityFilterChain internalSecurity(
        HttpSecurity http,
        InternalApiKeyFilter internalApiKeyFilter
) { ... }

@Bean
@Order(4)
SecurityFilterChain fallbackSecurity(HttpSecurity http) { ... }
```

Uma função privada poderá concentrar apenas configurações realmente compartilhadas, como `STATELESS`, CSRF, form login e HTTP Basic. Matchers, filtros e autorização devem permanecer visíveis em cada cadeia para reduzir o risco de mistura acidental.

### 7.2 `InternalApiKeyFilter`

O filtro continuará como bean Spring, mas será adicionado somente à cadeia interna.

O método `shouldNotFilter()` poderá ser removido, porque a seleção por `/v1/**` passará a ser responsabilidade da cadeia. Como defesa adicional, ele poderá validar que está processando uma rota interna, mas essa checagem não deve substituir o `securityMatcher`.

O `FilterRegistrationBean` desabilitado deverá continuar existindo. Sem ele, o Spring Boot poderá registrar o filtro também como filtro servlet global, recriando o conflito que este cartão pretende eliminar.

### 7.3 Tratamento de erros

Definir `AuthenticationEntryPoint` e `AccessDeniedHandler` para evitar respostas HTML ou redirecionamentos.

- credencial ausente ou inválida: `401 Unauthorized`;
- usuário autenticado sem permissão: `403 Forbidden`;
- corpo JSON e `Content-Type: application/json`;
- não incluir token, API key ou detalhes internos da validação.

A resposta mobile deverá seguir o formato `ErrorResponse` do OpenAPI quando houver informações suficientes, incluindo `code`, `message`, `timestamp`, `path` e `correlationId`. A API interna poderá preservar temporariamente o formato atual para não quebrar consumidores; uma padronização completa pode ser feita separadamente.

## 8. Regras e invariantes

1. O `InternalApiKeyFilter` nunca executa em `/mobile/v1/**`.
2. O `BearerTokenAuthenticationFilter` nunca autentica `/v1/**`.
3. A API key não é armazenada no `SecurityContext` como credencial.
4. Nenhuma cadeia cria `HttpSession` ou cookie `JSESSIONID`.
5. Somente os quatro `POST` de autenticação listados ficam livres de access token.
6. `permitAll()` não deve ser usado como substituto da separação de filtros.
7. A ordem das cadeias é explícita e coberta por testes.
8. Rotas desconhecidas são negadas mesmo com credencial válida.
9. O JWT continua validando assinatura, emissor, audiência e expiração por meio do `JwtDecoder` criado no cartão [04].
10. Segredos e tokens nunca aparecem em logs ou mensagens de erro.

## 9. Estratégia de testes

### 9.1 Testes HTTP de separação

| Cenário | Resultado esperado |
| --- | --- |
| health sem credencial | permitido |
| rota pública de autenticação sem credencial | passa pela segurança |
| rota mobile protegida sem JWT | `401` |
| rota mobile protegida com JWT válido | passa pela segurança |
| rota mobile protegida somente com API key | `401` |
| rota interna sem API key | `401` |
| rota interna com API key válida | passa pela segurança |
| rota interna somente com JWT válido | `401` |
| rota interna com API key inválida | `401` |
| rota mobile com JWT inválido ou expirado | `401` |
| rota desconhecida sem credencial | negada |
| rota desconhecida com API key válida | negada |
| rota desconhecida com JWT válido | negada |
| qualquer fluxo anterior | ausência de `JSESSIONID` |

Enquanto os controllers mobile não existirem, testes poderão usar um controller somente de teste nos caminhos necessários. Isso permite diferenciar claramente “passou pela segurança” de “retornou 404 porque o controller ainda não existe”.

O `spring-security-test` poderá criar uma autenticação JWT para testar autorização e separação de cadeias. Os testes existentes de `JwtDecoder` continuarão responsáveis pela validação criptográfica real.

### 9.2 Testes do filtro interno

- chave correta cria principal `internal-service`;
- autenticação contém `ROLE_INTERNAL`;
- credenciais do principal ficam vazias;
- chave ausente ou incorreta retorna `401`;
- comparação da chave continua usando `MessageDigest.isEqual`;
- filtro não é registrado globalmente.

### 9.3 Regressão

- testes atuais dos controllers e serviços internos continuam passando;
- consumidores internos continuam usando apenas `X-Internal-Api-Key`;
- configuração JWT do cartão [04] continua carregando;
- health check continua acessível ao Docker Compose;
- suíte completa passa quando Docker/Testcontainers estiver disponível.

## 10. Sequência de implementação

1. Criar constantes ou matchers explícitos para os quatro grupos de rota.
2. Extrair a configuração stateless comum sem esconder regras de autorização.
3. Criar a cadeia de health check.
4. Criar a cadeia mobile e suas quatro exceções de access token.
5. Criar a cadeia interna e mover para ela o `InternalApiKeyFilter`.
6. Criar a cadeia fallback com `denyAll()`.
7. Remover do filtro interno a responsabilidade de decidir sobre health e mobile.
8. Implementar respostas JSON para autenticação e acesso negado.
9. Atualizar e ampliar os testes HTTP.
10. Executar testes unitários, de contexto e a suíte completa.

## 11. Critérios de aceite

O cartão poderá ser concluído quando houver evidência automatizada de que:

- `/v1/**` aceita somente a API key interna;
- rotas mobile protegidas aceitam somente JWT;
- rotas mobile de autenticação previstas não exigem access token;
- health check permanece público;
- rotas desconhecidas permanecem bloqueadas;
- nenhuma resposta cria sessão HTTP;
- o filtro interno não executa fora da cadeia interna;
- os testes existentes não apresentam regressão;
- a documentação permanece consistente com o OpenAPI e o ADR.

## 12. Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| fallback com prioridade maior bloquear todas as rotas | usar `@Order` explícito e teste para cada grupo |
| filtro interno ser registrado globalmente | manter `FilterRegistrationBean` com registro desabilitado |
| liberar todos os métodos de uma rota pública | usar matcher com `HttpMethod.POST` |
| API key autenticar rota mobile | não adicionar o filtro à cadeia mobile e testar o cenário negativo |
| JWT autenticar rota interna | não habilitar Resource Server na cadeia interna e exigir `ROLE_INTERNAL` |
| novo endpoint ficar público por engano | fallback `denyAll()` e prefixos obrigatórios |
| confundir rota pública com operação sem segurança | manter validação de código/refresh no domínio nos cartões posteriores |

## 13. Resultado esperado

Após o cartão [05], a infraestrutura aceitará dois modelos de autenticação no mesmo serviço sem misturar suas autoridades:

```text
Serviços internos ── X-Internal-Api-Key ──> /v1/**

Aplicativo mobile ── JWT ────────────────> /mobile/v1/**

Orquestrador ─────── sem credencial ─────> /actuator/health
```

Esse isolamento prepara com segurança as migrations de desafios e sessões, os endpoints de código via WhatsApp e a futura autorização por usuário do JWT.

## 14. Evidência da implementação

Implementado em 2026-10-14:

- quatro `SecurityFilterChain` ordenadas para health, mobile, API interna e fallback;
- Resource Server JWT presente somente na cadeia mobile;
- `InternalApiKeyFilter` presente somente na cadeia `/v1/**`;
- quatro operações públicas de autenticação limitadas ao método `POST`;
- fallback retornando `403` para rotas fora das fronteiras conhecidas;
- respostas JSON mobile com código, mensagem, timestamp, caminho e correlation ID;
- testes com JWT RSA real, API key, credenciais cruzadas, token inválido, token expirado, métodos HTTP e ausência de sessão.

Validação executada:

```text
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

O teste `FlywayPostgresIntegrationTest` foi excluído dessa execução porque depende de Testcontainers e o Docker Desktop não estava ativo no ambiente local. Nenhuma alteração deste cartão envolve migrations ou persistência.
