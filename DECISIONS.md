# Decisões de Modelagem

## Entidades

O modelo foi dividido em quatro entidades principais:

- Seller
- Budget
- SalesOrder
- BudgetMovement

A intenção foi manter o domínio simples e aderente aos requisitos do desafio, evitando abstrações desnecessárias para um projeto com timebox de quatro horas.

---

## Budget por competência

A verba foi modelada como uma entidade própria (`Budget`) identificada por:

- vendedor
- competência

Cada vendedor possui exatamente uma verba por competência.

Foi criada uma restrição de unicidade (`seller_id`, `competence`) para garantir essa regra também no banco de dados.

---

## Competência

A competência foi armazenada como `DATE`, utilizando sempre o primeiro dia do mês (ex.: `2026-07-01`).

Essa abordagem facilita ordenação, filtros e comparações utilizando recursos nativos do PostgreSQL, evitando manipulação de strings.

---

## Saldo materializado

O saldo atual permanece armazenado na tabela `budgets`.

Embora fosse possível calcular o saldo somando todos os movimentos, essa abordagem aumentaria o custo de leitura, especialmente para o painel do coordenador.

A consistência do saldo passa a depender das transações da aplicação.

---

## Histórico de movimentos

Cada consumo ou estorno gera um registro em `budget_movements`.

Essa tabela representa o histórico financeiro da verba e permite reconstruir todas as operações realizadas sobre um orçamento.

---

## Estorno

O movimento referencia diretamente o orçamento (`budget`) utilizado no consumo.

Dessa forma, caso um pedido seja cancelado em uma competência futura, o crédito é devolvido exatamente para a verba original, atendendo ao AC-03 sem necessidade de cálculos adicionais.

---

## Idempotência

Foi criada uma restrição de unicidade (`order_id`, `movement_type`) em `budget_movements`.

Isso impede que um mesmo pedido gere dois movimentos do mesmo tipo e adiciona uma camada extra de proteção além da lógica implementada na aplicação.

---

## Integridade dos dados

Algumas regras foram protegidas diretamente pelo banco:

- saldo não pode ser negativo;
- limite da verba não pode ser negativo;
- desconto não pode ser negativo;
- movimentações devem possuir valor positivo;
- tipos de movimentação possuem valores válidos.

Essas restrições evitam inconsistências caso a aplicação falhe ou outro cliente acesse o banco futuramente.

---

## O que ficou para implementação

A estratégia de concorrência (Pessimistic Lock vs Optimistic Lock) será implementada na camada de serviço durante o fluxo de fechamento do pedido.

A decisão foi adiada porque depende da implementação das regras transacionais e não apenas da modelagem do banco.

## Geração de ids
Visando a criação dos dados com um seeder, a remoção da opção: DEFAULT gen_random_uuid() foi feita para facilitar a inserção de dados para teste. Além disso, o spring/hibernate já é responsavel pela criação de UUID, então a opção era redundante.


# Decisões de Implementação — AC-01 (Consumo no fechamento do pedido)

## Estratégia de implementação incremental

A implementação foi iniciada pelo fluxo principal de negócio (happy path) definido no AC-01.

A decisão foi evoluir a solução incrementalmente, adicionando complexidade conforme os requisitos aparecem:

1. AC-01 — Fechamento com saldo suficiente.
2. AC-02 — Tratamento de saldo insuficiente.
3. AC-04 — Idempotência.
4. AC-05 — Concorrência.
5. AC-03 — Cancelamento e estorno.

Essa abordagem evita antecipar soluções complexas antes de existir uma necessidade real, mantendo o código mais simples e permitindo validar cada comportamento isoladamente.

---

# Separação entre Use Case e Entidades

A implementação utiliza um caso de uso específico para representar a operação de negócio:

```
CloseOrderUseCase
```

O fechamento de um pedido não pertence exclusivamente a uma única entidade, pois envolve múltiplos conceitos do domínio:

* Pedido (`SalesOrder`)
* Verba (`Budget`)
* Movimento financeiro (`BudgetMovement`)

Por esse motivo, o caso de uso atua como um orquestrador da operação:

```
Controller
    |
    v
CloseOrderUseCase
    |
    +--> SalesOrder
    |
    +--> Budget
    |
    +--> BudgetMovement
```

O Use Case é responsável por coordenar o fluxo, enquanto cada entidade é responsável por proteger seu próprio estado.

---

# Regras de negócio dentro das entidades

As alterações de estado foram encapsuladas nas entidades de domínio.

