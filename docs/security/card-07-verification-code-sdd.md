# SDD — Geração e armazenamento seguro do código de verificação

- Cartão: `[07] Implementar geração e armazenamento seguro do código`
- Status: implementado
- Data: 2026-10-14
- Serviço: `financial-service`
- Dependência: cartão [06] concluído

## 1. Objetivo

Implementar o núcleo de domínio responsável por criar desafios de autenticação mobile seguros:

- normalizar e proteger o telefone;
- gerar um código imprevisível de seis dígitos;
- persistir somente o HMAC do código;
- proteger o endereço IP usado futuramente nos limites contra abuso;
- aplicar validade de cinco minutos e intervalo de reenvio de sessenta segundos;
- invalidar o desafio anterior quando um novo for criado;
- garantir no máximo um desafio não finalizado por telefone, inclusive com múltiplas instâncias da aplicação;
- impedir exposição do código e do telefone completo em logs ou representações textuais.

O cartão cria a camada reutilizável pelos futuros endpoints de solicitação e confirmação. Ele não expõe uma rota HTTP e não envia mensagens pelo WhatsApp.

## 2. Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)
- [SDD do cartão 06 — Persistência de desafios e sessões](card-06-auth-persistence-sdd.md)
- [Migration V3 de autenticação mobile](../../financial-service/src/main/resources/db/migration/V3__mobile_authentication.sql)

## 3. Contexto atual

O cartão [06] criou `tb_desafio_autenticacao` com:

- UUID do desafio;
- telefone criptografado e hash de busca;
- HMAC do código;
- hash do endereço IP;
- tentativas;
- instantes de criação, expiração e reenvio;
- instantes de consumo e invalidação;
- índice único parcial que permite apenas um desafio não finalizado por telefone.

O `PhoneProtectionService` já oferece:

- normalização do telefone para dígitos;
- criptografia AES-GCM;
- HMAC de busca do telefone;
- máscara para uso seguro em mensagens operacionais.

Ainda não existem entidade JPA, repository, gerador de código, cálculo do HMAC do código ou serviço transacional de criação do desafio.

## 4. Escopo

### 4.1 Incluído

- mapear `tb_desafio_autenticacao` como entidade JPA;
- criar o repository do desafio;
- criar configuração validada para segredo, validade e reenvio;
- gerar códigos de seis dígitos com `SecureRandom`;
- calcular HMAC-SHA-256 do código com chave própria;
- comparar HMACs em tempo constante;
- calcular HMAC do endereço IP com subchave separada;
- criar e persistir desafios;
- invalidar o desafio anterior na mesma transação;
- serializar solicitações concorrentes do mesmo telefone no PostgreSQL;
- implementar expiração lógica;
- criar testes unitários e de integração com PostgreSQL.

### 4.2 Fora do escopo

- criar `POST /mobile/v1/auth/codes`;
- ler headers HTTP ou decidir quais proxies são confiáveis;
- aplicar os limites de cinco solicitações por hora, dez por dia e trinta por IP/hora;
- publicar ou enviar o código pelo WhatsApp;
- confirmar o código e contabilizar tentativas incorretas;
- criar usuário ou sessão mobile;
- emitir JWT ou refresh token;
- criar job de exclusão física de desafios antigos;
- alterar a migration V3 já aplicada.

## 5. Regras funcionais e invariantes

| Regra | Valor |
| --- | --- |
| Formato do código | seis dígitos decimais |
| Intervalo possível | `000000` a `999999` |
| Gerador aleatório | `SecureRandom` |
| Telefone normalizado | `^[1-9][0-9]{9,14}$`, conforme OpenAPI |
| Validade | cinco minutos |
| Reenvio | após sessenta segundos |
| Armazenamento do código | somente HMAC-SHA-256 hexadecimal |
| Desafios não finalizados | no máximo um por telefone |
| Expiração | lógica, calculada a partir de `expira_em` |

Invariantes:

