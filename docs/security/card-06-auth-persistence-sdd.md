# SDD — Persistência de desafios de autenticação e sessões mobile

- Cartão: `[06] Criar migrations de desafios e sessões mobile`
- Status: implementado
- Data: 2026-10-14
- Serviço: `financial-service`

## 1. Objetivo

Criar, por meio do Flyway, o modelo PostgreSQL necessário para persistir:

- desafios temporários de autenticação enviados por WhatsApp;
- tentativas, expiração, consumo e invalidação desses desafios;
- sessões mobile revogáveis;
- hashes de refresh tokens e seu histórico de rotação;
- dados suficientes para detectar reutilização de refresh token.

A persistência deve permitir que os fluxos de autenticação funcionem de forma consistente após reinícios da aplicação e quando houver mais de uma instância do `financial-service`.

## 2. Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)
- [Configuração das chaves JWT](mobile-jwt-configuration.md)
- [SDD do cartão 05 — Separação das cadeias de segurança](card-05-security-chains-sdd.md)

## 3. Contexto atual

O `financial-service` possui duas migrations:

- `V1__baseline_schema.sql`: usuários, categorias e transações;
- `V2__transactional_outbox.sql`: outbox transacional.

A tabela `tb_usuario` já armazena o telefone criptografado em `telefone` e um hash de busca em `telefone_hash`. O `PhoneProtectionService` normaliza, criptografa e calcula o hash do telefone.

As cadeias de segurança do cartão [05] já deixam públicas, na camada JWT, as operações de solicitação de código, verificação, refresh e logout. Entretanto, os controllers e as regras desses fluxos ainda não existem.

Sem persistência compartilhada, tentativas e expirações seriam perdidas em reinícios, duas instâncias poderiam aceitar o mesmo código ou refresh token e o logout não teria uma sessão durável para revogar.

## 4. Escopo

### 4.1 Incluído

- criar a migration `V3__mobile_authentication.sql`;
- criar `tb_desafio_autenticacao`;
- criar `tb_sessao_mobile`;
- adicionar chaves primárias, estrangeiras, restrições e índices;
- armazenar telefone de forma protegida;
- armazenar somente HMAC do código e hash do refresh token;
- suportar rotação e detecção de reutilização de refresh tokens;
- atualizar o teste de integração do Flyway para validar a V3 em PostgreSQL real.

### 4.2 Fora do escopo

- criar entidades JPA e repositories;
- implementar geração ou envio do código pelo WhatsApp;
- implementar controllers e services de autenticação;
- emitir access tokens ou refresh tokens;
- implementar rate limiting;
- executar limpeza periódica de registros expirados;
- alterar o contrato OpenAPI;
- migrar os campos temporais das tabelas existentes para `TIMESTAMPTZ`.

Este cartão cria a estrutura que os cartões posteriores usarão. Ele não torna os endpoints de autenticação funcionais.

## 5. Decisões de modelagem

### 5.1 UUIDs gerados pela aplicação

Desafios, sessões e registros de refresh token usarão UUID. A aplicação gerará os valores antes da inserção, portanto a migration não dependerá de extensões PostgreSQL como `uuid-ossp` ou `pgcrypto`.

O UUID do desafio poderá ser exposto como `challengeId`. O UUID da família de sessão poderá ser usado no claim `sid` do access token. IDs sequenciais do banco não serão expostos nesses fluxos.

### 5.2 Instantes com fuso horário

Todos os campos temporais novos usarão `TIMESTAMPTZ`. A aplicação trabalhará com `Instant` e persistirá os instantes em UTC.

Isso evita interpretar expiração de forma diferente conforme o fuso horário do processo ou do banco.

### 5.3 Segredos nunca serão persistidos em texto puro

- telefone: valor criptografado e hash de busca;
- código: HMAC-SHA-256 em hexadecimal, com 64 caracteres;
- IP usado nos limites de abuso: HMAC-SHA-256 em hexadecimal;
- refresh token: SHA-256 em hexadecimal, com 64 caracteres. Como o token possui 256 bits aleatórios, não necessita de senha ou salt para resistir a tentativa exaustiva.

