# 0005. Cópias locais de dados de outros contextos

## Contexto
Reservas precisa saber a capacidade das categorias (dono: Eventos); Pagamentos precisa do valor e do dono da
reserva (dono: Reservas). Consultar o outro contexto criaria dependência síncrona.

## Decisão
Cada consumidor mantém uma cópia mínima alimentada por eventos: `Disponibilidade` (em Reservas, a partir de
EventoPublicado) e `ReservaRef` (em Pagamentos, a partir de ReservaCriada/ReservaExpirada).

## Consequências
+ Cada contexto decide com dados locais, sem consultar o vizinho; a autorização por posse também (usa o
  `usuarioId` da cópia).
+ Funciona igual nos microsserviços.
- Há duplicação de dados e, nos microsserviços, uma janela até a cópia chegar (o pagamento responde 409
  "reserva.indisponivel-para-pagamento" e o cliente tenta de novo).

## Alternativas
Consulta síncrona entre serviços: sem duplicação, mas acopla disponibilidade e latência.