# 0007. Idempotência do pagamento

## Contexto
Redes falham e clientes repetem requisições; cobrar duas vezes é inaceitável.

## Decisão
O cliente envia `Idempotency-Key`. Repetição da mesma chave devolve o resultado original (200, sem cobrar de novo);
a mesma chave para outra reserva dá 409. `UNIQUE(chave_idempotencia)` e um índice parcial
(`WHERE status IN ('INICIADO','APROVADO')`) garantem, no banco, no máximo um pagamento vivo por reserva.
`DataIntegrityViolationException` é traduzida para conflito no adaptador. A posse da reserva é conferida
**antes** da idempotência, para que outro usuário não receba o resultado do pagamento alheio.

## Consequências
+ Seguro contra repetição e contra concorrência (o banco decide a corrida).
- Os consumidores de eventos também precisam ser idempotentes (a emissão de ingressos é).