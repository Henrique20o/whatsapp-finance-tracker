# SDD — Entrega do código de acesso pelo WhatsApp

- Cartão: `[09] Integrar envio do código com whatsapp-service e WuzAPI`
- Status: implementado
- Data: 2026-10-17
- Serviços: `financial-service` e `whatsapp-service`
- Dependência: cartão [08] concluído
- Rota de origem: `POST /mobile/v1/auth/codes`

## 1. Objetivo

Completar a entrega assíncrona do código criado pelo fluxo mobile. Quando o `financial-service` aceitar uma solicitação, o desafio e um comando de entrega protegido deverão ser persistidos na mesma transação. O `whatsapp-service` consumirá esse comando, recuperará o conteúdo somente em memória e chamará a WuzAPI.

O desenho deverá garantir que:

- uma resposta `202 Accepted` sempre tenha um desafio e um comando de entrega persistidos;
- indisponibilidade momentânea do RabbitMQ ou da WuzAPI não perca imediatamente a entrega;
- telefone e código não sejam gravados em texto puro no outbox nem no RabbitMQ;
- o consumidor descarte mensagens expiradas;
- as retentativas sejam limitadas e falhas terminais cheguem a uma DLQ;
- logs e diagnósticos permitam rastreamento por IDs técnicos, sem revelar segredos;
- o fluxo financeiro existente no WhatsApp continue funcionando sem regressão.

## 2. Documentos relacionados

- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [SDD do cartão 07 — Geração e armazenamento seguro do código](card-07-verification-code-sdd.md)
- [SDD do cartão 08 — Endpoint para solicitar código](card-08-request-code-endpoint-sdd.md)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)

## 3. Contexto atual

O `financial-service` já:

- cria um desafio com código de seis dígitos;
- persiste somente HMAC do código, nunca o código original;
- protege telefone e IP;
- invalida o desafio anterior do mesmo telefone;
- devolve `202`, `challengeId` e tempos públicos;
- possui `tb_outbox` e um processador periódico para confirmações financeiras.

O `whatsapp-service` já:

- consome `financeiro.v1.whatsapp-saida`;
- utiliza `WuzApiClient.enviarMensagem(telefone, mensagem)`;
- possui retentativa AMQP e DLQ para mensagens financeiras.

Entretanto, o Card 08 não utiliza o telefone e o código retornados internamente por `DesafioAutenticacaoService`. Portanto, hoje o desafio é criado, mas o usuário não recebe o código.

O outbox atual possui `payload TEXT`. Gravar nele um JSON com telefone e código anularia as proteções do Card 07. Também não basta publicar diretamente no RabbitMQ dentro da requisição: uma confirmação do banco seguida de falha de publicação deixaria um desafio válido que nunca seria entregue.

## 4. Decisão arquitetural

Será utilizado o padrão **transactional outbox com payload sensível criptografado**.

```text
financial-service                              whatsapp-service

cria desafio + cifra conteúdo                  recebe envelope cifrado
       |                                               |
       v                                               v
desafio + outbox na mesma transação ---> Rabbit ---> valida prazo e decifra
                                                        |
                                                        v
                                                     WuzAPI
```

A decisão preserva atomicidade no banco e permite retentar a publicação. O telefone e o código ficam legíveis somente:

1. durante a criação do desafio no `financial-service`;
2. durante a decifragem e montagem da mensagem no `whatsapp-service`;
3. no corpo HTTPS enviado à WuzAPI, que precisa conhecer o destino e o texto.

Não será reutilizada nenhuma destas chaves para a entrega:

- `MOBILE_AUTH_HMAC_KEY`;
- `PHONE_ENCRYPTION_KEY`;
- `FINANCIAL_SERVICE_API_KEY`;
- chaves JWT;
- token da WuzAPI.

Uma chave simétrica exclusiva, `MOBILE_AUTH_DELIVERY_KEY`, será compartilhada apenas entre `financial-service` e `whatsapp-service`.

