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