O código original e o refresh token original existem apenas durante o processamento necessário para envio ou resposta e nunca são gravados no banco.

O segredo do HMAC do código não deve ser a mesma chave usada para criptografar telefones.

### 5.4 Histórico da rotação na própria tabela de sessões

Para cumprir o escopo de duas tabelas sem perder a capacidade de detectar reutilização, cada linha de `tb_sessao_mobile` representará uma geração de refresh token, e não apenas o estado atual da sessão.

Todas as gerações de uma mesma sessão compartilharão `sessao_id`. Cada nova geração apontará para a anterior por `token_anterior_id`.

```text
sessao_id S1

token A (usado) <- token B (usado) <- token C (ativo)
```

O claim `sid` permanece `S1` durante toda a sessão. Ao receber novamente o token A ou B, o serviço localizará seu hash, reconhecerá que `usado_em` já está preenchido e poderá revogar todas as linhas da família S1.

Essa escolha evita sobrescrever o hash anterior, o que tornaria impossível detectar a reutilização de tokens antigos.

## 6. Tabela `tb_desafio_autenticacao`

### 6.1 Finalidade

Representar uma solicitação de código e controlar seu ciclo de vida até o consumo, bloqueio, invalidação ou expiração.

Como a requisição de verificação contém apenas `challengeId`, código e nome opcional do dispositivo, o desafio precisa preservar de forma recuperável o telefone confirmado. Isso permitirá localizar ou criar o usuário depois da validação sem exigir que o aplicativo reenvie o telefone.

### 6.2 Colunas propostas

| Coluna | Tipo | Nulo | Finalidade |
| --- | --- | --- | --- |
| `id` | `UUID` | não | `challengeId` gerado pela aplicação e chave primária |
| `telefone_criptografado` | `VARCHAR(512)` | não | telefone recuperável somente pelo servidor |
| `telefone_hash` | `CHAR(64)` | não | busca, invalidação do desafio anterior e limites por telefone |
| `codigo_hmac` | `CHAR(64)` | não | HMAC do `challengeId`, telefone normalizado e código |
| `ip_hash` | `CHAR(64)` | não | contagem compartilhada de solicitações por IP sem guardar o IP original |
| `tentativas` | `SMALLINT` | não | quantidade de confirmações incorretas, com `DEFAULT 0` |
| `criado_em` | `TIMESTAMPTZ` | não | instante de criação, com `DEFAULT CURRENT_TIMESTAMP` |
| `expira_em` | `TIMESTAMPTZ` | não | limite de cinco minutos para confirmação |
| `reenvio_permitido_em` | `TIMESTAMPTZ` | não | fim do intervalo de sessenta segundos |
| `consumido_em` | `TIMESTAMPTZ` | sim | instante da confirmação bem-sucedida |
| `invalidado_em` | `TIMESTAMPTZ` | sim | bloqueio por tentativas ou substituição por novo desafio |

### 6.3 Restrições

- chave primária em `id`;
- `tentativas` entre zero e cinco;
- `expira_em > criado_em`;
- `reenvio_permitido_em > criado_em` e `reenvio_permitido_em <= expira_em`;
- `consumido_em` e `invalidado_em` não podem estar preenchidos simultaneamente;
- consumo e invalidação, quando presentes, não podem ser anteriores à criação;
- índice único parcial por `telefone_hash` enquanto `consumido_em` e `invalidado_em` forem nulos.

O índice único parcial garante no máximo um desafio não finalizado por telefone. Como o PostgreSQL não permite usar `now()` em um predicado de índice imutável, uma linha apenas expirada ainda precisa ser explicitamente invalidada antes da criação do próximo desafio.

### 6.4 Índices

| Índice | Colunas/predicado | Uso |
| --- | --- | --- |
| `uk_desafio_auth_telefone_ativo` | `telefone_hash`, somente não finalizados | impedir dois desafios ativos do mesmo telefone |
| `idx_desafio_auth_telefone_criado` | `telefone_hash, criado_em DESC` | limites de cinco por hora e dez por dia |
| `idx_desafio_auth_ip_criado` | `ip_hash, criado_em DESC` | limite de trinta solicitações por IP/hora |
| `idx_desafio_auth_expiracao` | `expira_em` | busca e futura limpeza de expirados |