1. O código original nunca é persistido.
2. O telefone original nunca é persistido nas novas tabelas.
3. O IP original nunca é persistido.
4. Um novo desafio invalida o anterior antes de ser persistido.
5. Um desafio expirado não pode voltar a ficar válido.
6. O relógio usado nas regras é injetável e testável.
7. A criação é atômica: ou a invalidação e o novo desafio são confirmados juntos, ou nenhuma alteração permanece.
8. Concorrência entre instâncias diferentes continua produzindo somente um desafio não finalizado.
9. Código, telefone completo, HMAC e chave secreta não aparecem em logs, exceções ou `toString()`.

## 6. Configuração

Criar `VerificationCodeProperties` com prefixo:

```yaml
app:
  security:
    mobile:
      verification-code:
        hmac-key: ${MOBILE_AUTH_HMAC_KEY}
        ttl: ${MOBILE_VERIFICATION_CODE_TTL:5m}
        resend-interval: ${MOBILE_VERIFICATION_RESEND_INTERVAL:60s}
```

### 6.1 Chave HMAC

`MOBILE_AUTH_HMAC_KEY` será uma chave mestra aleatória de 32 bytes em Base64, diferente de:

- `PHONE_ENCRYPTION_KEY`;
- chave privada JWT;
- chave da API interna.

A aplicação derivará subchaves por finalidade:

```text
HMAC(chaveMestra, "mobile-verification-code-v1") -> chave do código
HMAC(chaveMestra, "mobile-client-ip-v1")         -> chave do IP
```

Isso evita reutilizar diretamente a mesma chave criptográfica em duas funções.

Uma chave de desenvolvimento pode ser gerada no PowerShell sem OpenSSL:

```powershell
[Convert]::ToBase64String(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
```

O valor real não será incluído em `.env.example`; o arquivo conterá somente nome e orientação de preenchimento.

Como a tabela não possui versão da chave, a rotação de `MOBILE_AUTH_HMAC_KEY` invalidará desafios ainda ativos. O impacto máximo será de cinco minutos e é aceitável para o MVP.

### 6.2 Validação de propriedades

A inicialização deve falhar quando:

- a chave não for Base64 válido;
- a chave decodificada tiver menos de 32 bytes;
- `ttl` for nulo ou não positivo;
- `resend-interval` for nulo ou não positivo;
- `resend-interval` for maior que `ttl`.

O número de dígitos continuará constante no código. Torná-lo configurável criaria incompatibilidade entre formato, contrato e testes sem benefício para o MVP.

### 6.3 Normalização do telefone

O `PhoneProtectionService` continuará removendo os caracteres de formatação usados pelos fluxos internos, mas o resultado deverá obedecer ao contrato mobile: primeiro dígito entre 1 e 9 e comprimento total entre 10 e 15 dígitos.

Essa validação será centralizada no serviço de proteção para evitar que o endpoint, o domínio e o fluxo existente produzam hashes diferentes para o mesmo telefone. Os testes atuais do componente serão ampliados com limites, primeiro dígito inválido e caracteres sem dígitos suficientes.

## 7. Componentes propostos

### 7.1 `DesafioAutenticacao`

Entidade mapeada para `tb_desafio_autenticacao` com:

- `UUID id`;
- `String telefoneCriptografado`;
- `String telefoneHash`;
- `String codigoHmac`;
- `String ipHash`;
- `short tentativas`;
- `Instant criadoEm`;
- `Instant expiraEm`;
- `Instant reenvioPermitidoEm`;
- `Instant consumidoEm`;
- `Instant invalidadoEm`.

A entidade deverá concentrar comportamento de estado em vez de expor setters indiscriminados:

```java
boolean estaExpirado(Instant agora)
boolean foiFinalizado()
boolean estaDisponivel(Instant agora)
void invalidar(Instant agora)
```

Definições:

```text
foiFinalizado = consumidoEm != null OU invalidadoEm != null
estaExpirado  = agora >= expiraEm
estaDisponivel = não finalizado E não expirado
```

O instante exatamente igual a `expira_em` já é considerado expirado.

O método `toString()` não deve ser gerado com campos sensíveis. Caso exista, mostrará apenas `id`, tentativas e estado temporal.

### 7.2 `DesafioAutenticacaoRepository`