## 5. Escopo

### 5.1 Incluído

- criar um contrato versionado de comando de entrega;
- cifrar telefone e código com AES-256-GCM antes do outbox;
- persistir desafio e comando na mesma transação;
- adicionar exchange, fila, routing key e DLQ exclusivas para autenticação;
- publicar o envelope com confirmação do broker;
- limitar retentativas no outbox;
- consumir o envelope no `whatsapp-service`;
- rejeitar envelope inválido e descartar entrega expirada;
- decifrar o conteúdo somente imediatamente antes do envio;
- usar um template fixo, sem dados financeiros;
- classificar falhas transitórias e permanentes da WuzAPI;
- propagar `messageId`, `challengeId` e `correlationId` para rastreamento;
- adicionar configuração, documentação e testes nos dois serviços.

### 5.2 Fora do escopo

- confirmar o código informado pelo usuário — Card [10];
- criar usuário, sessão, access token ou refresh token;
- aplicar limites de solicitação por IP ou telefone — Card [11];
- informar ao aplicativo se o WhatsApp foi efetivamente entregue ou lido;
- expor endpoint para consultar o estado da entrega;
- armazenar telefone ou código em texto puro para facilitar suporte;
- garantir entrega exatamente uma vez;
- adicionar banco de dados ou Redis ao `whatsapp-service` somente para deduplicação;
- alterar os fluxos financeiros existentes no WhatsApp;
- enviar código por SMS, e-mail ou outro canal.

## 6. Garantia de entrega

O fluxo terá semântica **pelo menos uma vez** entre outbox, RabbitMQ e WuzAPI.

Isso significa:

- falhas transitórias serão retentadas;
- uma mensagem terminal ficará disponível na DLQ para diagnóstico;
- existe uma pequena possibilidade de envio duplicado se o processo cair depois de a WuzAPI aceitar a mensagem e antes do ACK no RabbitMQ.

A WuzAPI usada atualmente não recebe, no contrato local, uma chave de idempotência. Portanto, este SDD não promete entrega exatamente uma vez. Uma eventual duplicação levará o mesmo código e o mesmo prazo; não criará um segundo desafio. O `messageId` será preservado para permitir deduplicação durável futura caso o provedor passe a suportá-la ou o `whatsapp-service` ganhe um inbox persistente.

## 7. Contrato da mensagem

### 7.1 Envelope público

O RabbitMQ e o outbox armazenarão somente o envelope:

```json
{
  "schemaVersion": 1,
  "messageId": "ff468b6d-6d8c-4f15-9677-6d08beb8e221",
  "challengeId": "538f1567-a3eb-4e6d-b06d-e5987a340672",
  "correlationId": "b5f670f5-a9b8-4597-ad03-2af7b35fb391",
  "expiresAt": "2026-10-17T18:35:00Z",
  "keyId": "v1",
  "iv": "base64-do-iv",
  "ciphertext": "base64-do-conteudo-cifrado-e-tag"
}
```

Regras:

- `schemaVersion`: versão do formato do envelope;
- `messageId`: UUID único por comando de entrega;
- `challengeId`: liga a entrega ao desafio, sem revelar o código;
- `correlationId`: ID criado na requisição HTTP do Card 08;
- `expiresAt`: o mesmo instante de expiração do desafio;
- `keyId`: inicialmente `v1`, preparando rotação futura;
- `iv`: 12 bytes aleatórios, em Base64, exclusivos por cifragem;
- `ciphertext`: telefone e código cifrados, incluindo a tag GCM de 128 bits.

O envelope não terá `toString()` gerado automaticamente. Sua representação segura poderá conter apenas versão e IDs técnicos.

### 7.2 Conteúdo sensível antes da cifragem

```json
{
  "telefone": "5531999998888",
  "codigo": "482913"
}
```

Esse conteúdo:

- nunca será salvo ou publicado sem cifragem;
- não será escrito em logs, exceções, métricas ou tracing;
- não fará parte de DTO HTTP;
- existirá em memória pelo menor período prático.