Exemplo:

```java
budget.consume(amount);
```

ao invés de:

```java
budget.setBalance(
    budget.getBalance().subtract(amount)
);
```

E:

```java
order.close();
```

ao invés de:

```java
order.setStatus(OrderStatus.CLOSED);
order.setClosedAt(LocalDateTime.now());
```

A decisão foi tomada seguindo o princípio de encapsulamento de domínio.

A entidade deve controlar como seu estado pode ser alterado, evitando que diferentes partes da aplicação manipulem seus atributos diretamente e criem estados inválidos.

Com isso, as regras ficam próximas dos dados que elas protegem.

Exemplo:

* `Budget` sabe como consumir saldo.
* `SalesOrder` sabe como realizar seu fechamento.
* `BudgetMovement` sabe como representar um consumo válido.

O Use Case não conhece os detalhes internos dessas operações, apenas solicita que elas aconteçam.

---

# Relação com DDD

A implementação segue alguns conceitos de Domain Driven Design sem aplicar uma arquitetura complexa.

## Entidades com comportamento

As entidades não são apenas estruturas de dados anêmicas.

Elas possuem comportamento relacionado ao seu próprio ciclo de vida.

Exemplo:

```java
budget.consume(order.getDiscount());
```

representa uma ação do domínio:

"consumir verba promocional"

em vez de uma simples alteração de atributo.

---

## Use Cases como camada de aplicação

O `CloseOrderUseCase` representa uma operação do sistema.

Ele não contém regras internas de cada entidade, mas coordena a interação entre elas.

Seu papel é:

* buscar os objetos necessários;
* executar as operações de domínio;
* persistir novos objetos;
* garantir a transação da operação.

---

# Uso de transação

O fechamento do pedido é uma operação atômica.

Ela envolve três alterações:

1. Atualização do saldo da verba.
2. Criação do movimento de consumo.
3. Atualização do status do pedido.

Por isso, o caso de uso utiliza:

```java
@Transactional
```

Garantindo que:

* ou todas as alterações são persistidas;
* ou nenhuma alteração é aplicada.

Isso evita cenários como:

* pedido fechado sem desconto debitado;
* verba consumida sem movimento registrado;
* movimento registrado sem pedido fechado.

---

# Uso de Dirty Checking do JPA

As entidades `Budget` e `SalesOrder` são carregadas através dos repositories dentro da transação.

Após o carregamento, elas ficam no estado `Managed` do Hibernate.

Por isso, alterações como:

```java
budget.consume(amount);

order.close();
```

não precisam de chamadas explícitas:

```java
budgetRepository.save(budget);

orderRepository.save(order);
```

O Hibernate utiliza o mecanismo de Dirty Checking para detectar alterações nas entidades gerenciadas e executar os `UPDATEs` necessários durante o flush da transação.

---

# Persistência do movimento

Diferentemente de `Budget` e `SalesOrder`, o `BudgetMovement` é uma nova entidade criada durante a operação.

Ela inicia como um objeto Java comum e ainda não existe no contexto de persistência.

Por isso é necessário:

```java
movementRepository.save(movement);
```

Esse comando informa ao JPA que uma nova entidade deve ser persistida.

---

# Criação de movimentos através de métodos de fábrica

O movimento de consumo foi criado utilizando um método de criação específico:

```java
BudgetMovement.consumption(
    budget,
    order
);
```

Ao invés de permitir a criação manual com diversos setters.

A decisão reduz a possibilidade de criar movimentos inconsistentes, como:

* movimento de consumo com tipo incorreto;
* valor negativo;
* vínculo incorreto com pedido ou verba.

A criação passa a representar uma intenção de negócio.

---

# Endpoint como ação de domínio

O fechamento foi exposto como:

```
POST /api/orders/{id}/close
```

A escolha de utilizar uma ação explícita ao invés de um PATCH genérico foi intencional.

O fechamento de um pedido não é apenas uma alteração de status, pois gera efeitos colaterais:

* consumo de verba;
* registro financeiro;
* mudança do ciclo de vida do pedido.

Representar como uma ação deixa a API mais expressiva.

---

# Trade-offs desta implementação

## O que foi feito

* Fluxo principal de fechamento implementado.
* Regras básicas encapsuladas nas entidades.
* Operação protegida por transação.
* Novo movimento persistido explicitamente.
* Responsabilidades separadas entre controller, use case e domínio.

## O que ficou para evoluções futuras

### AC-02 — Saldo insuficiente

Adicionar uma regra dentro de `Budget.consume()`:

```java
if (balance < amount) {
    throw InsufficientBudgetException;
}
```

e tratar a exceção na camada HTTP.

---

### AC-04 — Idempotência

Adicionar proteção para evitar múltiplos consumos do mesmo pedido.

Possíveis estratégias:

* verificar existência de movimento antes do consumo;
* utilizar constraint única no banco;
* tornar a operação segura para múltiplas chamadas.

---

### AC-05 — Concorrência

Adicionar controle de concorrência no carregamento da verba.

Exemplo:

* lock pessimista (`PESSIMISTIC_WRITE`);
* ou controle otimista através de versão da entidade.

---

# Resumo da decisão

A implementação priorizou um modelo simples, mas com responsabilidades bem definidas:

* Controllers recebem requisições HTTP.
* Use Cases orquestram operações do sistema.
* Entidades protegem suas próprias regras.
* JPA gerencia alterações de entidades carregadas através de Dirty Checking.
* Transações garantem consistência entre múltiplas alterações.

A solução evita complexidade prematura, mas mantém uma estrutura preparada para evoluir conforme os próximos requisitos do domínio forem adicionados.


# Decisões de Implementação — AC-02 (Verba insuficiente)

## Evolução incremental da solução

O AC-02 foi implementado como uma evolução direta do AC-01.

A arquitetura construída anteriormente foi mantida, adicionando apenas os comportamentos necessários para suportar a nova regra de negócio.

Essa decisão foi intencional: quando uma nova regra apareceu, a expectativa era que a solução pudesse evoluir sem exigir alterações significativas na estrutura da aplicação.

O fato de o `CloseOrderUseCase` permanecer praticamente inalterado indicou que as responsabilidades estavam bem distribuídas.

---

# A regra pertence ao domínio

O novo requisito estabelece uma invariável do domínio:

> A verba promocional nunca pode ficar negativa.

Por esse motivo, a validação foi implementada dentro da entidade `Budget`, responsável por proteger seu próprio estado.

```java
public void consume(BigDecimal amount) {

    if (balance.compareTo(amount) < 0) {
        throw new InsufficientBudgetException(...);
    }

    balance = balance.subtract(amount);
}
```

O caso de uso continua apenas expressando a intenção:

```java
budget.consume(order.getDiscount());
```

Sem conhecer detalhes de como a entidade valida ou altera seu estado.

Essa decisão reduz o acoplamento entre a camada de aplicação e as regras do domínio.

---

# Exceções específicas de domínio

Foram criadas exceções específicas para representar erros de negócio.

Exemplos:

* `InsufficientBudgetException`
* `OrderNotFoundException`
* `BudgetNotFoundException`

Ao invés de utilizar exceções genéricas do Java (`IllegalArgumentException`, `NoSuchElementException`, etc.), cada situação relevante do domínio passou a possuir um tipo próprio.

Isso melhora:

* legibilidade;
* rastreabilidade dos erros;
* tratamento centralizado;
* evolução futura da aplicação.

Além disso, as exceções permanecem independentes de HTTP ou do Spring Framework.

Elas representam apenas regras do domínio.

---

# Tratamento centralizado de erros HTTP

A tradução entre exceções de domínio e respostas HTTP foi concentrada em um `@RestControllerAdvice`.

Essa decisão separa completamente:

* regras de negócio;
* protocolo HTTP.

O domínio apenas lança exceções.

A camada HTTP decide como essas exceções serão expostas ao consumidor da API.

Exemplo:

```text
InsufficientBudgetException
            │
            ▼
GlobalExceptionHandler
            │
            ▼
409 Conflict
```

Essa abordagem evita que casos de uso conheçam códigos HTTP ou estruturas de resposta da API.

---

# Padronização das respostas de erro

Foi criado um DTO simples (`ApiError`) para representar todas as respostas de erro.

Estrutura:

```json
{
  "code": "INSUFFICIENT_BUDGET",
  "message": "Available balance is lower than requested amount."
}
```

Além disso, foi introduzido um `ErrorCode` para evitar códigos literais espalhados pela aplicação.

Essa decisão facilita:

* padronização das respostas;
* consumo da API pelo frontend;
* futura internacionalização das mensagens.

---

# Escolha do HTTP 409 (Conflict)

O cenário de verba insuficiente retorna:

```text
409 Conflict
```

A requisição possui sintaxe válida e o recurso existe.

Entretanto, o estado atual da aplicação impede que a operação seja realizada.