O acesso normal para confirmação será feito pela chave primária `id` e não necessita de índice adicional.

## 7. Tabela `tb_sessao_mobile`

### 7.1 Finalidade

Manter o histórico de refresh tokens de uma sessão, permitindo rotação, logout, expiração por inatividade, limite absoluto e detecção de reutilização.

O access token JWT não será salvo. Ele é validado por assinatura e possui vida curta. Após o logout, um access token emitido anteriormente poderá continuar válido até completar seus quinze minutos, conforme decisão de segurança do MVP.

### 7.2 Colunas propostas

| Coluna | Tipo | Nulo | Finalidade |
| --- | --- | --- | --- |
| `id` | `UUID` | não | identificador desta geração do refresh token |
| `sessao_id` | `UUID` | não | família estável da sessão e valor do claim JWT `sid` |
| `usuario_id` | `BIGINT` | não | proprietário, referenciando `tb_usuario(id)` |
| `refresh_token_hash` | `CHAR(64)` | não | hash único do refresh token opaco |
| `token_anterior_id` | `UUID` | sim | geração imediatamente anterior da mesma família |
| `nome_dispositivo` | `VARCHAR(120)` | sim | informação opcional exibida ao usuário; não autentica o dispositivo |
| `criado_em` | `TIMESTAMPTZ` | não | emissão desta geração, com `DEFAULT CURRENT_TIMESTAMP` |
| `expira_inatividade_em` | `TIMESTAMPTZ` | não | emissão mais trinta dias, limitada pela expiração absoluta |
| `expira_absoluto_em` | `TIMESTAMPTZ` | não | criação da família mais noventa dias, igual em todas as gerações |
| `usado_em` | `TIMESTAMPTZ` | sim | preenchido quando o token é rotacionado |
| `revogado_em` | `TIMESTAMPTZ` | sim | preenchido no logout, reutilização ou revogação administrativa |
| `motivo_revogacao` | `VARCHAR(40)` | sim | motivo técnico controlado pela aplicação |

Valores esperados para `motivo_revogacao` inicialmente:

- `LOGOUT`;
- `REUTILIZACAO`;
- `EXPIRACAO`;
- `ADMINISTRATIVO`.

Os valores não serão restringidos por enum PostgreSQL nem por `CHECK` nesta migration. Manter o domínio no código evita uma migration apenas para acrescentar um novo motivo operacional.

### 7.3 Restrições

- chave primária em `id`;
- chave estrangeira `usuario_id -> tb_usuario(id)` sem exclusão em cascata;
- chave estrangeira autorreferente composta por `token_anterior_id`, `sessao_id` e `usuario_id`, garantindo que a geração anterior pertença à mesma família e ao mesmo usuário;
- `refresh_token_hash` único;
- `token_anterior_id` único, impedindo duas continuações da mesma geração;
- `expira_inatividade_em > criado_em`;
- `expira_absoluto_em >= expira_inatividade_em`;
- uso e revogação, quando presentes, não podem ser anteriores à criação;
- `revogado_em` e `motivo_revogacao` devem estar ambos nulos ou ambos preenchidos;
- índice único parcial por `sessao_id` para linhas com `usado_em IS NULL` e `revogado_em IS NULL`.

Na rotação, a aplicação deve calcular:

```text
expira_inatividade_em = menor valor entre:
  agora + 30 dias
  expira_absoluto_em
```

Uma sessão que já alcançou o limite absoluto não pode ser renovada.

### 7.4 Índices

