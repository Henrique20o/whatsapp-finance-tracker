# SDD — Proteção contra abuso do fluxo de código

- Cartão: `[11] Implementar proteção contra abuso do fluxo de código`
- Estado do documento: implementado
- Serviço: `financial-service`
- Rotas afetadas: `POST /mobile/v1/auth/codes` e `POST /mobile/v1/auth/codes/verify`
- Dependências: Cards [08] e [10]

## 1. Objetivo

Completar as proteções do fluxo público de autenticação por WhatsApp para impedir:

- envio repetitivo de mensagens e geração indevida de custo;
- força bruta do código de seis dígitos;
- abuso distribuído entre vários telefones a partir do mesmo endereço IP;
- diferenças de comportamento que permitam descobrir se um telefone já está cadastrado;
- limites inconsistentes quando existirem várias instâncias do `financial-service`.

O Card 11 deve aplicar limites compartilhados pelo PostgreSQL, responder `429 Too Many Requests` com `Retry-After` quando uma nova solicitação de código ainda não for permitida e manter o limite de cinco confirmações incorretas por desafio.

Nenhum bloqueio será permanente. O usuário legítimo sempre poderá tentar novamente depois do fim da janela indicada pelo servidor.

## 2. Documentos relacionados

- `docs/security/mobile-api-security.md`;
- `docs/security/card-07-verification-code-sdd.md`;
- `docs/security/card-08-request-code-endpoint-sdd.md`;
- `docs/security/card-10-verify-code-endpoint-sdd.md`;
- `docs/openapi/mobile-api.yml`;
- `docs/adr/001-api-mobile-no-financial-service.md`.

Em caso de divergência, este documento mantém as decisões de segurança já registradas em `mobile-api-security.md` e explicita apenas como implementá-las.

## 3. Contexto atual

O projeto já possui:

- telefone normalizado e persistido somente de forma criptografada e por HMAC de busca;
- endereço IP persistido somente como HMAC em `ip_hash`;
- `reenvio_permitido_em` calculado para cada desafio;
- índices por `(telefone_hash, criado_em)` e `(ip_hash, criado_em)`;
- advisory lock transacional por telefone durante a criação de desafios;
- invalidação do desafio anterior quando um novo é criado;
- limite de cinco códigos incorretos em `DesafioAutenticacao`;
- bloqueio pessimista na confirmação para impedir duas confirmações simultâneas;
- `ClientIpResolver`, que usa o endereço remoto efetivo e não confia diretamente em headers enviados pelo cliente;
- contrato OpenAPI com `429` e `Retry-After`.

Entretanto, `reenvio_permitido_em` ainda é apenas persistido. O endpoint cria outro desafio mesmo antes dos sessenta segundos e ainda não consulta os limites horários e diários.

## 4. Política do MVP

| Escopo | Regra | Janela | Resultado quando excedido |
| --- | ---: | ---: | --- |
| telefone | 1 solicitação | 60 segundos | `429` até `reenvio_permitido_em` |
| telefone | 5 solicitações | 1 hora móvel | `429` até a quantidade ficar abaixo de 5 |
| telefone | 10 solicitações | 24 horas móveis | `429` até a quantidade ficar abaixo de 10 |
| IP | 30 solicitações | 1 hora móvel | `429` até a quantidade ficar abaixo de 30 |
| desafio | 5 códigos incorretos | vida do desafio | desafio invalidado e resposta `401` |

As janelas são móveis, calculadas em relação ao `Clock` da aplicação. Não são reiniciadas em horas cheias nem à meia-noite.

### 4.1 Bloqueio progressivo temporário

No MVP, “progressivo” significa aplicar a maior espera entre camadas cada vez mais amplas:

1. intervalo curto de reenvio de sessenta segundos;
2. limite de volume por telefone em uma hora;
3. limite de volume por telefone em vinte e quatro horas;
4. limite compartilhado por IP em uma hora.