Esse comportamento representa melhor um conflito de estado do recurso do que erros de validação (400) ou erros inesperados (500).

---

# Rollback automático da transação

O `CloseOrderUseCase` permanece anotado com:

```java
@Transactional
```

Quando `Budget.consume()` lança `InsufficientBudgetException`, a execução do caso de uso é interrompida imediatamente.

Como consequência:

* nenhum movimento financeiro é criado;
* o pedido permanece aberto;
* nenhuma alteração de saldo é persistida.

O rollback ocorre automaticamente através do gerenciamento transacional do Spring.

Essa característica atende integralmente ao Acceptance Criteria sem necessidade de código adicional.

---

# Evolução preparada para os próximos critérios

A estrutura criada nesta etapa facilita a implementação dos próximos Acceptance Criteria.

Novas exceções de domínio poderão ser adicionadas e tratadas pelo mesmo mecanismo centralizado, como por exemplo:

* pedido já fechado;
* requisição duplicada;
* concorrência;
* operação inválida.

Da mesma forma, novas regras poderão ser incorporadas às entidades sem aumentar a complexidade dos casos de uso.

---

# Resumo da decisão

A implementação do AC-02 reforçou a separação de responsabilidades definida no AC-01.

* O domínio protege suas invariantes através das entidades.
* Os casos de uso permanecem responsáveis apenas por orquestrar operações.
* A camada HTTP traduz exceções de domínio em respostas apropriadas.
* O controle transacional garante consistência dos dados sem necessidade de tratamento manual de rollback.

Como resultado, uma nova regra de negócio foi adicionada sem alterações significativas na estrutura da aplicação, indicando que o modelo está preparado para evoluir conforme os próximos requisitos do desafio.

# Decisões de Implementação — AC-04 (Idempotência)

## Objetivo

O AC-04 estabelece que uma mesma requisição de fechamento pode ser recebida mais de uma vez, mas o desconto deve ser consumido apenas uma única vez.

O objetivo desta etapa foi tornar a operação **idempotente**, ou seja, permitir que a mesma requisição seja executada repetidamente sem alterar o resultado final após a primeira execução bem-sucedida.

---

# Estratégia adotada

A implementação verifica, no início do `CloseOrderUseCase`, se já existe um movimento de consumo associado ao pedido.

```text
Buscar pedido
        │
        ▼
Já existe movimento de consumo?
        │
   ┌────┴────┐
   │         │
 Sim        Não
   │         │
 return   Continua o fluxo
```

Caso o movimento já exista, o caso de uso simplesmente retorna sem executar nenhuma nova operação.

Com isso:

* a verba não é debitada novamente;
* nenhum novo movimento é criado;
* o pedido permanece no mesmo estado.

A operação passa a ser naturalmente idempotente para chamadas repetidas.

---

# Por que verificar o movimento e não o status do pedido?

Uma possibilidade seria utilizar:

```java
if (order.isClosed()) {
    return;
}
```

Essa abordagem foi descartada.

O status do pedido representa apenas o estado do pedido.

O movimento financeiro representa o fato de que houve consumo de verba.

Esse detalhe é importante porque, em sistemas reais, inconsistências podem ocorrer.

Exemplo:

```text
Pedido = CLOSED

Movimento = inexistente
```

Se a idempotência fosse baseada apenas no status do pedido, o sistema assumiria que o consumo já ocorreu, mesmo que isso não seja verdade.

Ao utilizar o movimento como referência, a verificação é baseada no evento financeiro que realmente caracteriza a operação.

---

# Uso do banco de dados como proteção adicional

Além da verificação na aplicação, a modelagem já possui uma proteção no banco:

```sql
UNIQUE (order_id, movement_type)
```

Isso impede que existam dois movimentos de consumo para o mesmo pedido.

A solução passa a possuir duas camadas de proteção:

1. A aplicação evita executar a operação novamente.
2. O banco impede que uma duplicação seja persistida caso a aplicação falhe em detectar o cenário.

Essa combinação reduz bastante a chance de inconsistências.

---

# O retorno da operação

Quando uma segunda requisição é recebida para um pedido já processado, a operação simplesmente retorna sucesso (`204 No Content`).

A decisão foi tomada porque esse comportamento representa melhor o conceito de idempotência.

Do ponto de vista do cliente da API, executar novamente uma operação que já foi concluída produz exatamente o mesmo resultado final.

Não existe necessidade de informar erro.

---

# Limitação conhecida

A solução implementada nesta etapa **não resolve concorrência**.

