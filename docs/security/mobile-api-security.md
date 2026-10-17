# Segurança e estratégia de testes da API mobile

- Status: aprovado
- Data: 2026-09-28
- Escopo: MVP da API mobile

## Documentos relacionados

- [ADR 001 — API mobile no financial-service](../adr/001-api-mobile-no-financial-service.md)
- [Contrato OpenAPI da API mobile](../openapi/mobile-api.yml)

## Objetivo

Definir regras verificáveis de autenticação, autorização, proteção de dados e testes antes da implementação da API mobile.

## Princípios

- negar acesso por padrão;
- identificar o usuário exclusivamente pelo token autenticado;
- limitar o impacto do roubo de credenciais;
- não armazenar códigos ou tokens recuperáveis no banco;
- não revelar se um telefone já está cadastrado;
- não registrar segredos ou dados pessoais completos em logs;
- manter a autenticação mobile separada da autenticação interna.

## Fronteiras de autenticação

| Rotas | Proteção |
| --- | --- |
| `/actuator/health` | Health check conforme configuração do ambiente |
| `/v1/**` | `X-Internal-Api-Key` |
| `/mobile/v1/auth/codes` | Validação de entrada e limites de uso |
| `/mobile/v1/auth/codes/verify` | Desafio, código e limites de tentativas |
| `/mobile/v1/auth/refresh` | Refresh token válido |
| `/mobile/v1/auth/logout` | Refresh token; operação idempotente |
| Demais `/mobile/v1/**` | Access token JWT obrigatório |
| Rotas não previstas | Acesso negado |

A chave interna não concede acesso às rotas mobile. Um JWT mobile não concede acesso às rotas internas.

## Access token JWT

O access token será um JWT assinado com chave assimétrica usando RS256 e terá validade de 15 minutos.

Claims obrigatórios:

| Claim | Finalidade |
| --- | --- |
| `sub` | Identificador interno e estável do usuário |
| `sid` | Identificador da sessão mobile |
| `iss` | Emissor `whatsapp-finance-tracker` |
| `aud` | Audiência `finance-tracker-mobile` |
| `iat` | Momento da emissão |
| `exp` | Momento da expiração |
| `jti` | Identificador único do token |

A validação deve conferir:

- assinatura;
- algoritmo permitido, sem aceitar algoritmos informados dinamicamente pelo cliente;
- emissor;
- audiência;
- expiração e instante de início, quando presente;
- estrutura e tipos das claims obrigatórias.

A chave privada deve permanecer fora do repositório. Somente a chave pública será usada pelos componentes que apenas validam tokens.

## Refresh token e sessão

O refresh token será um valor opaco de 256 bits gerado por fonte criptograficamente segura. Ele não será um JWT.

Regras:

- persistir somente o hash do token;
- vincular o token a um usuário, uma sessão e uma família de rotação;
- expirar após 30 dias de inatividade;
- aplicar limite absoluto de 90 dias para a sessão;
- emitir um novo refresh token em cada renovação;
- invalidar o refresh token utilizado;
- preservar informações suficientes para detectar reutilização;
- ao detectar reutilização de token antigo, revogar a família da sessão;
- revogar a sessão no logout;
- nunca registrar o token original em logs.

Após o logout, o access token já emitido poderá permanecer válido até completar seus 15 minutos. A revogação imediata de cada JWT, que exigiria uma lista de bloqueio ou consulta de sessão em toda requisição, fica fora do MVP.

## Código de verificação pelo WhatsApp

| Regra | Valor |
| --- | --- |
| Formato | Seis dígitos |
| Validade | Cinco minutos |
| Tentativas | Cinco por desafio |
| Intervalo de reenvio | Sessenta segundos |
| Uso | Único |

O código não será armazenado diretamente. O valor persistido será calculado com HMAC-SHA-256 sobre `challengeId`, telefone normalizado e código, usando um segredo do servidor mantido fora do repositório.

Regras adicionais:

- um novo desafio invalida o desafio ativo anterior do telefone;
- uma confirmação bem-sucedida consome o desafio de forma atômica;
- cinco erros invalidam o desafio;
- respostas externas não informam se o telefone pertence a um usuário existente;
- o usuário somente é criado depois da confirmação correta;
- código, HMAC e telefone completo não aparecem em logs.

## Limites contra abuso

| Limite | Regra |
| --- | --- |
| Reenvio por telefone | Um a cada 60 segundos |
| Solicitações por telefone | Cinco por hora |
| Solicitações por telefone | Dez por dia |
| Solicitações por IP | Trinta por hora |
| Confirmações incorretas | Cinco por desafio |

Ao atingir um limite, a API responderá `429 Too Many Requests` com o header `Retry-After`.

O limite por instalação não faz parte do MVP. Um identificador fornecido pelo aplicativo é fácil de apagar ou falsificar e não deve ser tratado como prova de identidade.