Quando mais de uma regra estiver bloqueando a solicitação, `Retry-After` representará o maior tempo restante. Não será criada blacklist permanente nem tabela de punições por reincidência neste cartão. Essa abordagem reduz o risco de bloquear indefinidamente usuários legítimos que compartilham IP ou que tocaram várias vezes no botão.

### 4.2 Limite por instalação

O Card 11 do Trello menciona limite por instalação, mas essa parte não integra o MVP. A decisão vigente em `mobile-api-security.md` registra que um identificador fornecido pelo aplicativo pode ser apagado ou falsificado e não deve ser tratado como identidade confiável.

Consequências:

- não adicionar `installationId` aos contratos HTTP;
- não criar fingerprint do aparelho;
- não persistir identificador publicitário ou de hardware;
- manter um ponto de extensão para uma futura credencial de instalação emitida e assinada pelo servidor, caso o risco operacional justifique.

## 5. Decisões arquiteturais

### 5.1 PostgreSQL como fonte compartilhada

Os limites serão calculados a partir de `tb_desafio_autenticacao`. Cada desafio persistido representa uma solicitação de código aceita.

Não será usado contador somente em memória, porque duas instâncias da aplicação teriam contagens diferentes. Também não será introduzido Redis no MVP: os registros e índices necessários já existem no PostgreSQL e os volumes iniciais não justificam outro componente operacional.

### 5.2 Sem nova tabela no MVP

Não é necessária uma tabela de rate limit porque:

- o histórico de solicitações já existe;
- os índices necessários já foram criados em `V3__mobile_authentication.sql`;
- solicitações rejeitadas não precisam ser contadas como novos desafios;
- o desbloqueio pode ser calculado pelo instante do registro que ocupa a posição limite dentro da janela.

Uma futura tabela de buckets ou Redis poderá substituir as consultas se métricas reais demonstrarem pressão excessiva no banco.

### 5.3 Serialização antes de contar

Uma consulta de contagem isolada não evita corrida. Duas transações poderiam observar quatro solicitações por telefone e ambas criarem a quinta.

Antes de consultar e criar, a aplicação deve adquirir advisory locks transacionais para:

1. o hash do telefone;
2. o hash do IP.

Todos os fluxos devem adquirir os locks sempre nessa ordem. Isso serializa solicitações concorrentes do mesmo telefone ou IP e evita ultrapassar o limite em ambientes com várias instâncias.

Os identificadores de lock devem possuir separação de domínio, por exemplo, derivando valores distintos de `PHONE:<hash>` e `IP:<hash>`. Os hashes não devem aparecer em logs.

### 5.4 A rejeição acontece antes dos efeitos

Quando houver bloqueio, a transação não pode:

- invalidar o desafio anterior;
- gerar novo código;
- persistir novo desafio;
- criar mensagem na outbox;
- publicar mensagem no RabbitMQ.

Assim, tocar em “reenviar” cedo demais não inutiliza o código que o usuário já recebeu.

### 5.5 Falhas transacionais não consomem cota

A criação do desafio e a inclusão na outbox continuam na mesma transação. Se a operação falhar e sofrer rollback, o desafio não permanece no histórico e, portanto, não consome cota.

## 6. Cálculo das janelas

Para cada regra de volume, o repository deverá localizar o registro que ocupa a posição limite em ordem decrescente de criação:

- quinto desafio mais recente do telefone dentro de uma hora;
- décimo desafio mais recente do telefone dentro de vinte e quatro horas;
- trigésimo desafio mais recente do IP dentro de uma hora.

Se esse registro existir dentro da janela, o limite foi atingido. O desbloqueio ocorre em:

```text
instanteDoRegistroLimite + duraçãoDaJanela
```

Usar a posição limite, e não simplesmente o registro mais antigo, também produz resultado correto caso existam dados anteriores à ativação do Card 11 acima dos novos limites.

O tempo público será calculado com arredondamento para cima:

```text
retryAfterSeconds = max(1, ceil(blockedUntil - now))
```