Existe uma pequena janela entre:

```text
Verificar se o movimento existe
```

e

```text
Persistir o novo movimento
```

Em um cenário de duas requisições simultâneas, ambas podem executar a verificação antes que qualquer uma delas tenha persistido o movimento.

Fluxo simplificado:

```text
Requisição A                  Requisição B

exists() → false              exists() → false

continua...                   continua...

save()                        save()
```

Nesse cenário, ambas acreditam que são a primeira execução.

A constraint `UNIQUE` do banco ainda impede a duplicação do movimento, mas uma das transações terminará com erro.

Ou seja, a implementação atual garante idempotência para chamadas repetidas em momentos diferentes, mas ainda não garante comportamento correto para chamadas simultâneas.

---

# Diferença entre idempotência e concorrência

Embora frequentemente apareçam juntas, idempotência e concorrência resolvem problemas diferentes.

## Idempotência

Pergunta respondida:

> "O que acontece se o cliente enviar a mesma requisição novamente?"

Exemplo:

```text
POST /orders/123/close
```

Executada agora.

Cinco segundos depois:

```text
POST /orders/123/close
```

O resultado continua sendo um único consumo de verba.

---

## Concorrência

Pergunta respondida:

> "O que acontece se duas requisições chegarem exatamente ao mesmo tempo?"

Exemplo:

```text
Requisição A
        │
        ├──────────────┐
        │              │
        ▼              ▼
Requisição B      Banco de Dados
```

Nesse caso, ambas disputam o mesmo recurso ao mesmo tempo.

O problema deixa de ser repetição lógica da operação e passa a ser sincronização entre transações.

---

# Como isso será resolvido no AC-05

O AC-05 introduz justamente o cenário de concorrência.

A estratégia prevista é utilizar bloqueio no carregamento da verba.

A busca da verba será realizada utilizando um lock pessimista (`PESSIMISTIC_WRITE`).

Com isso:

```text
Transação A
        │
        ▼
Obtém lock da verba
        │
        ▼
Consome saldo
        │
        ▼
Commit
```

Enquanto isso:

```text
Transação B

fica aguardando
```

Somente após o término da primeira transação a segunda continuará sua execução.

Nesse momento ela já encontrará:

* saldo atualizado;
* movimento registrado.

Assim, apenas uma das requisições conseguirá concluir o fechamento.

Essa estratégia elimina a janela de corrida existente na implementação do AC-04 e garante o atendimento do Acceptance Criteria relacionado à concorrência.

---

# Resumo da decisão

A implementação do AC-04 priorizou uma solução simples e incremental.

Foi adicionada apenas a lógica necessária para evitar reprocessamentos da mesma operação, mantendo o restante da arquitetura inalterado.

A solução reconhece explicitamente sua principal limitação: não trata concorrência simultânea.

Essa responsabilidade foi deixada intencionalmente para o AC-05, que introduz mecanismos específicos de sincronização entre transações.

Essa separação permite evoluir o sistema em pequenos passos, mantendo cada requisito responsável por resolver um único problema do domínio.

# Decisões de Implementação — AC-05 (Concorrência)

## Objetivo

O AC-05 introduz um cenário clássico de concorrência:

> Dois pedidos do mesmo vendedor podem ser fechados simultaneamente e disputar o mesmo saldo de verba.

O principal objetivo desta etapa foi garantir que a verba nunca fique negativa e que, quando o saldo não for suficiente para atender ambas as operações, no máximo uma delas seja concluída com sucesso.

---

# O recurso compartilhado é a verba

A primeira decisão foi identificar corretamente qual recurso precisava ser protegido.

Embora a operação seja "fechar pedido", o recurso compartilhado entre as transações não é o pedido.

É a verba (`Budget`).

Todo fechamento e todo estorno alteram o saldo da mesma entidade.

Por esse motivo, o mecanismo de sincronização foi aplicado sobre a verba.

---

# Lock pessimista

Foi adotado o bloqueio pessimista (`PESSIMISTIC_WRITE`) durante a leitura da verba.

Na prática, a consulta passa a ser executada utilizando `SELECT ... FOR UPDATE`.

Fluxo simplificado:

```text
Transação A

Busca verba (FOR UPDATE)
        │
        ▼
Obtém lock
        │
        ▼
Consome saldo
        │
        ▼
Registra movimento
        │
        ▼
Commit
        │
        ▼
Libera lock
```

Enquanto isso:

```text
Transação B

Busca verba (FOR UPDATE)

↓

Aguarda o término da Transação A
```

