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
