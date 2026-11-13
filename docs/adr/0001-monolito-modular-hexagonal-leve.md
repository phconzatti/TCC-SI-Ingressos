# 0001. Monólito modular com arquitetura hexagonal leve

## Contexto
O TCC compara monólito e microsserviços com o mesmo domínio. Para a comparação ser justa, as duas versões
precisam compartilhar regras de negócio; só a forma de implantação e de comunicação pode mudar.

## Decisão
Um monólito dividido em contextos (eventos, reservas, pagamentos, ingressos, usuários). Em cada um:
`domain` (Java puro), `application` (serviços e portas) e `adapter` (entrada e saída). "Leve" porque o domínio
não usa frameworks, mas não há camadas de mapeamento extras além das necessárias. Regras verificadas por
ArchUnit (domínio sem Spring/jakarta/jackson) e Spring Modulith (`verify()` rejeita ciclos entre contextos).

## Consequências
+ Os contextos já nascem com as fronteiras que virarão serviços; o domínio é reaproveitado sem mudanças.
+ As fronteiras são verificadas por teste, não só por convenção.
- Há mais classes (portas e adaptadores) do que num CRUD em camadas.

## Alternativas
Camadas tradicionais (controller/service/repository): menos classes, mas as dependências cruzam contextos e a
extração para serviços exigiria reescrever. Hexagonal "pleno" (um mapeador para cada borda): mais puro, mais
cerimônia do que o estudo precisa.