Os limites devem funcionar de maneira consistente quando houver mais de uma instância da aplicação. Para o MVP, os dados necessários serão persistidos no PostgreSQL ou em outro armazenamento compartilhado; contadores exclusivamente em memória não são suficientes para múltiplas instâncias.

## Proteção no aplicativo e no transporte

- HTTPS é obrigatório fora do ambiente local;
- o access token deve permanecer preferencialmente em memória;
- o refresh token deve ficar no Android Keystore ou iOS Keychain;
- o aplicativo deve apagar os dois tokens no logout;
- respostas contendo tokens devem usar `Cache-Control: no-store`;
- tokens não devem ser enviados em query parameters;
- autenticação mobile usa `Authorization: Bearer <access-token>`;
- CSRF será desabilitado para a API mobile, que não utiliza cookie de sessão;
- `nomeDispositivo` é opcional e apenas informativo;
- `nomeDispositivo` não será usado como fator de autenticação.

## Autorização e isolamento

- o proprietário dos dados vem exclusivamente do claim `sub`;
- DTOs financeiros não recebem telefone nem `usuarioId`;
- toda consulta de transação, categoria ou relatório inclui o usuário autenticado;
- recursos de outro usuário devem ser tratados como não encontrados;
- o cliente não escolhe claims do JWT;
- IDs recebidos nunca substituem a verificação de propriedade;
- a chave de idempotência é validada no contexto do usuário autenticado.

## Logs e segredos

É proibido registrar:

- códigos de verificação;
- access tokens;
- refresh tokens;
- chaves privadas;
- segredos de HMAC;
- chave interna completa;
- telefone completo.

Logs de segurança devem usar identificadores de correlação, IDs técnicos não sensíveis e telefone mascarado ou hash quando indispensável.

## Modelo de ameaças do MVP

Os controles e testes devem considerar:

1. enumeração de usuários por telefone;
2. tentativa de adivinhar códigos;
3. reutilização de código consumido ou expirado;
4. confirmação concorrente do mesmo desafio;
5. roubo e reutilização de refresh token;
6. adulteração ou reutilização de JWT;
7. acesso de um usuário aos dados de outro;
8. confusão entre autenticação interna e mobile;
9. duplicação de gastos por repetição da requisição;
10. exposição de segredos ou dados pessoais em logs;
11. interceptação de tráfego sem TLS.

## Estratégia de testes

### Testes unitários

- geração do código com fonte segura;
- cálculo e comparação do HMAC;
- expiração, consumo e limite de tentativas;
- normalização do telefone;
- emissão das claims obrigatórias do JWT;
- validação de assinatura, algoritmo, emissor, audiência e expiração;
- geração e hash do refresh token;
- rotação, expiração e revogação da sessão;
- validação de valores, datas e períodos financeiros.

### Testes de integração

Usar PostgreSQL real com Testcontainers para validar:

- migrations de desafios e sessões;
- restrições e índices;
- confirmação concorrente do mesmo código;
- renovação concorrente do mesmo refresh token;
- detecção de reutilização e revogação da família;
- logout idempotente;
- limites por telefone e IP;
- idempotência do registro de gasto;
- consultas filtradas pelo usuário autenticado.

### Testes HTTP e de segurança

- rota protegida sem JWT;
- JWT válido, adulterado, expirado ou com algoritmo inesperado;
- JWT com emissor ou audiência incorretos;
- chave interna aceita somente em `/v1/**`;
- JWT aceito somente em `/mobile/v1/**` protegido;
- usuário A impedido de acessar recursos do usuário B;
- código inválido, expirado, consumido ou bloqueado;
- refresh token inválido, expirado, revogado ou reutilizado;
- respostas `400`, `401`, `404`, `409`, `422` e `429` no formato do OpenAPI;
- ausência de segredos e dados pessoais completos nos logs.

### Teste ponta a ponta

Executar o fluxo:

1. solicitar código;
2. confirmar código;
3. receber access e refresh tokens;
4. listar categorias;
5. registrar gasto;
6. consultar transações e relatório;
7. renovar a sessão;
8. repetir uma operação protegida;
9. efetuar logout;
10. tentar renovar e receber `401 Unauthorized`.

## Definition of Done

Uma tarefa relacionada à segurança somente será concluída quando:

- o comportamento estiver implementado;
- testes positivos e negativos estiverem presentes;
- cenários concorrentes relevantes estiverem testados;
- migrations estiverem validadas com PostgreSQL;
- contrato OpenAPI e documentação estiverem atualizados;
- nenhum segredo ou telefone completo aparecer nos logs;
- a suíte completa estiver passando;
- os fluxos existentes do WhatsApp continuarem funcionando;
- os critérios específicos do cartão estiverem demonstrados por testes ou evidência verificável.
