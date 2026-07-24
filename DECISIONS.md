# DECISIONS.md

## Decisões de modelagem

### Entidades como responsáveis pelas regras de negócio

As regras de domínio foram concentradas nas entidades, enquanto os Use Cases atuam apenas como orquestradores.

Exemplos:

* `Budget.consume()` valida saldo suficiente antes de consumir a verba.
* `Budget.refund()` realiza o estorno da verba.
* `SalesOrder.close()` e `SalesOrder.cancel()` controlam a mudança de estado do pedido.
* `BudgetMovement` possui métodos fábrica para criação dos movimentos de consumo e estorno.

Essa abordagem evita que regras fiquem espalhadas pela camada de aplicação e mantém as invariantes próximas dos dados que elas protegem.

---

### Casos de uso enxutos

Os Use Cases possuem apenas a responsabilidade de coordenar o fluxo da operação:

* recuperar entidades;
* executar regras de domínio;
* persistir novos movimentos;
* propagar exceções de negócio.

Toda regra que altera o estado de uma entidade permanece encapsulada no próprio domínio.

---

### Movimentos financeiros como histórico imutável

Ao invés de armazenar apenas o saldo atual da verba, foi criada a entidade `BudgetMovement`.

Ela registra cada consumo e cada estorno realizado.

Essa decisão simplificou a implementação do AC-03, pois o cancelamento sempre consegue identificar exatamente qual verba foi utilizada originalmente, independentemente da competência atual.

---

### Concorrência protegendo o recurso compartilhado

O controle de concorrência foi aplicado sobre a entidade `Budget`, pois ela representa o recurso compartilhado entre múltiplos pedidos.

Foi adotado lock pessimista (`PESSIMISTIC_WRITE`) durante o carregamento da verba.

Essa estratégia garante que apenas uma transação altere o saldo de uma determinada verba por vez.

Além do lock, a solução utiliza transações e constraints do banco de dados para fornecer múltiplas camadas de proteção contra inconsistências.

---

### Tratamento de erros

As regras de domínio lançam exceções específicas (`InsufficientBudgetException`, `BudgetNotFoundException`, etc.).

A tradução dessas exceções para respostas HTTP fica centralizada em um `GlobalExceptionHandler`.

Dessa forma, o domínio permanece desacoplado da camada web.

---

## Decisões pragmáticas devido ao timebox

Algumas decisões foram tomadas para manter a implementação simples dentro do tempo disponível.

* Utilização de PostgreSQL com Flyway, evitando preocupações de infraestrutura mais complexas.
* Seeds para criação dos dados iniciais, sem implementação de telas ou APIs de cadastro.
* Competência obtida através de `LocalDate.now()` durante o fechamento do pedido, em vez de uma abstração específica para calendário de negócio.
* Ausência de autenticação e autorização, conforme permitido pelo enunciado.
* Implementação focada apenas nos fluxos exigidos pelos Acceptance Criteria.

---

## O que faria com mais tempo

* Implementaria testes de integração para validar cenários reais de concorrência.
* Criaria eventos de domínio para desacoplar alterações de pedido e movimentações financeiras.
* Introduziria uma abstração para cálculo da competência, evitando dependência direta do relógio da aplicação.
* Melhoraria a cobertura de testes unitários das entidades para validar invariantes do domínio.
* Implementaria observabilidade (logs estruturados e métricas) para facilitar diagnóstico em produção.

---

## Parte mais frágil da solução

O ponto mais frágil da implementação está relacionado ao lock pessimista.

Embora ele resolva corretamente o cenário proposto pelo desafio, ele reduz o paralelismo quando múltiplas operações disputam a mesma verba.

Em um sistema com alto volume de transações concorrentes sobre um mesmo vendedor, isso poderia aumentar o tempo de espera entre requisições.

Outra limitação é a dependência da competência atual (`LocalDate.now()`) durante o fechamento do pedido. Caso existam regras de fechamento retroativo ou antecipado, essa lógica provavelmente precisaria ser extraída para um componente específico do domínio.

---

## Como a solução poderia evoluir

Caso o sistema crescesse, avaliaria:

* lock otimista com política de retry;
* versionamento das entidades (`@Version`);
* eventos de domínio para integração com outros serviços;
* auditoria completa das movimentações;
* testes automatizados para cenários concorrentes.

Essas melhorias aumentariam a escalabilidade da solução sem alterar significativamente a modelagem atual.

---

## Uso de IA durante o desenvolvimento

Ferramentas de IA foram utilizadas como aceleradoras de implementação, principalmente para:

* geração de código repetitivo;
* revisão de modelagem;
* discussão de alternativas para concorrência e idempotência;
* apoio na documentação das decisões de arquitetura.

Todas as sugestões foram revisadas antes de serem incorporadas.

Em alguns momentos foi necessário corrigir ou adaptar propostas da IA. Os principais ajustes foram:

* mover regras de negócio da camada de aplicação para as entidades de domínio;
* alterar a ordem da verificação de idempotência após a introdução do lock pessimista, eliminando uma janela de concorrência;
* aplicar a mesma estratégia de sincronização também ao cancelamento de pedidos, garantindo consistência em qualquer operação que altere o saldo da verba.

A IA foi utilizada como ferramenta de produtividade, enquanto as decisões finais de modelagem e os trade-offs adotados foram validados durante o desenvolvimento.

## Teste de concorrência

O teste de concorrência não assume qual requisição adquire o lock primeiro, pois a ordem de execução das threads não é determinística.

O teste valida as invariantes do domínio:

- apenas um pedido é fechado;
- apenas um movimento de consumo é criado;
- o saldo nunca fica negativo;
- o saldo final corresponde ao consumo realizado.