Entre todas as regras violadas, prevalece o maior `blockedUntil`.

## 7. Fluxo de solicitação de código

```text
POST /mobile/v1/auth/codes
        |
        +--> validar JSON e telefone
        +--> resolver IP canônico
        +--> normalizar telefone
        +--> calcular telefone_hash e ip_hash
        +--> adquirir lock PHONE
        +--> adquirir lock IP
        +--> avaliar reenvio, hora/dia do telefone e hora do IP
                 |
                 +--> bloqueado: rollback/retorno 429 + Retry-After
                 |
                 +--> permitido
                        +--> invalidar desafio anterior
                        +--> gerar e persistir novo desafio
                        +--> adicionar comando à outbox
                        +--> commit
                        +--> retornar 202
```

## 8. Fluxo de confirmação

O Card 10 já implementou o limite de cinco tentativas com bloqueio pessimista no próprio desafio. O Card 11 deve preservar estas decisões:

- a tentativa incorreta incrementa `tentativas` na mesma transação;
- a quinta tentativa incorreta preenche `invalidado_em`;
- tentativas posteriores não alteram o desafio;
- código correto após a quinta falha não é aceito;
- concorrência não permite mais de um consumo;
- desafio inválido, expirado, consumido ou com tentativas esgotadas continua retornando `401 INVALID_OR_EXPIRED_CODE`.

Não retornar `429` especificamente por “cinco erros no desafio”. Diferenciar desafio existente e esgotado de um UUID inexistente criaria um oráculo. O `429` deste cartão será aplicado à solicitação de novos códigos; o limite de confirmação continua indistinguível por `401`.

O IP usado na confirmação não precisa ser igual ao IP da solicitação. Redes móveis podem trocar de endereço entre as duas chamadas.

## 9. Contrato HTTP

### 9.1 Solicitação permitida

O contrato atual permanece:

```http
HTTP/1.1 202 Accepted
Cache-Control: no-store
X-Correlation-Id: <uuid>
Content-Type: application/json
```

### 9.2 Solicitação limitada

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Cache-Control: no-store
X-Correlation-Id: <uuid>
Content-Type: application/json
```

```json
{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Aguarde antes de tentar novamente",
  "timestamp": "2026-10-18T10:00:00-03:00",
  "path": "/mobile/v1/auth/codes",
  "correlationId": "b5f670f5-a9b8-4597-ad03-2af7b35fb391"
}
```

Regras do contrato:

- `Retry-After` usa segundos inteiros e nunca é zero;
- o corpo não informa qual limite foi atingido;
- o corpo não informa se o telefone existe;
- a resposta não contém `challengeId`;
- toda resposta usa `Cache-Control: no-store`;
- `X-Correlation-Id` permanece disponível para suporte.

### 9.3 OpenAPI

O componente `TooManyRequests` já existe em `docs/openapi/mobile-api.yml`. A implementação deve ser testada contra esse formato. Não é necessária alteração estrutural no contrato neste cartão, salvo correções encontradas durante o desenvolvimento.

## 10. Configuração

Criar `MobileAuthRateLimitProperties` com prefixo:

```yaml
app.security.mobile.verification-code.rate-limit:
  phone-per-hour: ${MOBILE_AUTH_PHONE_PER_HOUR:5}
  phone-per-day: ${MOBILE_AUTH_PHONE_PER_DAY:10}
  ip-per-hour: ${MOBILE_AUTH_IP_PER_HOUR:30}