Java e Jackson usam `String`, que não pode ser apagada de memória de maneira confiável. A proteção adotada é reduzir escopo e referências, não prometer zeragem impossível do heap.

### 7.3 Dados autenticados adicionais

AES-GCM também autenticará os metadados públicos. A AAD será a codificação UTF-8 desta sequência canônica:

```text
mobile-auth-code|1|<messageId>|<challengeId>|<correlationId>|<expiresAt>|<keyId>
```

`expiresAt` usará `Instant.toString()`. Os dois serviços deverão reutilizar uma implementação ou testes vetoriais equivalentes para produzir exatamente os mesmos bytes. Alterar qualquer metadado fará a decifragem falhar.

## 8. Criptografia e chave

Algoritmo:

```text
AES/GCM/NoPadding
chave: 256 bits
IV: 96 bits aleatórios por mensagem
tag: 128 bits
codificação externa: Base64
```

Configuração:

```yaml
app:
  security:
    mobile:
      auth-delivery:
        key: ${MOBILE_AUTH_DELIVERY_KEY}
        key-id: ${MOBILE_AUTH_DELIVERY_KEY_ID:v1}
```

Regras operacionais:

- `MOBILE_AUTH_DELIVERY_KEY` será Base64 de exatamente 32 bytes aleatórios;
- não haverá valor padrão, fallback ou chave de desenvolvimento no código;
- os dois serviços deverão falhar na inicialização se a chave for ausente, inválida ou tiver tamanho incorreto;
- `.env.example` conterá somente um placeholder;
- `.env`, logs e arquivos de configuração versionados não deverão conter a chave real;
- `docker-compose.yml` passará a mesma variável aos dois serviços;
- `keyId` não é secreto e identifica qual chave decifra o envelope.

No MVP haverá apenas uma chave ativa. Como os códigos vivem cinco minutos, a troca coordenada da chave pode aguardar o esvaziamento da fila por esse período. Suporte simultâneo a chave atual e anterior poderá ser adicionado depois, sem alterar a versão do envelope.

## 9. Topologia RabbitMQ

Será criada uma topologia exclusiva, sem misturar códigos de autenticação com respostas financeiras:

| Recurso | Nome |
| --- | --- |
| exchange | `financeiro.v1.whatsapp-auth.exchange` |
| routing key | `financeiro.v1.whatsapp-auth.send` |
| fila | `financeiro.v1.whatsapp-auth` |
| DLX | `financeiro.v1.dlx` |
| routing key de falha | `financeiro.v1.whatsapp-auth.failed` |
| DLQ | `financeiro.v1.whatsapp-auth.dlq` |

Fila, exchange e DLQ serão duráveis. A fila terá dead-letter exchange configurada e mensagens serão persistentes.

O produtor definirá expiração por mensagem a partir do tempo restante do desafio. O consumidor também validará `expiresAt`; a validação dupla evita envio de código vencido mesmo se a configuração do broker for alterada.

Os nomes deverão ser iguais nos dois serviços e cobertos por testes de contrato. No MVP eles podem ser declarados em cada serviço, pois não há módulo compartilhado, mas não poderão divergir silenciosamente.

## 10. Fluxo no `financial-service`

### 10.1 Correlação explícita

`MobileAuthController` obterá o ID já criado por `MobileCorrelationIdFilter` e o passará explicitamente:

```java
solicitacaoCodigoService.solicitar(telefone, ipCanonico, correlationId);
```

O serviço de aplicação não deverá buscar o valor diretamente no MDC. O MDC é mecanismo de log da thread HTTP, enquanto o correlation ID é dado do caso de uso e precisa ser persistido no comando.

### 10.2 Transação atômica

`SolicitacaoCodigoService.solicitar` continuará `@Transactional` e executará:

1. criar o desafio por `DesafioAutenticacaoService`;
2. criar `messageId` aleatório;
3. montar o conteúdo sensível com telefone canônico e código;
4. cifrar o conteúdo com metadados autenticados;
5. serializar somente o envelope cifrado;
6. adicionar `CODIGO_AUTENTICACAO_WHATSAPP` ao outbox;
7. devolver somente o resultado público do Card 08.

Se cifragem, serialização ou persistência do outbox falhar, toda a transação será revertida. O endpoint retornará o erro genérico já previsto pelo Card 08. Não poderá existir um novo desafio confirmado sem seu comando de entrega.

Se o RabbitMQ estiver indisponível depois do commit, a requisição ainda retornará `202`: o outbox persistido tentará publicar posteriormente.

### 10.3 Novo tipo no outbox

Adicionar:

```text
CODIGO_AUTENTICACAO_WHATSAPP
```

O payload dessa categoria será sempre o envelope cifrado da seção 7. O evento existente `CONFIRMACAO_WHATSAPP` continuará com seu contrato atual.

O outbox não deve aceitar um objeto sensível genérico. A API deverá exigir um `ComandoEntregaCodigoCifrado`, reduzindo a chance de alguém salvar acidentalmente o DTO antes da cifragem.

### 10.4 Publicação confiável

O publisher deverá utilizar:

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

A linha só será marcada como `ENVIADO` quando:

- o broker confirmar a publicação;
- a mensagem não for devolvida por ausência de rota;
- a espera do confirm não exceder o timeout configurado.

Um retorno, NACK ou timeout será considerado falha transitória do outbox.

### 10.5 Estados e retentativas do outbox

Ampliar `OutboxStatus` para:

```text
PENDENTE
ENVIADO
FALHA
EXPIRADO
```

Política para o comando de autenticação:

- máximo de 5 tentativas de publicação;
- backoff de 2, 4, 8 e 16 segundos, limitado pelo prazo do código;
- antes de cada tentativa, verificar `expiresAt` e o estado do desafio;
- desafio ausente, expirado, consumido ou invalidado leva a `EXPIRADO` sem publicação;
- depois da quinta falha, marcar `FALHA` e interromper retentativas automáticas.

O processador deverá armazenar em `ultimo_erro` apenas uma categoria sanitizada, por exemplo:

```text
BROKER_NACK
BROKER_RETURNED
BROKER_TIMEOUT
INVALID_ENVELOPE
```

Não deverá copiar `exception.getMessage()` indiscriminadamente, pois mensagens de bibliotecas podem incorporar conteúdo externo.

### 10.6 Concorrência do outbox

O repository atual apenas seleciona os pendentes; duas instâncias podem selecionar a mesma linha. A implementação deverá buscar uma mensagem por vez com bloqueio pessimista e `SKIP LOCKED`, processando cada item em uma transação curta e independente.

Isso impede publicação simultânea por duas instâncias. Ainda permanece a janela inevitável entre confirmação do broker e commit do status, razão pela qual a garantia continua sendo pelo menos uma vez.

O comportamento do evento financeiro existente deverá ser preservado e coberto por regressão.

## 11. Fluxo no `whatsapp-service`

### 11.1 Consumidor dedicado

Criar um listener exclusivo, conceitualmente:

```java
@RabbitListener(
    queues = RabbitMQConfig.MOBILE_AUTH_QUEUE,
    containerFactory = "mobileAuthListenerContainerFactory"
)
void entregar(CodigoAutenticacaoEnvelope envelope);
```

Fluxo:

1. validar campos, UUIDs, versão e `keyId`;
2. verificar se `expiresAt` ainda está no futuro;
3. reconstruir a AAD canônica;
4. decifrar e autenticar o conteúdo;
5. validar telefone e código decifrados;
6. montar o template fixo;
7. verificar novamente a expiração imediatamente antes da chamada externa;
8. chamar `WuzApiClient.enviarMensagem`;
9. retornar normalmente para ACK.

Uma mensagem já expirada será reconhecida e descartada, com métrica e log seguro. Ela não será retentada nem enviada à WuzAPI, pois não existe ação útil.

