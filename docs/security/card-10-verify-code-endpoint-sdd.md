# SDD — Confirmação do código de acesso

- Cartão: `[10] Criar endpoint de confirmação do código`
- Status: implementado
- Data: 2026-10-17
- Serviço: `financial-service`
- Dependências: cartões [07], [08] e [09] concluídos
- Rota: `POST /mobile/v1/auth/codes/verify`

## 1. Objetivo

Implementar a confirmação segura do desafio criado por `POST /mobile/v1/auth/codes`. O endpoint receberá o `challengeId`, o código de seis dígitos e, opcionalmente, o nome informativo do dispositivo. Em caso de sucesso, consumirá o desafio exatamente uma vez e localizará ou criará o usuário associado ao telefone confirmado.

O desenho deverá garantir que:

- código, telefone, HMAC e chaves nunca sejam devolvidos ou registrados;
- desafio inexistente, expirado, invalidado, consumido ou com código incorreto produza a mesma resposta pública;
- cada tentativa incorreta seja contabilizada de forma transacional;
- o código seja comparado em tempo constante;
- duas confirmações concorrentes do mesmo desafio tenham no máximo um sucesso;
- o usuário seja localizado ou criado somente depois da prova de posse do telefone;
- o desafio consumido e o usuário resultante sejam persistidos na mesma transação;
- a implementação prepare a emissão atômica de sessão dos cartões [12] e [13], sem inventar token provisório.

## 2. Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)
- [Segurança e estratégia de testes da API mobile](mobile-api-security.md)
- [SDD do cartão 07 — Geração e armazenamento seguro do código](card-07-verification-code-sdd.md)
- [SDD do cartão 08 — Endpoint para solicitar código](card-08-request-code-endpoint-sdd.md)
- [SDD do cartão 09 — Entrega do código pelo WhatsApp](card-09-whatsapp-code-delivery-sdd.md)

## 3. Contexto atual

Os cartões anteriores já entregam:

- desafio persistido em `tb_desafio_autenticacao`;
- telefone criptografado com AES-GCM e índice de busca por HMAC;
- código de seis dígitos gerado com `SecureRandom`;
- HMAC-SHA-256 do código vinculado ao `challengeId` e telefone;
- validade nominal de cinco minutos;
- invalidação do desafio anterior no reenvio;
- contagem persistida de tentativas com restrição entre zero e cinco;
- entrega assíncrona do código pelo WhatsApp;
- rota `/mobile/v1/auth/codes/verify` liberada na cadeia pública de autenticação;
- correlação e respostas de erro mobile sem detalhes internos.

Ainda não existe:

- DTO de confirmação;
- método de domínio para registrar tentativa e consumo;
- busca do desafio com bloqueio de escrita;
- serviço transacional de confirmação;
- integração entre confirmação e `UsuarioService`;
- mapeamento HTTP dos casos inválidos;
- testes de concorrência da confirmação.

O `VerificationCodeHasher` já possui `matches`, que recalcula o HMAC e usa `MessageDigest.isEqual`. Ele será reutilizado; não será criada outra implementação criptográfica.

## 4. Decisões arquiteturais

### 4.1 Bloqueio pessimista por desafio

O desafio será carregado com `PESSIMISTIC_WRITE`, equivalente a:

```sql
SELECT *
  FROM tb_desafio_autenticacao
 WHERE id = :challengeId
 FOR UPDATE;
```

O bloqueio permanecerá até o fim da transação. A primeira confirmação poderá consumir o desafio. Uma segunda requisição concorrente aguardará, relerá o registro já consumido e será rejeitada.

Foi escolhido bloqueio pessimista, em vez de somente verificar e depois salvar, porque o fluxo é curto, a contenção esperada por desafio é baixa e a propriedade de uso único precisa ser simples de provar.

### 4.2 Um único erro público para falhas de autenticação

Estas situações retornarão a mesma resposta:

- `challengeId` bem formado, mas inexistente;
- desafio expirado;
- desafio invalidado por reenvio;
- desafio já consumido;
- código incorreto;
- desafio sem possibilidade de novas tentativas.

