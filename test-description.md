# Etapa 2 — Take-home

**Timebox:** 4 horas de trabalho efetivo. **Prazo:** 5 dias corridos.
**Stack:** Java 21 + Spring Boot 3.x + PostgreSQL no backend; React 18+ + TypeScript no frontend. Banco em memória (H2) é aceitável se preferir.

**Ferramentas de IA são liberadas e esperadas.** Aqui a gente trabalha com elas todos os dias. O que avaliamos não é se você escreveu cada linha, é se você entende o que entregou e sabe onde a entrega é frágil.

---

## Capability: Verba Promocional do Vendedor

Cada vendedor recebe uma verba promocional com teto mensal (competência = ano/mês). Ao fechar um pedido com desconto, o desconto é debitado da verba. A verba não pode ficar negativa.

### Acceptance criteria

**AC-01 — Consumo no fechamento**
- **WHEN** um pedido com desconto de valor `D` é fechado por um vendedor cuja verba na competência tem saldo `S >= D`
- **THEN** o saldo passa a ser `S - D` e um movimento de consumo é registrado, vinculado ao pedido.

**AC-02 — Verba insuficiente**
- **WHEN** um pedido com desconto `D` é fechado e o saldo da verba é `S < D`
- **THEN** o fechamento é rejeitado, o pedido permanece no status anterior e nenhum movimento é registrado. O cliente da API recebe um erro que permite distinguir esse caso de qualquer outro.

**AC-03 — Estorno no cancelamento**
- **WHEN** um pedido já fechado é cancelado
- **THEN** o valor consumido por aquele pedido é devolvido à verba e um movimento de estorno é registrado.
- **E** se o cancelamento ocorre em competência posterior à do consumo, o estorno é creditado na competência **do consumo original**, não na competência corrente.

**AC-04 — Idempotência**
- **WHEN** a requisição de fechamento de um mesmo pedido chega duas vezes
- **THEN** a verba é debitada uma única vez.

**AC-05 — Concorrência**
- **WHEN** dois pedidos do mesmo vendedor são fechados simultaneamente e a soma dos descontos excede o saldo
- **THEN** no máximo um deles é fechado. O saldo nunca fica negativo.

**AC-06 — Painel do coordenador (frontend)**
- **WHEN** o coordenador abre o painel da equipe
- **THEN** ele vê o saldo consolidado da equipe e a lista de vendedores.
- **E** o número consolidado, sozinho, deve deixar claro se existe algum vendedor em situação crítica (saldo baixo ou zerado) sem que o coordenador precise abrir a lista. **Como você resolve isso é decisão sua — e queremos ouvir o porquê.**

### Fora de escopo

Autenticação, cadastro de vendedores/pedidos (pode fazer seed), UI de criação de pedido, deploy, CI.

---

## O que entregar

1. Repositório (Git, com histórico — commits pequenos dizem mais do que um squash).
2. `README.md`: como rodar em um comando.
3. **`DECISIONS.md`** — o arquivo mais importante da entrega. Curto, sem enfeite:
   - Que decisões de modelagem você tomou e por quê.
   - Onde você foi pragmático de propósito por causa do timebox, e o que faria com mais tempo.
   - **Qual parte da sua solução é a mais frágil, e como ela quebraria em produção.**
   - Como você usou IA no processo, e onde precisou corrigi-la.

Não queremos uma entrega completa. Queremos uma entrega **consciente**. Um take-home que corta escopo e diz exatamente o que cortou vale mais que um que finge cobrir tudo.