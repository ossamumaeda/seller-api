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