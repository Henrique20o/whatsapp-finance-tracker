# Integração do aplicativo com a API mobile

A API mobile é hospedada pelo `financial-service` sob `/mobile/v1`. Os caminhos e campos deste guia seguem o contrato em [OpenAPI](../openapi/mobile-api.yml).

## Entrar com código do WhatsApp

1. Envie `POST /mobile/v1/auth/codes` com `telefone` no formato internacional somente com dígitos. Guarde `challengeId` e respeite `reenviarEmSegundos`.
2. Envie `POST /mobile/v1/auth/codes/verify` com `challengeId`, o código recebido e, opcionalmente, `nomeDispositivo`.
3. Em sucesso, armazene o `accessToken` em memória e o `refreshToken` no Android Keystore ou iOS Keychain. As duas respostas sensíveis enviam `Cache-Control: no-store`.
4. Envie o access token em `Authorization: Bearer <token>` nas rotas financeiras. Ele dura 15 minutos.

O código expira em cinco minutos e tem uso único. Se a confirmação ou entrega da resposta se perder depois do commit, peça outro código e entre novamente.

## Renovar e sair

Antes de expirar o access token, envie `POST /mobile/v1/auth/refresh` com o refresh token no corpo. Substitua os dois tokens pelo novo par recebido. Serialize as renovações: se o mesmo token já rotacionado for reapresentado, a família inteira é revogada e o app deve pedir um novo código.

Para sair do aparelho atual, envie `POST /mobile/v1/auth/logout` com o refresh token e apague ambos os tokens locais. Para gerenciar outros aparelhos, use `GET /mobile/v1/auth/sessions`, `DELETE /mobile/v1/auth/sessions/{id}` ou `DELETE /mobile/v1/auth/sessions`. Um access token já emitido pode funcionar por até 15 minutos após revogação.

## Gastos, categorias e relatórios

- Carregue categorias ativas com `GET /mobile/v1/categorias`.
- Liste gastos com `GET /mobile/v1/transacoes`; sem datas, use páginas de dez itens. Com `dataInicio` e `dataFim`, envie as duas datas ISO inclusivas.
- Crie cada gasto com `POST /mobile/v1/transacoes` e uma `Idempotency-Key` UUID nova por operação. Em retry, reutilize a mesma chave e o mesmo payload. Payload diferente com a mesma chave resulta em `409`.
- Use `GET /mobile/v1/relatorios/resumo` para o total e os grupos por categoria. Sem datas, o período cobre os últimos 30 dias civis, incluindo hoje.
- Use `GET /mobile/v1/relatorios/por-periodo` com datas obrigatórias para uma série diária com dias sem gasto preenchidos com zero.

Datas são interpretadas em `America/Sao_Paulo`; o intervalo inclui as datas inicial e final. Relatórios e listagens ignoram gastos cancelados. Ao tocar em um total ou categoria, chame a listagem de transações com as mesmas datas e `categoriaId`.

O backend obtém o usuário do JWT. Não envie telefone ou ID de usuário para escolher o proprietário. Categorias desativadas permanecem visíveis em lançamentos antigos, mas não aceitam novos gastos.

## Erros e suporte

Use `code` como identificador de erro, não o texto de `message`. Respeite `Retry-After` em `429`. Envie `X-Correlation-Id` somente se a integração futura adotar um proxy confiável; a API atual gera e devolve um identificador por requisição. Ao abrir suporte, compartilhe esse identificador, sem enviar códigos, tokens, telefone completo ou descrição do gasto.

## Dados no dispositivo

- Access token: memória de processo sempre que possível.
- Refresh token: armazenamento seguro do sistema operacional, nunca em preferências simples, logs ou analytics.
- Logout: apagar os dois tokens locais mesmo quando a chamada à API falhar.
- Chave de idempotência: manter junto da operação pendente até receber resposta conclusiva; não reciclar por outra operação.