Estender `JpaRepository<DesafioAutenticacao, UUID>` e oferecer operações explícitas:

```java
int invalidarNaoFinalizadosPorTelefone(String telefoneHash, Instant agora);
void adquirirBloqueioDoTelefone(long lockId);
```

A invalidação será uma atualização em massa:

```sql
UPDATE tb_desafio_autenticacao
   SET invalidado_em = :agora
 WHERE telefone_hash = :telefoneHash
   AND consumido_em IS NULL
   AND invalidado_em IS NULL
```

Ela também finaliza desafios que apenas expiraram. Isso é necessário porque o índice parcial da V3 não utiliza o relógio e ainda considera uma linha expirada, porém não finalizada, como ocupante da chave única.

O repository poderá ganhar posteriormente consultas com bloqueio pessimista para confirmação e contagens para rate limiting. Essas operações não fazem parte deste cartão.

### 7.3 `VerificationCodeGenerator`

Responsável somente por gerar o código:

```java
int valor = secureRandom.nextInt(1_000_000);
String codigo = String.format(Locale.ROOT, "%06d", valor);
```

`SecureRandom` será recebido por injeção de dependência. Isso mantém a fonte criptográfica em produção e permite teste determinístico do caso com zeros à esquerda.

O gerador não registra nem persiste o resultado.

### 7.4 `VerificationCodeHasher`

Responsável pelo HMAC e pela comparação segura.

Entrada canônica em UTF-8:

```text
v1:<challengeId>:<telefoneNormalizado>:<codigo>
```

Os componentes possuem formatos sem `:`:

- UUID canônico;
- telefone contendo apenas dígitos;
- código contendo exatamente seis dígitos.

API conceitual:

```java
String hash(UUID challengeId, String telefoneNormalizado, String codigo);

boolean matches(
    String hmacEsperado,
    UUID challengeId,
    String telefoneNormalizado,
    String codigoRecebido
);
```

O resultado será hexadecimal minúsculo de 64 caracteres. `matches` decodificará os dois valores e usará `MessageDigest.isEqual`, evitando comparação comum de `String`.

O hasher validará o formato do código antes do cálculo e nunca incluirá código ou telefone nas mensagens de erro.

### 7.5 `ClientIpProtectionService`

Receberá um endereço IP canônico já resolvido pela camada HTTP e calculará HMAC-SHA-256 com a subchave `mobile-client-ip-v1`.

Este componente não interpretará `X-Forwarded-For`. A configuração de proxies confiáveis pertence ao endpoint/infraestrutura futura. Aceitar diretamente um header controlado pelo cliente permitiria contornar limites por IP.

O resultado terá 64 caracteres hexadecimais e será o único valor persistido em `ip_hash`.

### 7.6 `DesafioAutenticacaoService`

Serviço transacional que coordena:

- `PhoneProtectionService`;
- `VerificationCodeGenerator`;
- `VerificationCodeHasher`;
- `ClientIpProtectionService`;
- `DesafioAutenticacaoRepository`;
- `Clock`;
- `VerificationCodeProperties`.

API conceitual:

```java
@Transactional
DesafioCriado criar(String telefone, String enderecoIpCanonico);
```

`DesafioCriado` transportará temporariamente:

- `challengeId`;
- telefone normalizado para o futuro adaptador WhatsApp;
- código original para envio;
- `expiraEm`;
- `reenvioPermitidoEm`.

Como contém valores sensíveis, esse objeto:

- não será DTO HTTP;
- será marcado para não ser serializado por Jackson;
- terá `toString()` explicitamente redigido;
- terá vida limitada ao fluxo de envio;
- nunca será passado a logs ou exceções.

Uma evolução posterior poderá substituir o retorno sensível por um evento interno entregue diretamente a uma porta de envio.

## 8. Concorrência e transação

### 8.1 Problema

Duas instâncias podem receber solicitações simultâneas para o mesmo telefone:

```text
Instância A: não encontra desafio -> tenta inserir A
Instância B: não encontra desafio -> tenta inserir B
```

