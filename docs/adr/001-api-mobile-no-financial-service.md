# ADR 001 — API mobile no financial-service

- Status: aceito
- Data: 2026-09-15

## Contexto

O sistema precisa disponibilizar uma API para um aplicativo mobile. O MVP deve permitir autenticação por código enviado pelo WhatsApp, manutenção da sessão, registro manual de gastos e consulta de relatórios.

O `financial-service` já concentra os usuários, as categorias, as transações, os relatórios e as principais regras financeiras. Atualmente, seus endpoints são usados internamente pelo `ai-service` e pelo `whatsapp-service` e são protegidos pela chave compartilhada `X-Internal-Api-Key`.

Foi necessário decidir entre:

1. adicionar a API mobile ao `financial-service`;
2. criar um novo serviço, como `mobile-api` ou BFF;
3. expor diretamente os endpoints internos existentes.

## Decisão

O MVP da API mobile será implementado no `financial-service`, sob o prefixo `/mobile/v1/**`.

As rotas internas atuais permanecerão sob `/v1/**` e continuarão protegidas por `X-Internal-Api-Key`. A API mobile terá sua própria autenticação, baseada em código de segurança enviado pelo WhatsApp, access token JWT e refresh token persistido e revogável.

Em produção, somente `/mobile/v1/**` deverá ser publicado para o aplicativo. As rotas internas não deverão ser expostas como parte da API pública.

## Fronteiras de segurança

- `/actuator/health`: health check do serviço;
- `/v1/**`: API interna protegida por `X-Internal-Api-Key`;
- `/mobile/v1/auth/codes`: solicitação de código, protegida por limites de uso;
- `/mobile/v1/auth/codes/verify`: confirmação do código e criação da sessão;
- `/mobile/v1/auth/refresh`: renovação por refresh token;
- `/mobile/v1/auth/logout`: revogação da sessão;
- demais rotas em `/mobile/v1/**`: access token JWT obrigatório;
- rotas não previstas: acesso negado.

Nas rotas financeiras, o usuário será identificado exclusivamente pelo claim `sub` do JWT. O aplicativo não poderá informar telefone ou `usuarioId` para escolher o proprietário dos dados.

## Rotas do MVP

### Autenticação

- `POST /mobile/v1/auth/codes`
- `POST /mobile/v1/auth/codes/verify`
- `POST /mobile/v1/auth/refresh`
- `POST /mobile/v1/auth/logout`

### Funcionalidades financeiras

- `GET /mobile/v1/categorias`
- `GET /mobile/v1/transacoes`
- `POST /mobile/v1/transacoes`
- `GET /mobile/v1/relatorios/resumo`

`GET /mobile/v1/transacoes` atenderá às dez transações recentes da tela inicial, à listagem dos gastos de um período e ao detalhamento dos gastos de uma categoria.

`GET /mobile/v1/relatorios/resumo` utilizará os últimos 30 dias quando nenhuma data for informada. Com `dataInicio` e `dataFim`, utilizará o período solicitado e retornará os identificadores das categorias necessários para abrir o detalhamento.

## Alternativas consideradas

### Criar um mobile-api ou BFF

Essa alternativa oferece uma separação mais clara entre a interface mobile e as regras financeiras. Entretanto, no MVP ela adicionaria um quarto serviço, novas chamadas de rede, duplicação de contratos e mais infraestrutura antes da validação da solução.

Ela poderá ser reconsiderada caso a API cresça, passe a atender outros clientes ou exija uma camada de composição independente.

### Expor os endpoints internos existentes

Essa alternativa foi descartada. Os endpoints atuais usam uma chave interna compartilhada e recebem o telefone informado pelo cliente. Esse modelo não é adequado para autenticar usuários finais nem para garantir que cada usuário acesse somente os próprios dados.

## Consequências positivas

- reaproveitamento das regras de negócio e dos repositórios existentes;
- manutenção de Java, Spring Boot, PostgreSQL e Flyway;
- menor custo de infraestrutura e operação durante o MVP;
- foco do aprendizado em HTTP, autenticação, autorização e sessões;
- possibilidade de preservar o contrato `/mobile/v1` em uma extração futura.

## Consequências negativas

- o `financial-service` passará a atender clientes internos e o aplicativo mobile;
- será necessário manter cadeias de segurança distintas no mesmo serviço;
- alterações de segurança exigirão testes de regressão dos fluxos internos;
- o serviço assumirá temporariamente responsabilidades de autenticação do usuário.

## Fora do MVP

- edição e cancelamento de transações pelo aplicativo;
- criação, edição e desativação de categorias;
- gerenciamento de múltiplos aparelhos e logout global;
- perfil editável e notificações push;
- receitas, metas, orçamentos e dashboards avançados;
- extração de gastos por IA dentro do aplicativo.

## Evolução futura

A criação de um `mobile-api` ou BFF deverá ser reavaliada quando houver necessidade de escalar a API independentemente, compor dados de vários serviços, atender diferentes tipos de cliente ou isolar a autenticação em um componente próprio.

Se isso acontecer, o contrato público `/mobile/v1/**` deverá ser preservado sempre que possível para evitar alterações desnecessárias no aplicativo.
