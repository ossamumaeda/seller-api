# Budget Control

Take-home desenvolvido para gerenciamento de verba promocional de vendedores.

## Tecnologias

* Java 21
* Spring Boot 3
* PostgreSQL
* Flyway
* Docker Compose
* Maven

---

# Como executar

## 1. Clone o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd budget-control
```

## 2. Suba o banco de dados

```bash
docker compose up -d
```

O PostgreSQL será iniciado com o banco configurado para a aplicação.

---

## 3. Execute a aplicação

Linux / macOS

```bash
./mvnw spring-boot:run
```

Windows

```bash
mvnw.cmd spring-boot:run
```

Durante a inicialização:

* o Flyway executará automaticamente as migrations;
* os dados iniciais (seed) serão carregados;
* a API ficará disponível em:

```
http://localhost:8080
```

---

# Banco de dados

Configuração padrão utilizada:

| Propriedade | Valor          |
| ----------- | -------------- |
| Banco       | PostgreSQL     |
| Porta       | 5437           |
| Database    | budget_control |
| Usuário     | postgres       |
| Senha       | postgres       |

---

# Estrutura do projeto

```
src
 ├── domain
 ├── repository
 ├── usecase
 ├── controller
 ├── exception
 ├── config
 └── resources
      ├── db
      │    ├── migration
      │    └── seed
      └── application.yml
```

---

# Funcionalidades implementadas

* Fechamento de pedidos com consumo de verba
* Validação de saldo disponível
* Estorno de verba no cancelamento
* Idempotência no fechamento e cancelamento
* Controle de concorrência utilizando lock pessimista
* Histórico de movimentações financeiras

---

# Principais decisões

As principais decisões de arquitetura, trade-offs e limitações da solução estão documentadas em:

```
DECISIONS.md
```
