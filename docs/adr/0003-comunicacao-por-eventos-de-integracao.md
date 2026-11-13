# 0003. Contextos se comunicam só por eventos de integração

## Contexto
Para extrair um contexto como serviço sem reescrevê-lo, ele não pode chamar nem consultar outro diretamente.

## Decisão
Os contextos trocam apenas records imutáveis em `compartilhado/contratos` (EventoPublicado, ReservaCriada,
ReservaExpirada, PagamentoAprovado, ReservaConfirmada), publicados pela porta `PublicadorDeEventos`. Referências
entre contextos são só ids.

## Consequências
+ O contrato entre contextos é explícito, versionável e testável.
+ Trocar o transporte (memória por broker) altera só um adaptador.
- Exige consumidores idempotentes, requisito do transporte com entrega "ao menos uma vez" nos microsserviços.
- `compartilhado` é um módulo aberto: contratos e kernel são a única dependência permitida entre contextos.

## Alternativas
Chamadas diretas entre serviços de aplicação: mais simples, mas cria o acoplamento que o TCC quer medir.