Falha de autenticação GCM, formato inválido, versão desconhecida ou chave desconhecida será falha permanente e seguirá para a DLQ.

### 11.2 Template

O texto será montado no consumidor, e não transportado no evento:

```text
Seu código de acesso ao Finance Tracker é: 482913

Ele expira em 5 minutos. Não compartilhe este código.

Se você não solicitou este acesso, ignore esta mensagem.
```

O template:

- contém apenas o código e instruções de segurança;
- não contém saldo, categoria, transação ou qualquer dado financeiro;
- não pergunta nem solicita resposta pelo WhatsApp;
- não inclui telefone;
- informa que, se o usuário não solicitou, pode ignorar a mensagem.

O tempo exibido será derivado da configuração nominal de cinco minutos. O consumidor não deverá arredondar o tempo restante para criar uma promessa diferente do `expiresAt` real.

### 11.3 Classificação das falhas da WuzAPI

| Falha | Tratamento |
| --- | --- |
| timeout, conexão recusada, I/O | transitória |
| HTTP 408 | transitória |
| HTTP 429 | transitória |
| HTTP 5xx | transitória |
| HTTP 4xx diferente de 408/429 | permanente |
| envelope ou criptografia inválidos | permanente |
| mensagem expirada | ACK e descarte |

O listener usará exceções próprias e sanitizadas para que headers da DLQ não recebam telefone, código, corpo da WuzAPI ou corpo da mensagem.

### 11.4 Retentativa do consumidor

O container exclusivo fará no máximo 3 tentativas totais:

```text
tentativa inicial
1ª retentativa após 1 segundo
2ª retentativa após 2 segundos
```

Antes de toda retentativa será verificado se ainda há prazo útil. Ao esgotar as tentativas, `RepublishMessageRecoverer` enviará o envelope cifrado para `financeiro.v1.whatsapp-auth.dlq`.

Falhas permanentes não deverão consumir todas as retentativas; serão encaminhadas diretamente para a DLQ. O listener financeiro atual continuará usando sua configuração existente.

## 12. Segurança de logs e observabilidade

É proibido registrar:

- telefone completo, mascarado ou cifrado;
- código;
- conteúdo decifrado;
- corpo enviado à WuzAPI;
- payload completo do RabbitMQ ou do outbox;
- IV e ciphertext;
- chaves ou tokens;
- resposta bruta da WuzAPI.

Logs permitidos:

```text
event=MOBILE_AUTH_DELIVERY_PUBLISHED messageId=<uuid> challengeId=<uuid> correlationId=<uuid>
event=MOBILE_AUTH_DELIVERY_SENT messageId=<uuid> challengeId=<uuid> correlationId=<uuid>
event=MOBILE_AUTH_DELIVERY_RETRY messageId=<uuid> attempt=2 category=WUZAPI_5XX
event=MOBILE_AUTH_DELIVERY_DLQ messageId=<uuid> category=WUZAPI_TIMEOUT
event=MOBILE_AUTH_DELIVERY_EXPIRED messageId=<uuid>
```

Métricas agregadas recomendadas:

- comandos persistidos;
- publicações confirmadas;
- falhas e expirações do outbox;
- envios aceitos pela WuzAPI;
- retentativas;
- descartes expirados;
- mensagens encaminhadas à DLQ.

Labels poderão conter `service`, `outcome` e categoria de erro de conjunto limitado. Telefone, IP, `challengeId`, `messageId` e `correlationId` não serão labels de métricas.

`correlationId` será usado em logs, mas não enviado ao aplicativo além do header já existente. A fila não aceitará um correlation ID fornecido livremente pelo cliente; o Card 08 gera esse valor no servidor.

## 13. Componentes previstos

### 13.1 `financial-service`

Novos componentes conceituais:

```text
security/mobile/delivery/
├── MobileAuthDeliveryProperties.java
├── MobileAuthDeliveryCipher.java
├── MobileAuthDeliveryEnvelope.java
└── MobileAuthDeliverySecret.java

producer/
└── MobileAuthDeliveryProducer.java
```

Alterações principais:

- `MobileAuthController`: passar correlation ID;
- `SolicitacaoCodigoService`: cifrar e adicionar comando ao outbox na transação;
- `OutboxService`: aceitar o novo tipo seguro;
- `OutboxProcessor`: rotear por tipo, limitar tentativas e sanitizar erros;
- `OutboxStatus`: incluir `FALHA` e `EXPIRADO`;
- `OutboxRepository`: seleção concorrente segura;
- `RabbitMQConfig`: declarar exchange, fila, DLQ e binding;
- `application.yml`: chave e publisher confirms;
- `docker-compose.yml` e `.env.example`: nova variável sem segredo real.

Os nomes finais podem acompanhar o padrão do projeto, mantendo essas responsabilidades separadas.

### 13.2 `whatsapp-service`

Novos componentes conceituais:

```text
security/mobile/
├── MobileAuthDeliveryProperties.java
├── MobileAuthDeliveryCipher.java
└── MobileAuthDeliveryEnvelope.java

listener/
└── MobileAuthCodeListener.java

service/
├── MobileAuthMessageFactory.java
└── MobileAuthDeliveryService.java
```

Alterações principais:

- `RabbitMQConfig`: topologia e container de retry dedicados;
- `WuzApiClient`: classificação segura das respostas HTTP;
- `application.yaml`: chave de entrega;
- `docker-compose.yml` e `.env.example`: injeção da variável.

O listener existente `WhatsAppResponseListener` não será reutilizado, porque seu DTO contém telefone e mensagem em texto e pertence a outro caso de uso.

## 14. Sequência completa

```text
App mobile
   |
   | POST /mobile/v1/auth/codes
   v
MobileAuthController
   | correlationId explícito
   v
SolicitacaoCodigoService @Transactional
   |
   +--> DesafioAutenticacaoService.criar
   |       └── persiste HMAC do código e telefone protegido
   |
   +--> MobileAuthDeliveryCipher
   |       └── telefone + código -> AES-256-GCM
   |
   └--> OutboxService
           └── persiste somente envelope cifrado
   |
COMMIT
   |
   +--> 202 para o app
   |
OutboxProcessor
   └── publish + broker confirm
           |
           v
financeiro.v1.whatsapp-auth
           |
           v
MobileAuthCodeListener
   ├── valida prazo e envelope
   ├── decifra conteúdo
   ├── cria template fixo
   └── WuzApiClient
           |
           v
         WuzAPI
           |
           v
       WhatsApp do usuário
```

## 15. Comportamento em falhas

| Ponto de falha | Resultado |
| --- | --- |
| criação do desafio | rollback; endpoint não retorna `202` |
| cifragem | rollback do desafio e outbox; erro genérico |
| persistência do outbox | rollback do desafio; erro genérico |
| RabbitMQ indisponível após commit | `202`; outbox retenta até limite/prazo |
| exchange sem rota | mensagem não marcada como enviada; outbox retenta |
| desafio invalidado antes da publicação | outbox marcado `EXPIRADO` |
| mensagem vence na fila | broker remove ou consumidor descarta |
| chave divergente | consumidor envia envelope cifrado à DLQ |
| WuzAPI temporariamente indisponível | consumidor retenta e depois envia à DLQ |
| WuzAPI rejeita permanentemente | DLQ sem retentativas inúteis |
| queda após WuzAPI aceitar e antes do ACK | possível reenvio do mesmo código |

A resposta `202` continuará significando “solicitação aceita para processamento”, e não “mensagem entregue”.

## 16. Estratégia de testes

### 16.1 Criptografia nos dois serviços