Resposta:

```http
HTTP/1.1 401 Unauthorized
Cache-Control: no-store
```

```json
{
  "code": "INVALID_OR_EXPIRED_CODE",
  "message": "O código é inválido ou expirou",
  "timestamp": "2026-10-17T16:00:00-03:00",
  "path": "/mobile/v1/auth/codes/verify",
  "correlationId": "b5f670f5-a9b8-4597-ad03-2af7b35fb391"
}
```

O cliente não poderá distinguir se o desafio existe nem qual estado interno causou a rejeição.

### 4.3 Sem token provisório

O OpenAPI final define sucesso `200` com `TokenResponse`, mas:

- o access token será implementado no cartão [12];
- a sessão e o refresh token serão implementados no cartão [13].

O cartão [10] não criará um token temporário, JWT incompleto, identificador de usuário público ou segunda etapa secreta. Durante o desenvolvimento incremental, antes da integração dos cartões [12] e [13], o sucesso técnico do endpoint será:

```http
HTTP/1.1 204 No Content
Cache-Control: no-store
```

Essa resposta é transitória e não altera o contrato final publicado. A API mobile não deverá ser considerada pronta para consumo externo até o cartão [13]. No cartão [13], a mesma rota passará a responder `200 TokenResponse` sem alterar o request.

### 4.4 Fronteira transacional preparada para sessão

O serviço de confirmação retornará internamente o usuário confirmado, mas não confirmará a transação prematuramente por desenho de API. Nos cartões [12] e [13], um orquestrador transacional envolverá:

```text
validar desafio
  + consumir desafio
  + localizar/criar usuário
  + criar sessão/refresh token
  + emitir metadados necessários ao JWT
```

Assim, se a criação da sessão falhar, o desafio não ficará consumido sem entregar uma sessão.

No estado intermediário do cartão [10], a transação termina após a criação/localização do usuário e o consumo do desafio. Esse estado é apenas de desenvolvimento e teste.

## 5. Escopo

### 5.1 Incluído

- implementar o request de `POST /mobile/v1/auth/codes/verify`;
- validar `challengeId`, código e nome opcional do dispositivo;
- rejeitar campos JSON desconhecidos;
- buscar o desafio com bloqueio pessimista;
- validar expiração, consumo e invalidação;
- decifrar o telefone somente depois de validar o estado do desafio;
- comparar o HMAC em tempo constante;
- incrementar tentativas incorretas;
- invalidar o desafio ao atingir cinco tentativas incorretas;
- consumir o desafio atomicamente no sucesso;
- localizar ou criar o usuário pelo telefone confirmado;
- garantir as categorias padrão do usuário pelo comportamento atual de `UsuarioService`;
- devolver erro público uniforme;
- adicionar `Cache-Control: no-store` e `X-Correlation-Id`;
- criar testes unitários, MVC, integração PostgreSQL e concorrência.

### 5.2 Fora do escopo

- emitir ou validar access token JWT — cartão [12];
- criar sessão ou refresh token — cartão [13];
- rotacionar refresh token — cartão [14];
- implementar logout — cartão [15];
- limites por telefone, IP ou instalação e bloqueio progressivo — cartão [11];
- aceitar telefone no request de confirmação;
- reenviar código automaticamente;
- alterar a entrega pelo RabbitMQ ou WuzAPI;
- retornar nome, telefone, ID do usuário ou qualquer dado de perfil;
- criar uma nova migration, salvo se a implementação revelar incompatibilidade real no esquema existente.

## 6. Contrato HTTP

### 6.1 Requisição

```http
POST /mobile/v1/auth/codes/verify
Content-Type: application/json
```

```json
{
  "challengeId": "538f1567-a3eb-4e6d-b06d-e5987a340672",
  "codigo": "482913",
  "nomeDispositivo": "Samsung Galaxy S24"
}
```

Regras:

| Campo | Obrigatório | Regra |
| --- | --- | --- |
| `challengeId` | sim | UUID válido |
| `codigo` | sim | exatamente seis dígitos (`^[0-9]{6}$`) |
| `nomeDispositivo` | não | após `trim`, entre 1 e 100 caracteres |
| campos adicionais | — | rejeitados |

O request não aceitará telefone nem `usuarioId`. A identidade vem exclusivamente do desafio.

### 6.2 Resposta intermediária do cartão [10]

```http
HTTP/1.1 204 No Content
Cache-Control: no-store
X-Correlation-Id: b5f670f5-a9b8-4597-ad03-2af7b35fb391
```

Não haverá corpo. Essa resposta será substituída pelo `200 TokenResponse` final quando os cartões [12] e [13] forem integrados.

### 6.3 Erros

| Situação | HTTP | `code` | Observação |
| --- | --- | --- | --- |
| JSON inválido ou fora do contrato | 400 | `INVALID_REQUEST` | não consulta o desafio |
| desafio/código rejeitado | 401 | `INVALID_OR_EXPIRED_CODE` | mensagem única para todos os estados |
| limite global/de abuso | 429 | `TOO_MANY_REQUESTS` | reservado ao cartão [11] |
| banco ou dependência indisponível | 503 | `SERVICE_UNAVAILABLE` | sem detalhes internos |

Todas as respostas terão `Cache-Control: no-store` e `X-Correlation-Id`.

## 7. Modelo de domínio

### 7.1 Alterações em `DesafioAutenticacao`

Adicionar operações de domínio; controller e serviço não alterarão campos diretamente:

```java
public boolean podeSerConfirmado(Instant agora) {
    return consumidoEm == null
            && invalidadoEm == null
            && agora.isBefore(expiraEm)
            && tentativas < 5;
}

public void registrarTentativaIncorreta(Instant agora) {
    // exige desafio ainda confirmável
    // incrementa sem ultrapassar 5
    // na quinta falha, define invalidadoEm = agora
}

public void consumir(Instant agora) {
    // exige desafio ainda confirmável
    // define consumidoEm = agora
}
```

As invariantes serão:

- `tentativas` nunca menor que zero nem maior que cinco;
- desafio consumido não pode ser invalidado;
- desafio invalidado não pode ser consumido;
- instante de finalização não pode anteceder `criadoEm`;
- desafio expirado não pode ser consumido;
- somente código correto consome o desafio;
- tentativa correta não incrementa `tentativas`.

### 7.2 Tentativas incorretas

A tentativa que apresentar código incorreto será persistida mesmo que o endpoint devolva `401`. Portanto, a rejeição esperada não poderá provocar rollback da transação.

O fluxo deverá retornar um resultado controlado ou lançar a exceção pública somente depois de garantir o commit da tentativa. A implementação não usará uma `RuntimeException` lançada dentro da mesma transação se isso fizer o Spring reverter o incremento.

Opções aceitáveis:

1. o serviço transacional retorna `ConfirmacaoCodigoResultado.rejeitado()` e o controller converte em `401`; ou
2. um método transacional interno persiste a tentativa e o método externo lança a exceção após o commit.

A opção recomendada é resultado explícito, pois torna o comportamento de commit visível e testável.

## 8. Persistência e concorrência

### 8.1 Consulta com bloqueio

Adicionar ao `DesafioAutenticacaoRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select d from DesafioAutenticacao d where d.id = :id")
Optional<DesafioAutenticacao> findByIdForUpdate(@Param("id") UUID id);
```

O bloqueio é por linha do desafio. O advisory lock por telefone usado na criação não será necessário na confirmação, porque:

- o reenvio já invalida desafios anteriores sob lock por telefone;
- a confirmação precisa serializar o uso de um `challengeId` específico;
- a restrição parcial garante no máximo um desafio não finalizado por telefone.

### 8.2 Ordem dentro da transação

```text
BEGIN
  SELECT desafio FOR UPDATE
  validar estado temporal
  decifrar telefone
  calcular e comparar HMAC

  código incorreto:
    incrementar tentativas
    possivelmente invalidar
    COMMIT
    responder 401

  código correto:
    localizar/criar usuário pelo hash do telefone
    garantir categorias padrão
    consumir desafio
    COMMIT
    responder 204 no estágio atual
END
```

