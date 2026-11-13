# 0002. Banco relacional único, um schema por contexto, sem chaves estrangeiras entre schemas

## Contexto
O domínio exige consistência forte em dois pontos: não vender além da capacidade e não duplicar pagamentos ou
ingressos. Isso favorece transações e restrições de integridade.

## Decisão
PostgreSQL 16 com um schema por contexto (migrações Flyway, `ddl-auto: validate`). Dentro de um schema há chaves
estrangeiras; entre schemas, só ids, sem FK.

## Consequências
+ Restrições `CHECK`, `UNIQUE` e índices parciais são a última linha de defesa contra erros de concorrência.
+ Cada schema vira o banco de um serviço sem alterar o modelo de dados.
- A integridade entre contextos é garantida por eventos e verificada por consulta (`scripts/consistencia.sql`),
  não pelo banco.

## Alternativas
Banco por contexto já no monólito: simula melhor os microsserviços, mas perde transação única e complica a
comparação. NoSQL: perde as restrições que sustentam os invariantes.