| Índice | Colunas/predicado | Uso |
| --- | --- | --- |
| `uk_sessao_mobile_refresh_hash` | `refresh_token_hash` | localizar token apresentado e impedir duplicidade |
| `uk_sessao_mobile_token_anterior` | `token_anterior_id`, quando não nulo | impedir duas rotações concorrentes do mesmo token |
| `uk_sessao_mobile_familia_ativa` | `sessao_id`, somente não usados e não revogados | permitir uma única geração corrente por família |
| `idx_sessao_mobile_familia` | `sessao_id` | consultar e revogar toda a família |
| `idx_sessao_mobile_usuario_ativa` | `usuario_id`, somente não usados e não revogados | listar sessões atuais de um usuário |
| `idx_sessao_mobile_expiracao` | `expira_inatividade_em, expira_absoluto_em` | busca e futura limpeza de expirados |

## 8. Operações que o esquema deverá suportar

### 8.1 Solicitar código

Em uma transação:

1. normalizar e proteger telefone e IP;
2. invalidar eventual desafio não finalizado daquele telefone;
3. inserir o novo desafio;
4. confirmar a transação antes de solicitar o envio assíncrono.

A integração com WhatsApp será implementada depois. A migration apenas garante o estado persistente necessário.

### 8.2 Confirmar código

Em uma transação:

1. buscar o desafio pelo UUID com bloqueio de linha;
2. conferir se não foi consumido, invalidado ou expirado;
3. incrementar tentativas em caso de erro e invalidar ao alcançar cinco;
4. em caso de sucesso, preencher `consumido_em` exatamente uma vez;
5. localizar ou criar o usuário;
6. inserir a primeira geração de `tb_sessao_mobile`;
7. emitir os tokens somente depois do commit bem-sucedido.

O bloqueio transacional e o estado persistido impedirão duas confirmações concorrentes do mesmo desafio.

### 8.3 Renovar sessão

Em uma transação:

1. calcular o hash do refresh token recebido;
2. buscar a geração correspondente com bloqueio de linha;
3. rejeitar expiração, revogação ou uso anterior;
4. se `usado_em` já estiver preenchido, revogar toda a família como reutilização;
5. preencher `usado_em` na geração atual;
6. inserir uma nova geração com o mesmo `sessao_id` e referência à anterior;
7. emitir o novo par de tokens somente depois do commit.

As restrições únicas funcionam como última defesa caso duas requisições tentem rotacionar o mesmo token simultaneamente.

### 8.4 Logout

O serviço localizará a família pelo hash recebido e preencherá `revogado_em` e `motivo_revogacao = 'LOGOUT'` em todas as suas gerações ainda não revogadas.

A operação será idempotente: token inexistente, família já revogada ou repetição do mesmo logout não expõem diferença ao cliente e continuam resultando em `204 No Content`.

## 9. Migration Flyway

Criar:

```text
financial-service/src/main/resources/db/migration/
└── V3__mobile_authentication.sql
```

Diretrizes:

- a migration é somente de avanço; não haverá script automático de rollback;
- não alterar V1 ou V2 depois de aplicadas;
- não usar `IF NOT EXISTS`, pois divergências de schema devem falhar visivelmente;
- nomear constraints e índices para facilitar diagnóstico;
- criar primeiro `tb_desafio_autenticacao` e depois `tb_sessao_mobile`;
- declarar explicitamente todas as chaves estrangeiras e regras de exclusão;
- não inserir segredos ou dados de exemplo.

Ambientes que já possuem V1 e V2 receberão somente V3. Bancos vazios executarão V1, V2 e V3 na ordem.

## 10. Estratégia de testes

Atualizar `FlywayPostgresIntegrationTest`, que utiliza PostgreSQL 15 por Testcontainers.

### 10.1 Aplicação e validação da migration

- iniciar um PostgreSQL vazio;
- aplicar V1, V2 e V3;
- confirmar as versões `1, 2, 3` em `flyway_schema_history`;
- confirmar que o contexto inicia com `spring.jpa.hibernate.ddl-auto=validate`;
- confirmar a existência das duas novas tabelas.

### 10.2 Estrutura

- validar nomes e tipos das colunas críticas;
- validar nulabilidade e valores padrão;
- validar chave estrangeira para `tb_usuario`;
- validar chave estrangeira autorreferente;
- validar a existência dos índices previstos.

### 10.3 Restrições do desafio