Somente após o commit da primeira transação a segunda continua sua execução.

Isso garante que ambas nunca alterem o mesmo saldo simultaneamente.

---

# Por que lock pessimista?

Existem duas estratégias comuns para controle de concorrência no JPA:

* Lock otimista (`@Version`)
* Lock pessimista (`PESSIMISTIC_WRITE`)

Foi escolhido o lock pessimista por alguns motivos.

Primeiro, trata-se de um domínio financeiro onde a consistência é mais importante do que maximizar paralelismo.

Além disso, o cenário apresentado pelo desafio envolve exatamente duas transações disputando o mesmo recurso.

Nesse contexto, bloquear temporariamente uma única linha da tabela é simples, previsível e suficientemente eficiente.

Com isso, evita-se a necessidade de implementar lógica de retry, tratamento de `OptimisticLockException` e reprocessamento da operação.

Para o escopo do desafio, essa solução oferece uma excelente relação entre simplicidade e robustez.

---

# Evolução da estratégia de idempotência

Durante o AC-04 a operação verificava se já existia um movimento de consumo antes de continuar o processamento.

O fluxo era semelhante a:

```text
Buscar pedido

↓

Verificar existência do movimento

↓

Consumir saldo
```

Essa abordagem funcionava para chamadas repetidas em momentos diferentes.

Entretanto, ainda existia uma janela de corrida.

Exemplo:

```text
Thread A                     Thread B

exists() = false             exists() = false

continua...                  continua...
```

Ambas poderiam acreditar que eram a primeira execução.

---

# A janela de concorrência foi eliminada

Com a introdução do lock pessimista, a ordem da operação foi alterada.

Novo fluxo:

```text
Buscar pedido

↓

Obter lock da verba

↓

Verificar existência do movimento

↓

Consumir saldo

↓

Registrar movimento

↓

Fechar pedido
```

Agora, apenas uma transação consegue executar essa sequência por vez.

Quando a segunda transação obtiver o lock, ela encontrará o estado atualizado da primeira.

Com isso:

* poderá identificar que o movimento já existe (idempotência);
* ou perceber que o saldo não é mais suficiente (regra de negócio).

A janela existente no AC-04 deixa de existir.

---

# O mesmo princípio foi aplicado ao cancelamento

Embora o Acceptance Criteria trate apenas do fechamento simultâneo, a mesma estratégia foi aplicada ao cancelamento.

A justificativa é que o cancelamento também altera o saldo da verba.

Sem sincronização, um cancelamento concorrente com um fechamento poderia provocar perda de atualização (*lost update*).

Por esse motivo, o `CancelOrderUseCase` também passou a adquirir um lock pessimista antes de alterar o saldo.

Essa decisão mantém um comportamento consistente para qualquer operação que modifique uma verba.

---

# Integridade em múltiplas camadas

Mesmo utilizando bloqueio pessimista, a constraint de unicidade foi mantida no banco:

```sql
UNIQUE (order_id, movement_type)
```

Ela deixa de ser a principal proteção contra concorrência e passa a atuar como uma garantia adicional de integridade.

A solução passa a proteger os dados em três níveis:

* Domínio: impede saldo negativo através da entidade `Budget`.
* Aplicação: controla o fluxo utilizando transações e bloqueio pessimista.
* Banco de dados: impede registros duplicados através das constraints.

Essa abordagem reduz significativamente a possibilidade de inconsistências.

---

# Trade-off da solução

O lock pessimista reduz o paralelismo das operações que utilizam a mesma verba.

Entretanto, essa limitação foi considerada aceitável porque:

* apenas vendedores da mesma competência disputam o mesmo registro;
* o tempo de retenção do lock é pequeno;
* consistência é mais importante do que throughput neste domínio.

Em um sistema com altíssimo volume de operações simultâneas, outras estratégias poderiam ser avaliadas, como lock otimista com retry automático ou mecanismos distribuídos.

Para o escopo deste desafio, o lock pessimista oferece uma solução simples, robusta e fácil de explicar.

---

# Resumo da decisão

A implementação do AC-05 concentrou o controle de concorrência no recurso realmente compartilhado: a verba.

A introdução do lock pessimista eliminou a janela de corrida existente na implementação anterior e tornou o processamento seguro para execuções simultâneas.

Além disso, a mesma estratégia foi aplicada tanto ao fechamento quanto ao cancelamento de pedidos, garantindo que qualquer alteração de saldo siga exatamente as mesmas regras de sincronização e consistência.