Consumir por último permite que falhas durante a criação do usuário ou das categorias revertam todo o sucesso. Como tudo participa da mesma transação, nenhuma confirmação parcial ficará persistida.

### 8.3 Concorrência esperada

Para duas requisições simultâneas com o código correto:

```text
requisição A                        requisição B

obtém FOR UPDATE                    aguarda lock
confere HMAC
cria/localiza usuário
marca consumido
commit
                                    obtém lock
                                    encontra consumido
204                                 401
```

Para tentativas incorretas concorrentes, cada requisição obterá o lock em sequência e o contador refletirá todas as tentativas confirmadas no banco, sem lost update.

## 9. Usuário confirmado

O telefone virá exclusivamente de `telefoneCriptografado` do desafio. O fluxo será:

```java
String telefone = phoneProtectionService.decrypt(desafio.getTelefoneCriptografado());
String telefoneHash = phoneProtectionService.lookupHash(telefone);
```

Antes de continuar, o hash recalculado deverá ser comparado em tempo constante com `desafio.getTelefoneHash()`. Uma divergência indica dado inconsistente ou chave incorreta e será tratada como falha interna, nunca como tentativa de código.

Depois do código correto:

```java
Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
```

O serviço existente:

- procura por `telefone_hash`;
- cria o usuário quando ausente;
- cifra o telefone antes de persistir;
- garante categorias padrão.

Nenhum usuário será criado para código incorreto, desafio expirado, invalidado ou consumido.

## 10. Componentes previstos

### 10.1 Novos componentes

| Componente | Responsabilidade |
| --- | --- |
| `VerificarCodigoRequest` | contrato HTTP e Bean Validation |
| `ConfirmacaoCodigoService` | orquestra confirmação transacional |
| `ConfirmacaoCodigoResultado` | sucesso/rejeição sem rollback acidental |
| `CodigoConfirmado` | resultado interno contendo usuário confirmado e nome opcional do dispositivo |
| `InvalidCodeException` ou mapeamento equivalente | produzir erro público uniforme fora da transação |

### 10.2 Componentes alterados

| Componente | Alteração |
| --- | --- |
| `MobileAuthController` | adicionar `POST /codes/verify` |
| `DesafioAutenticacao` | métodos de tentativa, disponibilidade e consumo |
| `DesafioAutenticacaoRepository` | busca com `PESSIMISTIC_WRITE` |
| `MobileApiExceptionHandler` | mapear rejeição para `401` e preservar resposta genérica |
| `UsuarioService` | somente ajustes mínimos se necessários para uso transacional seguro |

### 10.3 Componentes reutilizados

- `VerificationCodeHasher` para comparação constante;
- `PhoneProtectionService` para decifrar, normalizar e conferir o telefone;
- `UsuarioRepository` e restrição única de `telefone_hash`;
- `MobileCorrelationIdFilter` para correlação;
- `MobileErrorResponse` para erros;
- `Clock` para testes determinísticos.

## 11. Pseudocódigo do serviço

```java
@Transactional
public ConfirmacaoCodigoResultado confirmar(
        UUID challengeId,
        String codigo,
        String nomeDispositivo
) {
    Instant agora = clock.instant();
    Optional<DesafioAutenticacao> encontrado = repository.findByIdForUpdate(challengeId);

    if (encontrado.isEmpty()) {
        return ConfirmacaoCodigoResultado.rejeitado();
    }

    DesafioAutenticacao desafio = encontrado.get();
    if (!desafio.podeSerConfirmado(agora)) {
        return ConfirmacaoCodigoResultado.rejeitado();
    }

    String telefone = phoneProtectionService.decrypt(desafio.getTelefoneCriptografado());
    validarIntegridadeDoTelefone(desafio, telefone);

    boolean correto = verificationCodeHasher.matches(
            desafio.getCodigoHmac(),
            desafio.getId(),
            telefone,
            codigo
    );

    if (!correto) {
        desafio.registrarTentativaIncorreta(agora);
        return ConfirmacaoCodigoResultado.rejeitado();
    }

    Usuario usuario = usuarioService.buscarOuCriarUsuarioPorTelefone(telefone);
    desafio.consumir(agora);
    return ConfirmacaoCodigoResultado.confirmado(usuario, normalizarNome(nomeDispositivo));
}
```

