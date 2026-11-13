# 0010. Verificação de consistência por consulta direta ao banco

## Contexto
"O teste de carga terminou sem erros" não prova que o sistema permaneceu correto.

## Decisão
`scripts/consistencia.sql` confere 15 invariantes (e 1 aviso) lendo os três schemas. O mesmo arquivo é usado pelo
script PowerShell e pelos testes de integração, e um teste o valida corrompendo o banco de propósito.

## Consequências
+ Critério objetivo de correção, igual para as duas arquiteturas.
- Confere o estado final, não o histórico. Nos microsserviços, vale após a drenagem das filas, e o tempo até
  convergir é uma métrica do experimento.