```

O intervalo de reenvio continua em:

```yaml
app.security.mobile.verification-code.resend-interval: ${MOBILE_VERIFICATION_RESEND_INTERVAL:60s}
```

Validações de inicialização:

- todos os limites devem ser maiores que zero;
- `phone-per-day` deve ser maior ou igual a `phone-per-hour`;
- o intervalo de reenvio continua positivo e não pode superar o TTL do código;
- a aplicação deve falhar na inicialização diante de configuração inválida.

Os valores podem ser ajustados por ambiente sem recompilar, mas os padrões do MVP permanecem parte da política documentada.

## 11. Componentes previstos

### 11.1 Novos componentes

| Componente | Responsabilidade |
| --- | --- |
| `MobileAuthRateLimitProperties` | configuração tipada e validada dos limites |
| `MobileAuthRateLimitService` | avaliar todas as regras e calcular o desbloqueio |
| `RateLimitDecision` | representar permitido ou bloqueado e `retryAfterSeconds` |
| `RateLimitExceededException` | transportar somente o tempo de espera até o handler |
| `MobileAuthRateLimitMetrics` | registrar rejeições sem dados pessoais nem alta cardinalidade |

### 11.2 Componentes alterados

| Componente | Alteração |
| --- | --- |
| `DesafioAutenticacaoRepository` | consultas do registro-limite por telefone/IP e do último desafio do telefone |
| `DesafioAutenticacaoLockRepository` | lock com separação de domínio para telefone e IP |
| `DesafioAutenticacaoService` | aplicar locks e limite antes de invalidar/criar |
| `SolicitacaoCodigoService` | propagar a rejeição sem criar outbox |
| `MobileApiExceptionHandler` | mapear limite para `429` com `Retry-After` |
| `VerificationCodeConfiguration` | habilitar as novas propriedades, se necessário |
| `application.yml` | declarar os valores configuráveis |

### 11.3 Componentes preservados

| Componente | Motivo |
| --- | --- |
| `ClientIpResolver` | já entrega IP canônico sem confiar em header arbitrário |
| `PhoneProtectionService` | já normaliza e gera HMAC de busca do telefone |
| `ClientIpProtectionService` | já gera HMAC do IP |
| `DesafioAutenticacao` | já contém reenvio e cinco tentativas por desafio |
| `ConfirmacaoCodigoService` | já serializa, contabiliza e invalida tentativas incorretas |
| `OutboxService` | continua sendo chamado somente para solicitação aceita |

## 12. Fronteira transacional e concorrência

O método público que cria a solicitação deve permanecer `@Transactional`. A sequência obrigatória é:

1. normalizar e proteger telefone/IP;
2. adquirir locks transacionais;
3. ler o histórico necessário;
4. decidir o limite usando o mesmo `Instant agora`;
5. se bloqueado, interromper antes de qualquer escrita funcional;
6. se permitido, invalidar o desafio anterior;
7. salvar o novo desafio;
8. adicionar a mensagem à outbox;
9. efetivar tudo em um único commit.

Os advisory locks são liberados automaticamente no commit ou rollback.

### 12.1 Ordem dos locks

Todos os caminhos devem usar a ordem `PHONE` e depois `IP`. Não ordenar dinamicamente conforme o valor do hash, pois caminhos diferentes poderiam formar ciclos de espera.

### 12.2 Índices

Os índices atuais atendem às consultas:

```sql
idx_desafio_auth_telefone_criado (telefone_hash, criado_em DESC)
idx_desafio_auth_ip_criado       (ip_hash, criado_em DESC)
```

Antes de criar nova migration, a implementação deve confirmar com `EXPLAIN` em PostgreSQL que as consultas posicionais utilizam esses índices. Uma migration somente será adicionada se a evidência mostrar necessidade.

## 13. Segurança e privacidade

### 13.1 Dados que nunca podem ser registrados

- telefone completo;
- telefone criptografado;
- `telefone_hash`;
- endereço IP completo;
- `ip_hash`;
- código de verificação ou seu HMAC;
- conteúdo do envelope da outbox.

Embora telefone/IP em HMAC não estejam em texto puro, ainda são identificadores pseudônimos e não devem virar tags de métricas ou campos de log.

### 13.2 Logs permitidos

Um bloqueio pode registrar:

```text
event=MOBILE_AUTH_RATE_LIMITED
endpoint=CODES_REQUEST
scope=PHONE|IP
rule=RESEND|HOURLY|DAILY
retryAfterSeconds=<n>
correlationId=<uuid>
```

Não registrar uma linha separada para cada regra violada se isso produzir ruído; registrar a regra responsável pelo maior tempo de espera é suficiente para operação.

### 13.3 Métricas

Criar contador de baixa cardinalidade, por exemplo:

```text
mobile.auth.rate_limit.rejections
```

Tags permitidas:

- `endpoint=codes_request`;
- `scope=phone|ip`;
- `rule=resend|hourly|daily`.

Não usar telefone, IP, hashes, `challengeId` ou `correlationId` como tags. Métricas detalhadas e alertas completos continuam no Card [25].

## 14. Estratégia de testes

### 14.1 Propriedades

- aceita os padrões `5`, `10` e `30`;
- rejeita zero e valores negativos;
- rejeita limite diário menor que o horário;
- mantém a validação do intervalo de reenvio.

### 14.2 Decisão de limite

- permite a primeira solicitação;
- bloqueia reenvio antes de sessenta segundos;
- permite exatamente no instante `reenvio_permitido_em`;
- bloqueia a sexta solicitação do telefone dentro de uma hora;
- permite quando o quinto registro sai da janela de uma hora;
- bloqueia a décima primeira solicitação do telefone em vinte e quatro horas;
- bloqueia a trigésima primeira solicitação do IP em uma hora;
- escolhe o maior desbloqueio quando várias regras são violadas;
- arredonda `Retry-After` para cima e nunca retorna zero.

### 14.3 Serviço e outbox

- solicitação bloqueada não invalida o desafio vigente;
- solicitação bloqueada não gera código;
- solicitação bloqueada não salva desafio;
- solicitação bloqueada não cria item na outbox;
- solicitação permitida mantém desafio e outbox atômicos;
- falha transacional não deixa solicitação contabilizada.

### 14.4 MVC

- `429` contém `Retry-After` numérico;
- `429` contém `Cache-Control: no-store`;
- `429` contém `X-Correlation-Id`;
- corpo segue `MobileErrorResponse` e usa `RATE_LIMIT_EXCEEDED`;
- resposta não expõe telefone, IP, hashes, regra interna ou `challengeId`;
- `202`, `400` e `503` não sofrem regressão.

### 14.5 PostgreSQL/Testcontainers

- as consultas respeitam janelas com `TIMESTAMPTZ`;
- duas solicitações simultâneas do mesmo telefone resultam em uma aceita durante o intervalo de reenvio;
- solicitações simultâneas de telefones diferentes no mesmo IP não ultrapassam trinta por hora;
- duas instâncias lógicas usando o mesmo banco observam o mesmo limite;
- o desafio existente permanece válido quando o reenvio é rejeitado;
- os índices por telefone e IP são exercitados pelas consultas reais.

### 14.6 Confirmação

Manter e complementar os testes do Card 10:

- quinta tentativa errada invalida o desafio;
- sexta tentativa continua genérica e não altera contadores;
- código correto depois do esgotamento falha;
- duas confirmações concorrentes produzem no máximo um sucesso;
- o erro público permanece `401 INVALID_OR_EXPIRED_CODE`.

## 15. Plano de implementação

1. Criar propriedades tipadas para os limites.
2. Implementar as consultas posicionais no repository.
3. Generalizar o advisory lock para telefone e IP com separação de domínio.
4. Implementar `RateLimitDecision` e `MobileAuthRateLimitService`.
5. Integrar a decisão antes da invalidação e geração do desafio.
6. Criar `RateLimitExceededException`.
7. Mapear a exceção para `429` e adicionar `Retry-After`.
8. Adicionar métricas de baixa cardinalidade.
9. Criar testes unitários, MVC e de integração PostgreSQL.
10. Executar a suíte completa do `financial-service`.
11. Conferir o contrato OpenAPI e atualizar apenas se houver divergência real.

## 16. Critérios de aceite

- um telefone não recebe dois desafios dentro de sessenta segundos;
- um telefone não ultrapassa cinco solicitações em uma hora nem dez em vinte e quatro horas;
- um IP não ultrapassa trinta solicitações em uma hora;
- limites permanecem corretos sob concorrência e com múltiplas instâncias;
- `429` informa um `Retry-After` correto, inteiro e positivo;
- uma rejeição não invalida o código anterior nem cria mensagem na outbox;
- o mesmo comportamento é aplicado a telefones já cadastrados e novos;
- cinco códigos incorretos invalidam o desafio sem revelar seu estado;
- nenhum bloqueio é permanente;
- logs e métricas não contêm telefone, IP, código ou seus hashes;
- não é introduzido identificador de instalação no MVP;
- toda a suíte anterior continua passando.

## 17. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| corrida ultrapassar o limite | advisory locks transacionais antes de consultar e criar |
| deadlock entre locks | ordem única `PHONE` seguida de `IP` |
| usuário atrás de NAT compartilhar limite | limite de IP mais amplo e sempre temporário |
| novo código invalidar o anterior antes de uma rejeição | avaliar limite antes da invalidação |
| enumeração de usuários | mesma política e mesmo corpo para telefone novo ou existente |
| métricas vazarem identificadores | somente tags fixas de baixa cardinalidade |
| consultas crescerem com o histórico | índices existentes, consultas limitadas à posição necessária e futura limpeza do Card [25] |
| relógios diferentes entre instâncias | `Clock` para testes e sincronização de tempo na infraestrutura; timestamps persistidos em UTC |
| proxy mascarar todos os clientes com um IP | configurar endereço remoto confiável na infraestrutura, sem aceitar `X-Forwarded-For` arbitrário |

## 18. Fora do escopo

- fingerprint do aparelho ou limite por instalação;
- CAPTCHA;
- blacklist permanente;
- Redis ou API gateway dedicado para rate limiting;
- bloqueio por país ou operadora;
- detecção comportamental avançada;
- alertas e dashboards completos, pertencentes ao Card [25];
- limpeza histórica dos desafios, também tratada no Card [25];
- emissão de JWT e refresh token, pertencente aos Cards [12] e [13].

## 19. Resultado esperado

Ao final do Card 11, o fluxo de autenticação continuará simples para o usuário legítimo, mas uma automação não conseguirá gerar mensagens ilimitadas nem contornar os limites distribuindo pedidos entre telefones no mesmo IP.

O servidor será a fonte da verdade para o tempo de espera. O aplicativo apenas exibirá o contador baseado em `Retry-After`, sem inventar uma duração fixa e sem tentar reproduzir localmente as regras de segurança.

## 20. Evidências da implementação

Implementado em 18/10/2026:

- propriedades tipadas para `5` solicitações por telefone/hora, `10` por telefone/dia e `30` por IP/hora;
- consultas limitadas e ordenadas sobre os índices existentes de telefone e IP;
- advisory locks com separação de domínio e ordem fixa `PHONE` seguida de `IP`;
- avaliação das janelas antes da invalidação, geração do código e criação da outbox;
- resposta `429 RATE_LIMIT_EXCEEDED` com `Retry-After`, `Cache-Control: no-store` e `X-Correlation-Id`;
- métrica `mobile.auth.rate.limit.rejections` somente com tags de baixa cardinalidade;
- testes de propriedades, cálculo, ordem dos locks, serviço, métrica, MVC e cenários PostgreSQL/Testcontainers.

Validação local:

- suíte sem Testcontainers: `106` testes, nenhuma falha ou erro;
- suíte completa compilou e executou todos os testes não dependentes de Docker;
- o teste PostgreSQL/Testcontainers não pôde ser executado nesta sessão porque o daemon do Docker Desktop não estava ativo;
- os cenários PostgreSQL foram atualizados para provar que duas solicitações concorrentes do mesmo telefone produzem somente uma criação e que um `429` não invalida o desafio nem duplica a outbox.