A garantia de consistência vem do lock pessimista aplicado na busca da verba.

## Estratégia de testes

A cobertura de testes foi direcionada principalmente para regras de negócio e cenários onde inconsistências financeiras poderiam ocorrer.

Foram priorizados:

- consumo de verba;
- rejeição por saldo insuficiente;
- estornos;
- idempotência;
- concorrência;
- contratos HTTP da API.

A intenção não foi buscar cobertura total de código, mas garantir que os invariantes importantes do domínio permanecessem protegidos.

### Testes de concorrência

O teste de concorrência não assume qual requisição será processada primeiro, pois a ordem de execução das threads não é determinística.

O cenário valida as invariantes do negócio:

- somente um pedido pode ser fechado quando a verba disponível não suporta ambos os consumos;
- somente um movimento de consumo deve ser criado;
- o saldo nunca pode ficar negativo;
- o saldo final deve ser consistente com o movimento registrado.

A proteção contra concorrência é realizada através de lock pessimista na entidade `Budget`, garantindo que duas transações não consigam consumir a mesma verba simultaneamente.

### Defesa em camadas

Além da validação na aplicação através da verificação de movimentos existentes, algumas regras críticas também possuem proteção no banco de dados.

Exemplo:

- A aplicação verifica se já existe um movimento de consumo para evitar duplicidade em cenários normais.
- O banco possui uma constraint `UNIQUE(order_id, movement_type)` como última barreira contra condições de corrida.

Dessa forma, mesmo em um cenário onde duas requisições passam simultaneamente pela validação da aplicação, o banco mantém a consistência dos dados.

### Testes de integração

Foram adicionados testes utilizando o contexto real do Spring para validar:

- persistência dos movimentos;
- comportamento das constraints do banco;
- integração entre entidades e repositories.

Esses testes complementam os testes unitários dos casos de uso, garantindo que as regras importantes estejam protegidas tanto no domínio quanto na camada de persistência.


# Dashboard (AC-06)

## Objetivo

O requisito do painel do coordenador foi tratado como um caso de uso de consulta (read model), separado do modelo de domínio utilizado pelas operações financeiras.

O objetivo foi fornecer ao frontend exatamente as informações necessárias para a tela, sem expor entidades JPA ou detalhes da estrutura de persistência.

---

## Endpoint específico para o dashboard

Foi criado um endpoint dedicado:

```text
GET /api/dashboard/budget
```

Ao invés de reutilizar endpoints de `Seller` ou `Budget`, foi criada uma projeção específica para o painel.

Essa decisão reduz o acoplamento entre frontend e backend e permite que a estrutura da tela evolua sem impactar o modelo de domínio.

---

## Agrupamento por competência

Em vez de limitar o endpoint apenas à competência atual ou exigir um filtro por parâmetro, o endpoint retorna todas as competências cadastradas agrupadas.

Essa abordagem foi escolhida por três motivos:

* simplifica o contrato da API;
* permite que o frontend navegue entre competências sem novas requisições;
* mantém o backend responsável apenas por organizar os dados de negócio.

A responsabilidade de escolher qual competência exibir ficou com o frontend.

---

## Indicadores calculados no backend

O backend envia informações já consolidadas para a interface.

Para cada competência são calculados:

* limite total da equipe;
* saldo total disponível;
* percentual de utilização da verba;
* quantidade de vendedores em situação crítica;
* lista de vendedores.

Esses valores representam regras de negócio e não apenas transformações de apresentação.

---

## Saúde da verba

Foi criado o conceito de saúde da verba (`BudgetHealthStatus`), representado pelos estados:

* HEALTHY
* WARNING
* CRITICAL

A classificação é baseada no percentual de utilização da verba.

Centralizar essa regra no backend evita duplicação de lógica entre diferentes clientes e garante que todos utilizem os mesmos critérios.

Caso os limites mudem futuramente, apenas o backend precisa ser alterado.

---

## DTOs específicos

Foram criados DTOs exclusivos para o dashboard:

* `BudgetDashboardResponse`
* `CompetenceBudgetResponse`
* `SellerBudgetSummaryResponse`

A API não retorna entidades JPA diretamente.

Essa decisão reduz o acoplamento entre a camada de persistência e a interface e evita que alterações internas do domínio afetem consumidores da API.

---

## Agrupamento em memória

O agrupamento por competência é realizado na camada de aplicação utilizando Streams do Java.

Apesar de ser possível executar agregações diretamente no banco de dados, essa abordagem foi considerada suficiente para o escopo do desafio.

As vantagens são:

* implementação mais simples;
* maior legibilidade;
* menor complexidade de consultas SQL.

Em um cenário com grande volume de dados, uma evolução natural seria mover parte dessas agregações para consultas específicas utilizando projeções ou funções de agregação do banco.

---

## Responsabilidades

O caso de uso `GetBudgetDashboardUseCase` é responsável apenas por montar a visão do painel.

O fluxo segue o mesmo padrão utilizado em todo o projeto:

```
Controller
    ↓
UseCase
    ↓
Repository
    ↓
DTO
```

Dessa forma, o controller permanece fino, o caso de uso concentra a lógica de composição dos dados e o frontend recebe uma estrutura pronta para consumo.

---

## Trade-offs

A solução prioriza simplicidade e clareza.

Retornar todas as competências em uma única resposta é adequado para o tamanho esperado do desafio, mas pode não escalar para ambientes com muitos anos de histórico ou milhares de vendedores.

Nesse cenário, uma evolução natural seria:

* paginação das competências;
* filtro por período;
* consultas agregadas específicas para dashboard;
* cache dos indicadores consolidados.

Essas otimizações foram consideradas desnecessárias para o escopo e o timebox do teste.