- aceitar um desafio válido;
- rejeitar tentativas menores que zero ou maiores que cinco;
- rejeitar expiração anterior ou igual à criação;
- rejeitar desafio simultaneamente consumido e invalidado;
- rejeitar dois desafios não finalizados do mesmo telefone;
- permitir novo desafio após invalidar o anterior.

### 10.4 Restrições da sessão

- aceitar a primeira geração de uma sessão para usuário existente;
- rejeitar usuário inexistente;
- rejeitar hash de refresh token duplicado;
- rejeitar duas novas gerações apontando para o mesmo token anterior;
- rejeitar duas gerações correntes da mesma família;
- aceitar uma nova geração depois de marcar a anterior como usada;
- rejeitar datas de expiração inconsistentes;
- rejeitar motivo de revogação sem `revogado_em`.

Os testes de concorrência do consumo e da rotação serão adicionados com os services, pois dependem das fronteiras transacionais da aplicação. Neste cartão, as restrições de banco que dão suporte a esses cenários serão validadas diretamente.

## 11. Critérios de aceite

O cartão poderá ser concluído quando:

- `V3__mobile_authentication.sql` funcionar em PostgreSQL 15 vazio e em schema contendo V1 e V2;
- o Flyway validar a migration sem erros;
- `tb_desafio_autenticacao` e `tb_sessao_mobile` existirem com as constraints propostas;
- o banco impedir múltiplos desafios não finalizados por telefone;
- o banco impedir duas gerações correntes na mesma família de sessão;
- o histórico permitir identificar refresh token já utilizado;
- códigos e refresh tokens originais não tiverem colunas de armazenamento;
- telefone e IP não forem armazenados em texto puro nas novas tabelas;
- o teste de integração cobrir migrations, índices, chaves estrangeiras e restrições críticas;
- a suíte existente não apresentar regressão.

## 12. Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| sobrescrever refresh hash e perder detecção de reutilização | manter uma linha por geração e uma família estável por `sessao_id` |
| duas renovações aceitarem o mesmo token | bloqueio transacional no serviço e constraints únicas no banco |
| duas verificações consumirem o mesmo código | bloqueio da linha do desafio e atualização atômica |
| desafio expirado bloquear uma nova solicitação pelo índice parcial | invalidar explicitamente o desafio anterior antes da nova inserção |
| guardar telefone ou IP em texto puro | criptografia do telefone e HMAC dos identificadores de busca |
| comparação incorreta de horários | `TIMESTAMPTZ`, `Instant` e UTC |
| exclusão de usuário apagar evidências de segurança | chave estrangeira sem `ON DELETE CASCADE` |
| crescimento contínuo das tabelas | prever limpeza posterior, fora deste cartão, com índices de expiração |
| migration funcionar no H2 e falhar em produção | validar exclusivamente o comportamento relevante em PostgreSQL real via Testcontainers |

## 13. Resultado esperado

Após o cartão [06], o banco terá a base necessária para os próximos fluxos:

```text
telefone + código
       |
       v
tb_desafio_autenticacao
       |
       | confirmação única
       v
tb_usuario <── tb_sessao_mobile (família S1)
                         |
                         +── geração A: usada
                         +── geração B: usada
                         └── geração C: atual
```

O resultado ainda não será uma autenticação utilizável pelo aplicativo. Ele será uma camada de persistência verificável, preparada para receber as entidades, repositories e regras transacionais dos cartões seguintes.

## 14. Evidência da implementação

Implementado em 2026-10-14:

- migration `V3__mobile_authentication.sql` com as tabelas de desafios e sessões;
- proteção contra mais de um desafio não finalizado por telefone;
- índices para limites por telefone, IP e expiração;
- histórico de rotações na própria `tb_sessao_mobile`;
- vínculo composto que impede uma geração anterior de outra família ou usuário;
- proteção contra duas gerações correntes ou duas continuações do mesmo token;
- restrições de tentativas, datas, finalização e revogação;
- teste PostgreSQL para schema, índices, chaves estrangeiras e ciclos de vida.

Validação executada com PostgreSQL 15 via Testcontainers:

```text
Successfully validated 3 migrations
Successfully applied 3 migrations, now at version v3
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
