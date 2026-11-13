# 0006. Concorrência: lock pessimista com defesa em profundidade no banco

## Contexto
Centenas de pedidos simultâneos disputam poucos ingressos. A alternativa mais simples é a otimista (versão), que
sob contenção alta gera muitas retentativas.

## Decisão
`SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`) em `Disponibilidade` ao reservar, em `Reserva` ao
confirmar/expirar e em `Ingresso` no check-in. Mesmo assim, o banco impõe `CHECK (reservados <= capacidade)`,
`UNIQUE` e um índice único parcial como segunda barreira.

## Consequências
+ Teste: 50 threads disputando 10 ingressos resultam em exatamente 10 vendas; 10 check-ins simultâneos do mesmo
  código passam uma única vez.
+ Ordem fixa de travas (reserva, depois disponibilidade; o pagamento trava a reserva por último) evita deadlock
  por construção. A prova empírica fica para os testes de carga do TC2.
- Serializa a venda por categoria: aceitável para o estudo; é o ponto a ser medido sob carga.

## Alternativas
Otimista com retentativa; fila serializada por categoria. Ficam como comparação futura.