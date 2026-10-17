# SDD — Endpoint para solicitar código de acesso

- Cartão: `[08] Criar endpoint para solicitar código de acesso`
- Status: implementado
- Data: 2026-10-14
- Serviço: `financial-service`
- Dependências: cartões [05] e [07] concluídos
- Rota: `POST /mobile/v1/auth/codes`

## 1. Objetivo

Expor na API mobile o primeiro passo da autenticação por WhatsApp. O endpoint receberá um telefone internacional, criará um desafio seguro usando o componente do cartão [07] e devolverá ao aplicativo somente os metadados necessários para a futura confirmação.

O endpoint deverá:

- aceitar chamadas sem JWT e sem `X-Internal-Api-Key`;
- validar estritamente o contrato HTTP;
- resolver o endereço IP da conexão sem confiar em headers enviados pelo cliente;
- criar exatamente um desafio para cada solicitação aceita;
- devolver `202 Accepted` com `challengeId` e tempos em segundos;
- gerar um identificador de correlação para rastreamento técnico;
- não devolver nem registrar o código, telefone ou hashes;
- não consultar, criar ou revelar a existência de usuário;
- preparar uma fronteira clara para o envio que será implementado no cartão [09].

## 2. Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)
- [SDD do cartão 05 — Cadeias de segurança](card-05-security-chains-sdd.md)
- [SDD do cartão 07 — Geração e armazenamento seguro do código](card-07-verification-code-sdd.md)

## 3. Contexto atual

O cartão [05] já definiu uma cadeia de segurança própria para `/mobile/v1/**` e liberou explicitamente:

```text
POST /mobile/v1/auth/codes
```

Isso significa que a solicitação do código não exige access token, como é necessário para o primeiro acesso, mas permanece isolada das rotas internas protegidas por `X-Internal-Api-Key`.

O cartão [07] implementou:

- normalização e proteção do telefone;
- geração de código de seis dígitos com `SecureRandom`;
- HMAC do código e do endereço IP;
- persistência do desafio;
- validade de cinco minutos;
- intervalo de reenvio de sessenta segundos persistido para aplicação futura;
- invalidação transacional do desafio anterior;
- coordenação concorrente por telefone no PostgreSQL.

Ainda não existe controller, DTO HTTP, tratamento uniforme de erros de validação, resolução explícita do IP ou correlação compartilhada para esse endpoint.

## 4. Escopo

### 4.1 Incluído

- implementar `POST /mobile/v1/auth/codes`;
- criar DTOs específicos de entrada e saída;
- validar telefone conforme o OpenAPI;
- rejeitar JSON malformado, campos ausentes e campos desconhecidos;
- resolver o IP a partir da conexão estabelecida com o servidor;
- chamar `DesafioAutenticacaoService.criar` uma única vez;
- mapear o resultado sensível para uma resposta pública segura;
- retornar `202 Accepted`;
- adicionar `Cache-Control: no-store`;
- gerar e devolver `X-Correlation-Id`;
- padronizar erros mobile desse fluxo com `ErrorResponse`;
- devolver `400` para requisição inválida e `503` para indisponibilidade conhecida da infraestrutura;
- criar testes unitários, MVC e de integração com PostgreSQL.

### 4.2 Fora do escopo

- publicar mensagem no RabbitMQ;
- alterar o `whatsapp-service` ou integrar com WuzAPI;
- entregar efetivamente o código pelo WhatsApp — cartão [09];
- reutilizar o outbox atual para armazenar telefone ou código em texto puro;
- validar o código informado pelo usuário — cartão [10];
- aplicar intervalo mínimo de reenvio e limites por telefone/IP — cartão [11];
- incrementar tentativas ou bloquear um desafio;
- localizar ou criar usuário;
- criar sessão, access token ou refresh token;
- aceitar `X-Forwarded-For`, `Forwarded` ou headers semelhantes sem uma política de proxies confiáveis;
- alterar o formato principal já definido no OpenAPI.

O `429 Too Many Requests` continuará documentado no contrato porque faz parte da API final, mas não será produzido neste cartão. Sua implementação pertence ao cartão [11].