O índice único parcial impediria a duplicidade, mas uma das requisições terminaria com violação de constraint. Isso não é um resultado aceitável para o fluxo normal de reenvio.

### 8.2 Bloqueio por telefone

Antes de invalidar e inserir, o serviço adquirirá um advisory lock transacional do PostgreSQL:

```sql
SELECT pg_advisory_xact_lock(:lockId)
```

`lockId` será derivado deterministicamente dos primeiros oito bytes do `telefone_hash` e interpretado como `long` assinado.

Características:

- chamadas do mesmo telefone são serializadas;
- telefones diferentes continuam concorrentes;
- o bloqueio é compartilhado entre instâncias;
- o PostgreSQL libera o bloqueio automaticamente no commit ou rollback;
- uma colisão extremamente improvável apenas serializa dois telefones, sem misturar dados.

O advisory lock é defesa de coordenação. O índice `uk_desafio_auth_telefone_ativo` permanece como garantia final de integridade.

### 8.3 Ordem da criação

Dentro de uma única transação:

1. normalizar o telefone;
2. calcular `telefone_hash`;
3. adquirir o bloqueio do telefone;
4. obter `agora` pelo `Clock`;
5. invalidar desafios não finalizados daquele hash;
6. gerar `challengeId` e código;
7. calcular HMAC do código;
8. criptografar o telefone;
9. calcular `ip_hash`;
10. calcular expiração e reenvio;
11. persistir o novo desafio;
12. retornar os dados mínimos para o fluxo de envio.

Se qualquer passo falhar, a invalidação do desafio anterior também será revertida.

## 9. Fluxo de dados

```text
telefone + IP canônico
        |
        v
normalização e proteção
        |
        +--> telefone criptografado
        +--> telefone_hash
        +--> ip_hash
        |
        v
bloqueio PostgreSQL por telefone
        |
        v
invalidação do desafio anterior
        |
        v
SecureRandom -> código de 6 dígitos
        |
        +--> código original: somente para envio
        |
        └--> HMAC: persistido em tb_desafio_autenticacao
```

O banco nunca terá informação suficiente para recuperar o código original.

## 10. Expiração e limpeza

Este cartão adotará expiração lógica:

- `agora >= expira_em` significa expirado;
- não é necessário atualizar imediatamente `invalidado_em` quando o relógio passa da expiração;
- ao criar outro desafio para o telefone, qualquer registro não finalizado será invalidado;
- registros expirados permanecem disponíveis para auditoria e limites de uso.

Um processo periódico de retenção poderá excluir registros antigos em outro cartão. A limpeza não é necessária para o funcionamento ou aceite do MVP neste momento.

## 11. Logs e tratamento de erros

É proibido registrar:

- código original;
- `codigo_hmac`;
- telefone normalizado ou criptografado;
- endereço IP original;
- `ip_hash` completo;
- chave mestra ou subchaves HMAC.

Se houver log operacional, ele poderá conter:

- `challengeId`;
- correlation ID recebido da camada superior;
- evento técnico, como `DESAFIO_CRIADO`;
- últimos quatro dígitos mascarados, somente quando realmente necessário.

Erros criptográficos serão convertidos em exceções técnicas genéricas. A mensagem não incluirá a entrada que causou o erro.

O endpoint futuro decidirá o formato externo e continuará respondendo de maneira que não revele se o telefone pertence a um usuário existente.

## 12. Alterações previstas

Estrutura conceitual:

```text
financial-service/src/main/java/com/wa/finance/
├── config/
│   ├── CryptographyConfiguration.java
│   └── VerificationCodeProperties.java
├── domain/
│   └── DesafioAutenticacao.java
├── repository/
│   └── DesafioAutenticacaoRepository.java
├── security/
│   ├── ClientIpProtectionService.java
│   ├── PhoneProtectionService.java (validação alinhada ao OpenAPI)
│   ├── VerificationCodeGenerator.java
│   └── VerificationCodeHasher.java
└── service/
    ├── DesafioAutenticacaoService.java
    └── DesafioCriado.java
```

Arquivos de configuração:

- acrescentar propriedades em `application.yml`;
- documentar `MOBILE_AUTH_HMAC_KEY` em `.env.example`;
- fornecer valores seguros e fixos somente nos contextos de teste.

Os nomes finais dos pacotes podem ser ajustados ao padrão existente, preservando as responsabilidades descritas.

## 13. Estratégia de testes

### 13.1 `VerificationCodeGeneratorTest`

- gera exatamente seis dígitos;
- gera `000042` quando a fonte controlada devolve 42;
- aceita os limites `000000` e `999999`;
- solicita valores com limite superior exclusivo `1_000_000`.

Não serão criados testes estatísticos de aleatoriedade; eles são instáveis e não demonstram segurança criptográfica.

### 13.2 `VerificationCodeHasherTest`

- mesmo desafio, telefone e código produzem o mesmo HMAC;
- código diferente produz HMAC diferente;
- telefone diferente produz HMAC diferente;
- `challengeId` diferente produz HMAC diferente;
- resultado possui 64 caracteres hexadecimais;
- `matches` aceita a combinação correta;
- `matches` rejeita código incorreto e HMAC malformado sem expor valores.

### 13.3 `ClientIpProtectionServiceTest`

- mesmo IP produz o mesmo hash;
- IP diferente produz hash diferente;
- resultado possui 64 caracteres hexadecimais;
- resultado não contém o IP original.

### 13.4 `PhoneProtectionServiceTest`

- mantém a equivalência entre telefone formatado e somente dígitos;
- aceita saída normalizada de 10 a 15 dígitos iniciada entre 1 e 9;
- rejeita telefone curto, longo ou iniciado por zero;
- mantém criptografia e hash de busca compatíveis com o telefone normalizado.

### 13.5 `VerificationCodePropertiesTest`

- aceita chave Base64 de 32 bytes e tempos válidos;
- rejeita Base64 inválido;
- rejeita chave curta;
- rejeita tempos nulos, zerados ou negativos;
- rejeita reenvio maior que a validade.

### 13.6 `DesafioAutenticacaoTest`

- identifica desafio disponível antes da expiração;
- considera expirado no instante exato de `expiraEm`;
- considera consumido ou invalidado como finalizado;
- invalidação é idempotente;
- não permite transição que viole as invariantes da entidade.

### 13.7 `DesafioAutenticacaoServiceTest`

- normaliza telefone antes de proteger e calcular HMAC;
- calcula expiração em cinco minutos;
- calcula reenvio em sessenta segundos;
- adquire bloqueio antes de invalidar e inserir;
- invalida desafio anterior;
- persiste somente telefone criptografado, hashes e HMAC;
- não persiste o código original;
- rollback preserva o desafio anterior quando a nova criação falha;
- objeto de retorno não revela valores sensíveis em `toString()`.

### 13.8 Integração PostgreSQL

Criar teste com Testcontainers para:

- persistir e recuperar a entidade usando `TIMESTAMPTZ` e `Instant`;
- criar um desafio e confirmar que o código original não aparece em nenhuma coluna;
- criar outro desafio para o mesmo telefone e confirmar que o anterior foi invalidado;
- executar duas criações concorrentes para o mesmo telefone e confirmar:
  - ambas terminam sem violação inesperada;
  - somente uma linha permanece não finalizada;
  - a última criação substitui a anterior;
- executar criações para telefones diferentes sem bloqueio lógico entre elas;
- reiniciar o contexto ou abrir uma nova transação e confirmar que o estado permanece disponível.

O teste de concorrência deverá usar threads e transações realmente distintas. Um teste unitário com mocks não valida o comportamento do advisory lock.

## 14. Sequência de implementação

1. Criar e validar `VerificationCodeProperties`.
2. Registrar `SecureRandom` e `Clock` como dependências injetáveis.
3. Implementar a derivação das subchaves HMAC.
4. Implementar `VerificationCodeGenerator`.
5. Implementar `VerificationCodeHasher` e comparação constante.
6. Implementar `ClientIpProtectionService`.
7. Mapear `DesafioAutenticacao`.
8. Criar `DesafioAutenticacaoRepository` com invalidação e advisory lock.
9. Implementar `DesafioAutenticacaoService` e seu resultado protegido.
10. Adicionar configurações de aplicação e teste.
11. Criar testes unitários.
12. Criar testes PostgreSQL de persistência, substituição e concorrência.
13. Executar a suíte completa e revisar logs de teste.