O código real não registrará request, código, telefone, HMAC, conteúdo criptografado nem entidade completa.

## 12. Segurança e observabilidade

### 12.1 Logs permitidos

Eventos possíveis:

```text
event=MOBILE_AUTH_VERIFY_REJECTED challengeId=<uuid> correlationId=<uuid> category=<categoria>
event=MOBILE_AUTH_VERIFY_CONFIRMED challengeId=<uuid> correlationId=<uuid> userId=<id>
event=MOBILE_AUTH_VERIFY_FAILURE correlationId=<uuid> category=<categoria>
```

Categorias internas poderão distinguir `NOT_FOUND`, `EXPIRED`, `INVALIDATED`, `CONSUMED`, `INVALID_CODE` e `ATTEMPTS_EXHAUSTED`, mas essa distinção não será enviada ao cliente.

O nível de log deverá evitar facilitar enumeração em produção. Rejeições esperadas não terão stack trace.

### 12.2 Dados proibidos

Não registrar:

- código recebido ou armazenado;
- telefone completo ou mascarado neste fluxo;
- `codigoHmac`, `telefoneHash` ou `ipHash`;
- chave HMAC, chave do telefone ou chave de entrega;
- JSON bruto do request;
- conteúdo de entidades cujo `toString` possa mudar no futuro.

### 12.3 Cache

Sucesso e erro usarão:

```http
Cache-Control: no-store
```

Isso evita armazenamento de respostas do fluxo de autenticação por browsers, proxies ou ferramentas intermediárias.

## 13. Estratégia de testes

### 13.1 Testes unitários de domínio

- desafio disponível aceita consumo;
- consumo define `consumidoEm`;
- desafio consumido não pode ser consumido novamente;
- desafio invalidado não pode ser consumido;
- desafio expirado não pode ser consumido;
- tentativa incorreta incrementa exatamente uma vez;
- quinta tentativa invalida o desafio;
- contador nunca ultrapassa cinco;
- tentativa correta não incrementa contador;
- instantes anteriores à criação são rejeitados.

### 13.2 Testes do serviço

- código correto retorna usuário existente;
- código correto cria usuário ausente;
- categorias padrão são garantidas somente após sucesso;
- código incorreto persiste incremento e não cria usuário;
- desafio inexistente não tenta decifrar telefone;
- expirado, consumido e invalidado são rejeitados;
- HMAC malformado é rejeitado sem comparação insegura;
- divergência entre telefone decifrado e `telefoneHash` produz falha interna;
- nome do dispositivo ausente permanece `null`;
- nome é aparado e limitado na fronteira HTTP;
- nenhuma informação sensível aparece em mensagens de exceção.

### 13.3 Testes MVC

- request válido produz `204` no estágio do cartão [10];
- request inválido produz `400`;
- código/desafio rejeitado produz `401` genérico;
- `Cache-Control: no-store` está presente;
- `X-Correlation-Id` é devolvido;
- campos desconhecidos são rejeitados;
- telefone e `usuarioId` não pertencem ao contrato;
- rota continua pública sem JWT e sem `X-Internal-Api-Key`;
- método HTTP diferente é negado.

### 13.4 Integração PostgreSQL/Testcontainers

- `findByIdForUpdate` usa PostgreSQL real;
- incremento incorreto é commitado mesmo com resposta `401`;
- confirmação persiste `consumido_em` e usuário na mesma transação;
- falha ao criar usuário reverte o consumo;
- usuário existente não é duplicado;
- duas confirmações corretas simultâneas resultam em um sucesso e uma rejeição;
- tentativas incorretas simultâneas não perdem incrementos;
- desafio invalidado por reenvio não pode ser confirmado;
- restrições da migration continuam válidas.