## 5. Contrato HTTP

### 5.1 Requisição

```http
POST /mobile/v1/auth/codes
Content-Type: application/json
```

```json
{
  "telefone": "5531999998888"
}
```

Regras:

| Campo | Regra |
| --- | --- |
| `telefone` | obrigatório |
| formato | somente dígitos |
| primeiro dígito | `1` a `9` |
| comprimento | 10 a 15 dígitos |
| campos adicionais | rejeitados |

Expressão do contrato:

```regex
^[1-9][0-9]{9,14}$
```

O endpoint não aceitará a apresentação formatada `+55 (31) 99999-8888`. A normalização defensiva do domínio permanece, mas a fronteira HTTP exige o formato canônico previsto no OpenAPI.

### 5.2 Resposta aceita

```http
HTTP/1.1 202 Accepted
Content-Type: application/json
Cache-Control: no-store
X-Correlation-Id: b5f670f5-a9b8-4597-ad03-2af7b35fb391
```

```json
{
  "challengeId": "538f1567-a3eb-4e6d-b06d-e5987a340672",
  "expiraEmSegundos": 300,
  "reenviarEmSegundos": 60
}
```

Semântica:

- `challengeId` identifica o desafio que será enviado depois para `/codes/verify`;
- `expiraEmSegundos` informa ao aplicativo a duração nominal do código;
- `reenviarEmSegundos` permite renderizar o contador da interface;
- `202` informa que a solicitação foi aceita para o fluxo assíncrono, não que o WhatsApp confirmou a entrega.

O corpo não conterá telefone mascarado, código, estado do usuário ou indicação de cadastro existente.

### 5.3 Erros

Formato:

```json
{
  "code": "INVALID_REQUEST",
  "message": "Os dados enviados são inválidos",
  "timestamp": "2026-10-14T20:30:00Z",
  "path": "/mobile/v1/auth/codes",
  "correlationId": "b5f670f5-a9b8-4597-ad03-2af7b35fb391"
}
```

Mapeamento deste cartão:

| Situação | HTTP | `code` | Mensagem pública |
| --- | --- | --- | --- |
| JSON inválido, ausente ou fora do contrato | 400 | `INVALID_REQUEST` | `Os dados enviados são inválidos` |
| IP da conexão não pode ser interpretado | 503 | `SERVICE_UNAVAILABLE` | `Não foi possível processar a solicitação agora` |
| banco ou dependência necessária indisponível | 503 | `SERVICE_UNAVAILABLE` | `Não foi possível processar a solicitação agora` |

Erros inesperados não deverão expor stack trace, SQL, nomes de classes, telefone, código, chaves ou hashes. A resposta externa será genérica e o detalhe ficará somente no diagnóstico interno associado ao correlation ID.

## 6. Não enumeração de usuários

O endpoint não consultará `tb_usuario`.

Para qualquer telefone sintaticamente válido, o comportamento público será o mesmo:

```text
telefone associado a usuário existente -> 202
telefone ainda não associado a usuário -> 202
```

Não existirão respostas como:

```text
404 TELEFONE_NAO_ENCONTRADO
409 USUARIO_JA_EXISTE
```

O usuário somente será localizado ou criado após a confirmação correta do código no cartão [10]. Essa ordem impede enumeração e evita criar contas para números nunca comprovados.

O tempo de resposta não precisa ser matematicamente idêntico, mas os dois caminhos devem executar o mesmo fluxo de criação e não possuir uma consulta condicional de usuário que produza diferenças observáveis.

## 7. Identificadores

### 7.1 `challengeId`

Identifica o desafio de autenticação e possui duração funcional além da requisição atual. É devolvido no corpo e será apresentado novamente pelo aplicativo ao confirmar o código.

### 7.2 `correlationId`

Identifica uma única requisição HTTP e serve para rastreamento técnico entre API, logs e, futuramente, o comando de entrega do cartão [09].

Regras:

- gerar um UUID aleatório no início da requisição mobile;
- armazená-lo como atributo da requisição;
- devolvê-lo no header `X-Correlation-Id` em sucesso e erro;
- usar o mesmo valor dentro de `ErrorResponse`;
- disponibilizá-lo temporariamente no MDC somente durante a requisição;
- remover o valor do MDC em bloco `finally` para impedir vazamento entre threads;
- não derivar o correlation ID de telefone, IP ou `challengeId`.

Para o MVP, valores enviados pelo cliente em `X-Correlation-Id` não serão considerados fonte confiável: o servidor sempre gerará seu próprio UUID. Uma política futura poderá aceitar um identificador externo validado e manter também um identificador interno.

O `SecurityErrorResponseWriter` deverá reutilizar o correlation ID criado pelo filtro quando o erro for produzido pelo Spring Security, evitando dois IDs diferentes para a mesma requisição.

O OpenAPI deverá documentar `X-Correlation-Id` pelo menos na resposta `202` e nas respostas de erro desta rota.

## 8. Resolução do endereço IP

Criar um componente HTTP dedicado, conceitualmente:

```java
String resolver(HttpServletRequest request);
```

Neste cartão, a origem será exclusivamente:

```java
request.getRemoteAddr()
```

O valor será validado e convertido para uma representação canônica IPv4 ou IPv6 antes de chegar a `ClientIpProtectionService`.

Não serão lidos diretamente:

- `X-Forwarded-For`;
- `Forwarded`;
- `X-Real-IP`;
- qualquer header arbitrário de IP.

Esses headers podem ser falsificados quando o cliente alcança diretamente a aplicação. Quando houver proxy/reverse proxy em produção, a infraestrutura deverá limitar o acesso direto e declarar proxies confiáveis antes de habilitar processamento de forwarded headers.

Consequência conhecida: enquanto a aplicação estiver atrás de um proxy não configurado, o IP protegido poderá representar o proxy. Isso não altera a criação do desafio, mas precisa ser resolvido antes dos limites por IP do cartão [11].

## 9. Componentes propostos

### 9.1 `MobileAuthController`

Responsável somente pela fronteira HTTP:

```java
@RestController
@RequestMapping("/mobile/v1/auth")
class MobileAuthController {

    @PostMapping("/codes")
    ResponseEntity<SolicitarCodigoResponse> solicitarCodigo(
        @Valid @RequestBody SolicitarCodigoRequest request,
        HttpServletRequest httpRequest
    );
}
```

Responsabilidades:

- receber e validar o JSON;
- solicitar a resolução do IP;
- chamar o serviço de aplicação uma única vez;
- mapear o resultado seguro para `202`;
- aplicar `Cache-Control: no-store`.

O controller não gerará código, não acessará repositories e não tratará existência de usuário.

### 9.2 `SolicitarCodigoRequest`

DTO imutável:

```java
record SolicitarCodigoRequest(
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{9,14}$")
    String telefone
) {}
```

O mapeamento JSON deverá rejeitar propriedades desconhecidas para cumprir `additionalProperties: false`.

### 9.3 `SolicitarCodigoResponse`

DTO público e imutável:

```java
record SolicitarCodigoResponse(
    UUID challengeId,
    long expiraEmSegundos,
    long reenviarEmSegundos
) {}
```

O tipo não possuirá código, telefone, HMAC, IP ou datas internas.

### 9.4 `SolicitacaoCodigoService`

Camada de aplicação entre controller e domínio:

```java
@Transactional
SolicitacaoCodigoCriada solicitar(String telefone, String ipCanonico);
```

Fluxo:

1. chamar `DesafioAutenticacaoService.criar`;
2. manter o código original somente em variável local;
3. mapear para um resultado seguro sem telefone ou código;
4. encerrar a referência sensível ao terminar o método.

O serviço será transacional para que o cartão [09] possa, posteriormente, persistir um comando de entrega na mesma transação do desafio. Neste cartão nenhum comando de envio será persistido.

`SolicitacaoCodigoCriada` será um tipo interno seguro contendo apenas:

- `challengeId`;
- duração nominal até expiração;
- duração nominal até reenvio.

O objeto sensível `DesafioCriado` nunca atravessará o controller.

### 9.5 `ClientIpResolver`

Adaptador da camada web responsável por:

- ler `remoteAddr`;
- validar IPv4 ou IPv6;
- produzir valor canônico;
- lançar exceção técnica sem incluir o valor original quando a infraestrutura fornecer dado inválido.

Ele não calcula HMAC. Essa responsabilidade continua em `ClientIpProtectionService`.

### 9.6 `MobileCorrelationIdFilter`

Filtro executado antes da cadeia de segurança mobile:

- aplica-se a `/mobile/v1/**`;
- cria um UUID por requisição;
- adiciona atributo e header;
- preenche e limpa MDC;
- não registra corpo ou headers sensíveis.

### 9.7 `MobileApiExceptionHandler`

`@RestControllerAdvice` restrito aos controllers mobile, responsável por converter erros da camada MVC e exceções conhecidas para `ErrorResponse`.

Casos mínimos:

- `MethodArgumentNotValidException`;
- `HttpMessageNotReadableException`;
- erro de IP canônico;
- indisponibilidade transitória conhecida da persistência.

O handler deve obter o correlation ID do atributo criado pelo filtro e usar o `Clock` da aplicação para o timestamp.

## 10. Limite com o envio pelo WhatsApp

O cartão [09] criará o comando/evento específico de autenticação e o consumidor no `whatsapp-service`.

O outbox atual armazena seu `payload` como `TEXT`. Ele não poderá ser reutilizado neste cartão para gravar um JSON contendo o código original ou o telefone em texto puro, pois isso anularia a proteção do cartão [07].

Antes da integração, o cartão [09] deverá escolher e documentar uma destas estratégias seguras:

- criptografar o payload sensível do comando antes da persistência no outbox;
- publicar por um canal que não persista o segredo em texto puro, aceitando e tratando sua garantia transacional;
- armazenar uma referência protegida e permitir recuperação controlada do conteúdo apenas pelo produtor autorizado.

Este SDD não antecipa essa decisão. No Card 08, a solicitação cria o desafio e retorna os metadados; a entrega real ainda não estará funcional.

## 11. Transação e fluxo de dados

```text
POST /mobile/v1/auth/codes
        |
        +--> MobileCorrelationIdFilter
        |      └── gera correlationId
        |
        +--> valida SolicitarCodigoRequest
        |
        +--> ClientIpResolver
        |      └── remoteAddr -> IP canônico
        |
        +--> SolicitacaoCodigoService @Transactional
               |
               └--> DesafioAutenticacaoService.criar
                      ├── protege telefone e IP
                      ├── gera e protege código
                      ├── invalida desafio anterior
                      └── persiste novo desafio
        |
        +--> descarta dados sensíveis do resultado
        |
        └--> 202 + challengeId + TTLs + correlationId
```

Se a criação falhar, a transação será revertida e o endpoint devolverá uma resposta genérica. Não poderá haver resposta `202` sem o desafio ter sido confirmado no banco.

## 12. Segurança e observabilidade

É proibido registrar:

- corpo da requisição;
- telefone completo ou criptografado;
- código original;
- HMAC do código;
- IP original ou seu HMAC completo;
- objeto `DesafioCriado`;
- segredos de configuração.

Logs operacionais permitidos:

```text
correlationId=<uuid> event=MOBILE_AUTH_CHALLENGE_ACCEPTED challengeId=<uuid>
```

Em falha:

```text
correlationId=<uuid> event=MOBILE_AUTH_CHALLENGE_FAILED category=PERSISTENCE
```

O log não deverá imprimir a exceção indiscriminadamente se sua mensagem puder conter valores externos ou detalhes de conexão. A exceção completa poderá ser tratada pelo mecanismo seguro de observabilidade da implantação, mas a resposta HTTP sempre será sanitizada.

Não serão adicionadas métricas com telefone ou IP como label, pois isso cria cardinalidade alta e risco de dados pessoais. Métricas agregadas poderão contabilizar solicitações aceitas, inválidas e indisponíveis.

## 13. Alterações previstas

Estrutura conceitual:

```text
financial-service/src/main/java/com/wa/finance/
├── controller/mobile/
│   └── MobileAuthController.java
├── dto/mobile/auth/
│   ├── SolicitarCodigoRequest.java
│   ├── SolicitarCodigoResponse.java
│   └── MobileErrorResponse.java
├── service/
│   ├── SolicitacaoCodigoService.java
│   └── SolicitacaoCodigoCriada.java
└── web/mobile/
    ├── ClientIpResolver.java
    ├── MobileApiExceptionHandler.java
    └── MobileCorrelationIdFilter.java
```

Arquivos existentes que poderão ser ajustados:

- `SecurityErrorResponseWriter`: reutilizar correlation ID da requisição;
- `SecurityConfiguration`: posicionar o filtro de correlação corretamente, sem alterar as permissões já definidas;
- `mobile-api.yml`: documentar `X-Correlation-Id` na resposta da rota;
- configuração Jackson: garantir rejeição de campos desconhecidos sem quebrar contratos internos existentes.

Os nomes finais dos pacotes podem acompanhar o padrão escolhido durante a implementação, preservando as responsabilidades.

## 14. Estratégia de testes

### 14.1 Testes do DTO e validação

- aceita telefone com 10 a 15 dígitos;
- rejeita nulo, vazio e espaços;
- rejeita `+`, parênteses, hífen e letras;
- rejeita menos de 10 ou mais de 15 dígitos;
- rejeita primeiro dígito zero;
- rejeita propriedades JSON desconhecidas;
- rejeita corpo ausente e JSON malformado.

### 14.2 `ClientIpResolverTest`

- resolve IPv4 por `remoteAddr`;
- resolve e canonicaliza IPv6;
- rejeita valor inválido sem incluí-lo na exceção;
- ignora `X-Forwarded-For`, `Forwarded` e `X-Real-IP`;
- não realiza lookup baseado em valor fornecido pelo cliente.

### 14.3 `SolicitacaoCodigoServiceTest`

- chama `DesafioAutenticacaoService.criar` exatamente uma vez;
- passa telefone e IP canônico corretos;
- mapeia `challengeId` e durações;
- não inclui telefone ou código no resultado seguro;
- propaga falha para permitir rollback;
- `toString()` do resultado não contém informação sensível.

### 14.4 `MobileCorrelationIdFilterTest`

- gera UUID em requisição mobile;
- adiciona o mesmo valor ao atributo, MDC e header;
- ignora correlation ID arbitrário enviado pelo cliente;
- remove o MDC ao final, inclusive quando ocorre exceção;
- não altera requisições fora de `/mobile/v1/**`.

### 14.5 Testes MVC do endpoint

- requisição válida sem autenticação retorna `202`;
- resposta contém exatamente os campos públicos esperados;
- resposta contém `Cache-Control: no-store` e `X-Correlation-Id` válido;
- resposta nunca contém telefone ou código;
- requisição inválida retorna `400` no formato `ErrorResponse`;
- correlation ID do header e do erro são iguais;
- falha transitória conhecida retorna `503` genérico;
- endpoint não exige nem utiliza `X-Internal-Api-Key`;
- headers de IP forjados não substituem `remoteAddr`.

### 14.6 Integração PostgreSQL

Com Testcontainers:

- uma requisição válida cria exatamente uma nova linha em `tb_desafio_autenticacao`;
- a linha não contém código, telefone ou IP em texto puro;
- o `challengeId` devolvido corresponde ao registro persistido;
- a resposta possui `300` e `60` com a configuração padrão;
- telefone previamente associado a usuário e telefone ainda não associado recebem o mesmo status e formato;
- nenhum usuário é criado durante a solicitação;
- duas solicitações sequenciais válidas invalidam o desafio anterior e deixam somente um não finalizado;
- falha na persistência não produz `202`.

Os testes de limite de reenvio e volume não pertencem a este cartão e serão adicionados no [11]. O teste de entrega real pelo WhatsApp pertence ao [09].

## 15. Sequência de implementação