- chave Base64 de 32 bytes é aceita;
- chave ausente, Base64 inválido ou tamanho incorreto impede inicialização;
- cada cifragem usa IV diferente;
- round trip recupera telefone e código;
- alteração de ciphertext, IV ou AAD falha;
- teste vetorial confirma interoperabilidade entre as implementações;
- `toString()` e mensagens de erro não contêm dados sensíveis.

### 16.2 `SolicitacaoCodigoServiceTest`

- cria exatamente um desafio e um comando;
- usa o mesmo `challengeId`, prazo e correlation ID no envelope;
- persiste somente o envelope cifrado;
- falha do outbox propaga e permite rollback;
- resultado público do Card 08 não muda;
- telefone e código não aparecem no payload serializado.

### 16.3 Integração PostgreSQL

- uma requisição aceita grava desafio e outbox na mesma transação;
- falha forçada do outbox não deixa desafio órfão;
- `tb_outbox.payload` não contém telefone, código nem template;
- novo pedido invalida o desafio anterior;
- comando de desafio invalidado antes de publicar é marcado `EXPIRADO`.

### 16.4 Publisher e outbox

- ACK do broker marca `ENVIADO`;
- NACK, retorno ou timeout mantém retentativa;
- quinta falha marca `FALHA`;
- prazo vencido marca `EXPIRADO`;
- erro persistido é categoria sanitizada;
- duas instâncias não processam a mesma linha simultaneamente;
- publicação inclui expiração e conteúdo persistente;
- evento financeiro existente continua sendo publicado.

### 16.5 Listener do WhatsApp

- envelope válido chama WuzAPI uma vez com telefone e template corretos;
- mensagem expirada não chama WuzAPI e recebe ACK;
- código e telefone fora do formato são falha permanente;
- tag GCM inválida vai para DLQ sem revelar conteúdo;
- HTTP 408, 429 e 5xx são retentados;
- outros 4xx seguem para DLQ sem retentativas inúteis;
- após o limite de falha transitória, mensagem chega à DLQ;
- sucesso não registra telefone, código ou texto;
- listener financeiro existente permanece funcional.

### 16.6 Integração RabbitMQ

Com Testcontainers:

- topologia é criada com nomes e bindings corretos;
- `financial-service` publica e `whatsapp-service` consome o envelope;
- mensagem não roteável não é marcada como enviada;
- falha terminal produz uma mensagem na DLQ;
- mensagem com TTL vencido não chega à WuzAPI;
- payload observado no broker está cifrado.

A WuzAPI deverá ser simulada por servidor HTTP local nos testes. Nenhum teste automatizado enviará mensagem real para um telefone.

## 17. Sequência de implementação

1. Criar propriedades e validação fail-fast da chave nos dois serviços.
2. Implementar o envelope versionado, a AAD e AES-GCM com testes vetoriais.
3. Declarar a topologia RabbitMQ dedicada nos dois serviços.
4. Implementar o producer com mensagens persistentes, TTL e confirms.
5. Ampliar o outbox para o novo tipo, estados terminais e concorrência segura.
6. Passar correlation ID pelo controller e persistir comando na transação do desafio.
7. Implementar factory do template e consumidor no `whatsapp-service`.
8. Classificar erros da WuzAPI e configurar retry/DLQ exclusivos.
9. Adicionar variáveis a `application.yml`, `application.yaml`, `.env.example` e `docker-compose.yml`.
10. Executar testes unitários, MVC, PostgreSQL e RabbitMQ.
11. Fazer teste manual com uma instância de desenvolvimento da WuzAPI, sem registrar o código.
12. Verificar a DLQ e os logs usando apenas IDs técnicos.

## 18. Critérios de aceite

O Card 09 poderá ser concluído quando:

- `POST /mobile/v1/auth/codes` persistir desafio e comando de forma atômica;
- cada comando tiver `messageId`, `challengeId`, `correlationId` e prazo coerentes;
- telefone e código estiverem cifrados no outbox e no RabbitMQ;
- a chave de entrega for exclusiva, obrigatória e validada no startup;
- a publicação só for marcada como enviada após confirmação do broker;
- retentativas do outbox forem limitadas pelo número de tentativas e validade do código;
- o `whatsapp-service` decifrar e enviar o template correto pela WuzAPI;
- mensagens vencidas nunca forem enviadas;
- falhas transitórias forem retentadas de forma limitada;
- falhas terminais chegarem à DLQ e forem rastreáveis por IDs técnicos;
- nenhum log, erro, métrica ou header de DLQ expuser telefone, código ou segredo;
- o fluxo financeiro atual permanecer funcional;
- testes automatizados dos dois serviços passarem sem regressão;
- um teste manual controlado comprovar que o código chega ao número solicitado.

## 19. Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| código ou telefone em texto no outbox | cifrar antes de serializar e testar o conteúdo bruto da tabela |
| chave reutilizada ou vazada | chave exclusiva, variável obrigatória e nenhuma impressão em log |
| metadados do envelope adulterados | autenticar metadados com AAD do AES-GCM |
| desafio salvo sem comando | mesma transação PostgreSQL |
| comando perdido com Rabbit fora | outbox e retentativas após o `202` |
| marcar enviado sem roteamento real | publisher confirms, returns e `mandatory=true` |
| duas instâncias publicarem juntas | bloqueio pessimista com `SKIP LOCKED` |
| retentativa após expiração | TTL no broker e validação antes de publicar/enviar |
| retry infinito | limites no outbox e no consumidor, com estados terminais/DLQ |
| erro da biblioteca expor conteúdo | categorias e exceções sanitizadas |
| duplicação após queda entre WuzAPI e ACK | assumir at-least-once; mesmo desafio/código; preparar `messageId` para inbox futuro |
| rotação quebrar mensagens pendentes | aguardar janela de cinco minutos ou manter chave anterior em evolução futura |
| envio tardio de desafio já substituído | verificar estado antes de publicar; mensagem recebida depois pode ser rejeitada no Card 10 |
| configuração de retry afetar mensagens financeiras | container e fila exclusivos para autenticação |

## 20. Resultado esperado

Ao final do Card 09, o primeiro passo da autenticação estará funcional de ponta a ponta:

```text
telefone válido
      |
      v
POST /mobile/v1/auth/codes
      |
      +--> desafio protegido
      +--> comando cifrado no outbox
      |
      v
202 Accepted
      |
      v
RabbitMQ -> whatsapp-service -> WuzAPI -> WhatsApp
```

O aplicativo ainda não poderá trocar o código por tokens até o Card [10]. O Card [11] complementará o fluxo com limites contra abuso e aplicação efetiva do intervalo de reenvio.

## 21. Evidências da implementação

O escopo foi implementado com:

- envelope versionado protegido por AES-256-GCM e AAD;
- chave exclusiva e obrigatória nos dois serviços;
- persistência transacional do desafio e do comando cifrado;
- publisher confirms, retorno obrigatório, TTL e mensagens persistentes;
- seleção concorrente do outbox com `FOR UPDATE SKIP LOCKED`;
- estados `ENVIADO`, `FALHA` e `EXPIRADO` e limite de cinco tentativas;
- exchange, fila e DLQ exclusivas para autenticação;
- consumidor com validação de prazo, formato, versão e autenticidade;
- classificação de falhas transitórias e permanentes da WuzAPI;
- exceções sanitizadas antes de retry/DLQ;
- template de autenticação sem dados financeiros;
- configuração do Docker Compose e exemplo de ambiente atualizados.

Validação realizada:

```text
financial-service: 92 testes, 0 falhas, 0 erros
whatsapp-service:   70 testes, 0 falhas, 0 erros
PostgreSQL 15 via Testcontainers
3 migrations validadas pelo Flyway
```

O teste PostgreSQL confirma que o endpoint grava o desafio e o outbox e que o payload bruto não contém o telefone nem os campos `telefone` e `codigo`. Testes criptográficos confirmam round trip, IV novo por mensagem e rejeição de metadados adulterados.