## 15. Critérios de aceite

O cartão poderá ser concluído quando houver evidência automatizada de que:

- todo código gerado possui seis dígitos e usa `SecureRandom`;
- o código persistido é exclusivamente um HMAC-SHA-256;
- não é possível recuperar o código original a partir do banco;
- o telefone é normalizado e armazenado somente criptografado e em hash;
- o IP é armazenado somente como HMAC;
- a validade é de cinco minutos;
- o reenvio é liberado após sessenta segundos;
- um novo desafio invalida o anterior atomicamente;
- concorrência entre instâncias deixa apenas um desafio não finalizado;
- a expiração lógica funciona no limite exato do tempo;
- código, telefone completo e segredos não aparecem em logs ou `toString()`;
- testes unitários e de integração passam;
- os 52 testes existentes continuam sem regressão.

## 16. Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| usar gerador previsível | utilizar exclusivamente `SecureRandom` |
| perder zeros à esquerda | formatar com `%06d` e `Locale.ROOT` |
| ataque ao banco revelar códigos | persistir HMAC com segredo externo, nunca o código |
| reutilização da chave de telefone | usar chave mestra mobile independente e subchaves por finalidade |
| concatenação ambígua no HMAC | entrada canônica versionada com formatos restritos |
| comparação revelar diferenças de tempo | usar `MessageDigest.isEqual` |
| duas instâncias criarem desafios concorrentes | advisory lock transacional e índice único parcial |
| falha após invalidar o código anterior | envolver invalidação e criação na mesma transação |
| confiar em IP fornecido pelo cliente | receber somente endereço canônico resolvido pela infraestrutura futura |
| objeto interno sensível aparecer em logs | `toString()` redigido e proibição explícita de logging |
| rotação do segredo invalidar códigos ativos | aceitar janela máxima de cinco minutos no MVP |
| tabela crescer indefinidamente | definir política de retenção em cartão posterior |

## 17. Resultado esperado

Após o cartão [07], o serviço terá um componente de domínio capaz de criar desafios seguros:

```text
criar(telefone, ip)
       |
       +--> desafio anterior invalidado
       +--> novo challengeId
       +--> código aleatório de seis dígitos
       +--> HMAC persistido
       +--> validade e reenvio persistidos
       └--> código original disponível apenas para o futuro envio
```

O aplicativo ainda não poderá solicitar o código pela API. O cartão [08] usará esse serviço para implementar `POST /mobile/v1/auth/codes`, e uma integração posterior entregará o código pelo WhatsApp.

## 18. Evidências da implementação

O escopo definido neste SDD foi implementado com os seguintes componentes:

- propriedades tipadas e validadas para chave HMAC, TTL e intervalo de reenvio;
- derivação de chaves HMAC distintas para código de verificação e endereço IP;
- geração de código de seis dígitos com `SecureRandom`;
- hash HMAC-SHA-256 vinculado ao desafio, telefone normalizado e código;
- comparação do hash em tempo constante;
- persistência do desafio na tabela `tb_desafio_autenticacao`, sem armazenar o código ou o telefone em texto puro;
- invalidação do desafio anterior e criação do novo desafio na mesma transação;
- serialização das criações concorrentes para o mesmo telefone por advisory lock transacional do PostgreSQL;
- objeto interno de retorno protegido contra serialização acidental pelo Jackson;
- configuração documentada em `.env.example` e integrada ao `docker-compose.yml`;
- testes unitários, de integração com PostgreSQL 15 e de rollback transacional.

Validação final executada:

```text
Successfully validated 3 migrations
PostgreSQL 15 via Testcontainers
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Continuam fora deste card, conforme o escopo planejado: exposição do endpoint HTTP,
envio do código pelo WhatsApp, validação do código recebido, contagem de tentativas e
emissão dos tokens da sessão mobile.