1. Criar DTOs de entrada, saída e erro mobile.
2. Criar o filtro de correlation ID e adaptar o writer de erros de segurança.
3. Criar e testar o resolvedor de IP.
4. Criar o resultado interno seguro e `SolicitacaoCodigoService`.
5. Criar `MobileAuthController` com resposta `202` e `no-store`.
6. Criar o advice de erros restrito à API mobile.
7. Garantir rejeição de propriedades desconhecidas sem regressão nas APIs internas.
8. Atualizar a documentação do header no OpenAPI.
9. Criar testes unitários e MVC.
10. Criar os testes de integração com PostgreSQL.
11. Executar a suíte completa do `financial-service`.

## 16. Critérios de aceite

O cartão poderá ser concluído quando:

- `POST /mobile/v1/auth/codes` estiver acessível sem JWT e sem chave interna;
- entrada válida produzir `202` de acordo com o OpenAPI;
- cada solicitação aceita criar exatamente um desafio;
- a resposta expuser somente `challengeId`, `expiraEmSegundos` e `reenviarEmSegundos`;
- o mesmo correlation ID estiver disponível no header e em eventuais erros;
- telefone, código, IP e hashes não aparecerem em resposta ou logs;
- nenhuma consulta ou criação de usuário ocorrer;
- headers de IP controlados pelo cliente forem ignorados;
- entradas inválidas produzirem `400` uniforme;
- indisponibilidade conhecida produzir `503` sem detalhes internos;
- não houver persistência do código ou telefone em outbox textual;
- testes comprovarem comportamento idêntico para telefone associado e não associado;
- testes unitários, MVC, PostgreSQL e a suíte existente passarem sem regressão.

## 17. Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| controller serializar `DesafioCriado` | criar resultado intermediário seguro e DTO público explícito |
| enumeração de usuários | não consultar usuário e manter resposta uniforme |
| confiar em IP forjado | usar apenas `remoteAddr` até existir política de proxies confiáveis |
| perder rastreabilidade | correlation ID único em header, erro e contexto interno |
| vazar dados no MDC ou entre threads | não adicionar telefone/IP e sempre limpar MDC em `finally` |
| responder `202` sem persistir | confirmar criação na transação antes da resposta |
| armazenar código no outbox atual | deixar integração fora do Card 08 e projetá-la de forma segura no Card 09 |
| prometer entrega com `202` | documentar que significa aceitação, não confirmação do WhatsApp |
| campos extras alterarem semântica | rejeitar propriedades desconhecidas |
| quebrar tratamento de erros interno | restringir o advice aos controllers mobile |
| aplicar limite somente em uma instância | implementar limites compartilhados somente no Card 11 |

## 18. Resultado esperado

Ao final do cartão [08], o aplicativo poderá iniciar o fluxo de autenticação:

```text
telefone válido
      |
      v
POST /mobile/v1/auth/codes
      |
      +--> desafio seguro persistido
      +--> nenhum usuário criado
      +--> nenhum segredo exposto
      |
      v
202 Accepted
challengeId + tempos públicos
```

O código ainda não chegará pelo WhatsApp até a conclusão do cartão [09]. A confirmação do código será implementada no cartão [10] e os limites contra abuso no cartão [11].

## 19. Evidências da implementação

O escopo deste SDD foi implementado com:

- `POST /mobile/v1/auth/codes` público dentro da cadeia de segurança mobile;
- DTO de entrada com validação estrita e rejeição de campos desconhecidos;
- resposta `202 Accepted` contendo somente `challengeId` e tempos públicos;
- `Cache-Control: no-store` em respostas de sucesso e erro;
- resolução do IP por `remoteAddr`, ignorando headers de encaminhamento controláveis pelo cliente;
- serviço de aplicação transacional que impede o resultado sensível do cartão [07] de alcançar o controller;
- correlation ID gerado pelo servidor, incluído no header e reutilizado nos erros de segurança e MVC;
- tratamento uniforme de `400` e `503` sem detalhes internos;
- documentação dos headers de resposta no OpenAPI;
- teste de ponta a ponta com PostgreSQL 15 confirmando persistência protegida e ausência de criação de usuário.

Validação final:

```text
Successfully validated 3 migrations
PostgreSQL 15 via Testcontainers
Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Permanecem fora deste cartão: publicação e entrega pelo WhatsApp ([09]), confirmação do código ([10]) e limites contra abuso ([11]).
