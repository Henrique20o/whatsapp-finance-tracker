# Runbook de implantação da API mobile

Este runbook prepara a operação do `financial-service` e complementa [configuração JWT](../security/mobile-jwt-configuration.md) e [contrato OpenAPI](../openapi/mobile-api.yml). Nenhuma chave real deve ser gravada neste repositório.

## Rede e publicação

- Coloque o `financial-service` em rede privada; o Docker Compose já usa `expose` sem publicar a porta do serviço no host.
- No gateway TLS confiável, encaminhe apenas `/mobile/v1/**` e o health check necessário. Negue o restante, incluindo `/v1/**`, actuator sensível, banco e RabbitMQ.
- Termine HTTPS no gateway e sobrescreva `X-Forwarded-For` com o endereço remoto observado pelo próprio proxy. Não encaminhe a lista arbitrária fornecida pelo cliente.
- Mantenha as filas e os serviços internos em rede privada. Não publique management do RabbitMQ nem porta do PostgreSQL.

## Segredos e inicialização

Injete chave privada JWT, chave pública, chave de HMAC, chave de proteção do telefone, credenciais de banco/mensageria e chave interna por secret manager. Restrinja leitura ao processo. Use `MOBILE_JWT_ACTIVE_KID` e preserve as chaves públicas antigas durante a janela de validade dos tokens.

A aplicação falha fechada quando o par RSA ativo é inválido, a chave interna ou segredos obrigatórios estão ausentes, ou a configuração de prazo é incoerente. Não inclua valores reais em logs, imagem Docker ou arquivo `.env` versionado.

## Banco e rollout

1. Faça backup consistente do PostgreSQL e registre a versão implantada.
2. Aplique as migrations Flyway em ordem (V4–V6 acrescentam origem/idempotência, ID público e gestão de categorias).
3. Confirme no log de inicialização que todas as migrations terminaram antes de liberar tráfego.
4. Implante o serviço com a API mobile habilitada e depois publique a rota no gateway.
5. Se a implantação falhar, retire a rota pública e restaure a versão anterior do serviço. As migrations são aditivas; restauração de dados deve usar o backup, não apagar colunas ou tabelas manualmente.

O backfill de origem marca lançamentos legados como `WHATSAPP`. A normalização de categorias preserva as transações e desativa duplicatas ativas determinísticas antes de criar o índice de unicidade.

## Verificações após implantação

1. Health check retorna sucesso sem detalhes internos.
2. `/v1/**` e endpoints actuator sensíveis não são acessíveis pela internet; rotas fora da lista permitida retornam 404/403 do gateway.
3. Solicite código de telefone de teste, confirme uma vez e valide `200`, `Cache-Control: no-store` e par de tokens.
4. Consulte categorias, crie gasto com chave UUID, repita a mesma requisição e confirme que existe um único lançamento e nenhuma confirmação inesperada por WhatsApp.
5. Compare resumo e série diária à listagem paginada do mesmo usuário e período.
6. Renove refresh, tente reutilizar o antigo, confirme a revogação da família, faça login novamente e teste logout.
7. Repita leituras com outro usuário de teste; não deve haver dados cruzados.
8. Inspecione logs e métricas: devem conter correlação e resultado, sem token, código, chave, telefone completo, descrição ou chave de idempotência.

Registre data, commit, ambiente e resultados da homologação em um sistema operacional aprovado. Não declare produção pronta sem jornada HTTPS real, segundo usuário para isolamento e plano de rollback exercitado.

## Retenção e resposta a incidentes

O job executa a cada hora em lotes de até 500 itens. Desafios expirados e idempotências são removidos após 30 dias. Famílias de sessão são removidas 30 dias após o prazo absoluto, em ordem que respeita a cadeia de refresh. O job registra apenas contagens.

Em suspeita de comprometimento de chave, mude imediatamente o `kid` de emissão, retenha a chave pública suspeita somente pelo tempo necessário para validação operacional e siga o procedimento de rotação. Revogue as famílias afetadas pelo endpoint de gerenciamento; JWTs já emitidos podem permanecer válidos até 15 minutos. Preserve logs de correlation ID e resultados sem copiar credenciais.
