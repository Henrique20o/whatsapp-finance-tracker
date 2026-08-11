# WhatsApp Finance Tracker

Sistema de controle financeiro pessoal integrado ao WhatsApp, desenvolvido para reduzir a dificuldade de registrar gastos no dia a dia.

O usuário envia uma mensagem em linguagem natural, como `Almocei no restaurante e gastei 45 reais`, e a aplicação utiliza inteligência artificial para identificar o valor, gerar uma descrição e classificar a despesa de acordo com as categorias cadastradas. A transação é armazenada no PostgreSQL e uma confirmação é enviada ao usuário pelo próprio WhatsApp.

Este projeto está em desenvolvimento e tem como objetivo explorar microsserviços, arquitetura orientada a eventos, integração com modelos de IA e processamento resiliente de mensagens.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Tecnologias e ferramentas](#tecnologias-e-ferramentas)
- [Instruções para execução](#instruções-para-execução)
- [Configuração do WuzAPI](#configuração-do-wuzapi)
- [Portas dos serviços](#portas-dos-serviços)
- [Endpoints disponíveis](#endpoints-disponíveis)
- [Mensageria e resiliência](#mensageria-e-resiliência)
- [Próximas etapas](#próximas-etapas)

---

## Funcionalidades

- Registro de despesas por mensagens de texto no WhatsApp.
- Cadastro automático do usuário a partir do telefone.
- Extração de valor, descrição e categoria com IA.
- Categorias padrão e personalizadas por usuário.
- Classificação baseada exclusivamente nas categorias ativas do usuário.
- Persistência das transações no PostgreSQL.
- Confirmação do lançamento pelo WhatsApp.
- Proteção contra transações duplicadas usando o ID original da mensagem.
- Retry limitado e fila de mensagens com falha (DLQ).
- Execução completa do ambiente com Docker Compose.

---

## Arquitetura

O projeto utiliza três microsserviços Spring Boot:

- **`whatsapp-service`**: recebe webhooks e envia respostas por meio do WuzAPI.
- **`ai-service`**: interpreta a mensagem, consulta as categorias do usuário e extrai os dados financeiros com IA.
- **`financial-service`**: concentra as regras de negócio e persiste usuários, categorias e transações.

Fluxo principal:

```text
WhatsApp
   ↕
WuzAPI
   ↓ webhook
whatsapp-service
   ↓ RabbitMQ: whatsapp-entrada
ai-service
   ├── consulta categorias no financial-service
   └── extrai a transação com OpenAI
   ↓ RabbitMQ: transacoes-entrada
financial-service
   ├── aplica regras de negócio
   ├── persiste no PostgreSQL
   └── publica a confirmação
   ↓ RabbitMQ: whatsapp-saida
whatsapp-service
   ↓
WuzAPI → WhatsApp
```

---

## Tecnologias e ferramentas

- **Linguagem:** Java 21
- **Framework:** Spring Boot 4.1
- **Inteligência artificial:** Spring AI e OpenAI
- **Banco de dados:** PostgreSQL 15
- **Mensageria:** RabbitMQ e Spring AMQP
- **Integração com WhatsApp:** WuzAPI
- **Cliente HTTP:** Spring RestClient
- **Containers:** Docker e Docker Compose
- **Mapeamento e persistência:** Spring Data JPA e Hibernate
- **Utilitários:** Lombok e Bean Validation

---

## Instruções para execução

### Pré-requisitos

- Docker Desktop instalado e em execução.
- Git instalado.
- Uma chave de API da OpenAI.
- Uma conta do WhatsApp disponível para conexão com o WuzAPI.

### 1. Clone o repositório

```bash
git clone https://github.com/Henrique20o/whatsapp-finance-tracker.git
cd whatsapp-finance-tracker
```

### 2. Configure as variáveis de ambiente

Crie o arquivo `.env` a partir do exemplo:

No Prompt de Comando do Windows:

```cmd
copy .env.example .env
```

No PowerShell, Linux ou macOS:

```bash
cp .env.example .env
```

Preencha o `.env` com valores próprios:

```dotenv
DB_USER=postgres
DB_PASS=uma_senha_segura

RABBITMQ_USER=admin
RABBITMQ_PASS=uma_senha_segura

WUZAPI_ADMIN_TOKEN=um_token_administrativo_seguro
AUTHENTICATION_API_KEY=token_da_instancia_wuzapi
WUZAPI_BASE_URL=http://wuzapi:8080

OPENAI_API_KEY=sua_chave_openai
```

> Nunca envie o arquivo `.env` para o Git. Ele já está incluído no `.gitignore`.

### 3. Suba os containers

```bash
docker compose up -d --build
```

### 4. Verifique os serviços

```bash
docker compose ps
```

Os serviços Spring podem ser verificados pelos endpoints:

```text
http://localhost:8082/actuator/health
http://localhost:8083/actuator/health
http://localhost:8084/actuator/health
```

### 5. Acompanhe os logs

```bash
docker compose logs -f whatsapp-service ai-service financial-service
```

Para encerrar o ambiente sem apagar os dados:

```bash
docker compose down
```

---

## Configuração do WuzAPI

Com os containers ativos, abra o dashboard:

[http://localhost:8080/dashboard](http://localhost:8080/dashboard)

Crie uma instância, conecte o WhatsApp pelo QR Code e configure:

```text
Webhook: http://whatsapp-service:8080/v1/webhook/wuzapi
Evento: Message
Formato: JSON
```

O endereço acima utiliza o nome e a porta internos do container. Não use `localhost:8082` como webhook dentro do WuzAPI, pois `localhost` apontaria para o próprio container.

O token da instância deve ser o mesmo configurado em `AUTHENTICATION_API_KEY` no `.env`.

---

## Portas dos serviços

| Serviço | URL local | Finalidade |
| --- | --- | --- |
| WuzAPI | `http://localhost:8080` | Dashboard e integração com WhatsApp |
| WhatsApp Service | `http://localhost:8082` | Recebimento de webhooks |
| AI Service | `http://localhost:8083` | Processamento interno de IA |
| Financial Service | `http://localhost:8084` | API e regras financeiras |
| PostgreSQL | `localhost:5433` | Banco de dados |
| RabbitMQ | `localhost:5672` | Protocolo AMQP |
| RabbitMQ Management | `http://localhost:15672` | Painel administrativo do broker |

Todos os serviços Spring utilizam a porta `8080` internamente na rede Docker.

---

## Endpoints disponíveis

### WhatsApp Service

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/v1/webhook/wuzapi` | Recebe eventos JSON enviados pelo WuzAPI |

### Financial Service

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/v1/categorias?telefone={telefone}` | Lista as categorias ativas do usuário |
| POST | `/v1/transacoes` | Registra uma transação diretamente pela API |

Exemplo de transação:

```json
{
  "messageId": "identificador-unico-opcional",
  "telefone": "5531999998888",
  "valor": 45.00,
  "descricao": "Almoço no restaurante",
  "categoriaNome": "Alimentação"
}
```

---

## Mensageria e resiliência

| Fila | Responsabilidade |
| --- | --- |
| `financeiro.v1.whatsapp-entrada` | Mensagens recebidas do WhatsApp |
| `financeiro.v1.transacoes-entrada` | Transações extraídas pela IA |
| `financeiro.v1.whatsapp-saida` | Respostas que serão enviadas ao usuário |
| `financeiro.v1.whatsapp-entrada.dlq` | Mensagens que falharam após as tentativas |

O consumidor do `ai-service` executa uma tentativa inicial e duas novas tentativas com backoff. Caso a falha continue, a mensagem é enviada para a DLQ em vez de retornar infinitamente para a fila principal.

O ID fornecido pelo WuzAPI também é armazenado como identificador externo único, evitando que uma mensagem repetida crie mais de uma transação.

---

## Próximas etapas

- Testes unitários e de integração com PostgreSQL e RabbitMQ.
- Validação programática da categoria retornada pela IA.
- Migrations de banco de dados com Flyway.
- Processamento de mensagens de áudio.
- Comandos para consultar e cancelar lançamentos.
- Extrato paginado e métricas financeiras.
- Dashboard web em Angular.
- Observabilidade, autenticação e preparação para produção.

---

## Autor

Desenvolvido por [Henrique](https://github.com/Henrique20o).
