# 0004. No monólito, eventos síncronos na mesma transação

## Contexto
O Spring oferece `@EventListener` (síncrono) e `@TransactionalEventListener`/Modulith (assíncrono, após commit).

## Decisão
`@EventListener` simples: o listener roda na mesma thread e na mesma transação de quem publicou. A exceção de um
listener desfaz toda a cadeia (pagamento, confirmação e emissão dos ingressos).

## Consequências
+ Atomicidade: o pagamento aprovado e o ingresso emitido existem juntos ou não existem. A corrida entre pagar
  e expirar se resolve por rollback.
+ Nenhum estado intermediário visível; os invariantes valem a qualquer instante.
- A latência do pedido inclui toda a cadeia; a chamada ao gateway fica dentro da transação.
- É exatamente o que muda nos microsserviços: a cadeia vira assíncrona, com outbox e saga compensatória
  (estorno). A diferença é o objeto de medição do TCC.

## Alternativas
Listeners após o commit: mais próximos dos microsserviços, mas introduzem consistência eventual e a tabela
`event_publication` (existe na migração V2, vazia com esta decisão).