O teste concorrente usará duas transações e conexões reais coordenadas por barreira/latch. Mock unitário não é evidência suficiente para o requisito de atomicidade.

### 13.5 Regressão

- solicitação do Card 08 continua retornando `202`;
- outbox e envio do Card 09 continuam funcionando;
- rotas `/v1/**` continuam exigindo `X-Internal-Api-Key`;
- rotas mobile protegidas continuam exigindo JWT;
- fluxo financeiro atual do WhatsApp não muda.

## 14. Plano de implementação

1. Adicionar regras de tentativa e consumo em `DesafioAutenticacao`.
2. Adicionar `findByIdForUpdate` ao repositório.
3. Criar `VerificarCodigoRequest`.
4. Criar resultado explícito de confirmação.
5. Implementar `ConfirmacaoCodigoService` transacional.
6. Integrar com `PhoneProtectionService`, `VerificationCodeHasher` e `UsuarioService`.
7. Adicionar `/codes/verify` ao `MobileAuthController`.
8. Mapear rejeição uniforme no `MobileApiExceptionHandler`.
9. Criar testes unitários de domínio e serviço.
10. Criar testes MVC do contrato intermediário.
11. Criar testes PostgreSQL de rollback e concorrência.
12. Executar a suíte completa do `financial-service`.
13. Confirmar que nenhum teste ou log imprime segredos.
14. No Card [13], substituir `204` pelo `200 TokenResponse` final dentro da mesma orquestração transacional.

## 15. Critérios de aceite

O cartão [10] estará concluído quando:

- request válido alcançar o serviço de confirmação;
- código correto consumir o desafio uma única vez;
- código incorreto incrementar tentativas de forma persistente;
- desafio inexistente, expirado, invalidado, consumido ou incorreto retornar `401` indistinguível;
- usuário for localizado ou criado somente após código correto;
- confirmação e criação do usuário forem atômicas;
- duas confirmações concorrentes produzirem no máximo um sucesso;
- não houver código, telefone ou hashes em logs e respostas;
- testes unitários, MVC e PostgreSQL passarem;
- a limitação transitória da resposta `204` estiver registrada e a API não for declarada pronta antes dos cartões [12] e [13].

O requisito final “tentativas concorrentes emitem no máximo uma sessão” será plenamente demonstrado no cartão [13]. Neste cartão, a garantia verificável é mais fundamental: tentativas concorrentes produzem no máximo uma confirmação consumida, que será a pré-condição para uma única sessão.

## 16. Riscos e mitigações

| Risco | Mitigação |
| --- | --- |
| duas requisições usam o mesmo código | `PESSIMISTIC_WRITE` e consumo na mesma transação |
| incremento de tentativa sofre rollback ao retornar erro | resultado explícito e erro convertido fora da decisão transacional |
| criação de usuário ocorre antes da prova de posse | chamar `UsuarioService` somente após HMAC correto |
| telefone cifrado não corresponde ao hash | recalcular e comparar antes de localizar usuário |
| diferença de tempo na comparação do código | `MessageDigest.isEqual` já usado pelo hasher |
| resposta revela estado do desafio | `401` único com mensagem genérica |
| desafio é consumido e sessão futura falha | cartões [12]/[13] envolverão confirmação e sessão na mesma transação |
| contrato intermediário confunde o app | não liberar integração externa antes do `TokenResponse` final |
| logs revelam segredo por conveniência | eventos estruturados apenas com IDs técnicos e categorias |

## 17. Evolução nos próximos cartões

- Card [11]: limites por telefone/IP, `429`, `Retry-After` e bloqueio progressivo.
- Card [12]: emissão e validação do access token JWT.
- Card [13]: sessão persistida, refresh token opaco e resposta final do `/codes/verify`.
- Card [14]: rotação e detecção de reutilização do refresh token.
- Card [15]: logout e gerenciamento de sessões.
