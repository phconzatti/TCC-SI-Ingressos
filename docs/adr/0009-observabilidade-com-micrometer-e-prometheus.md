# 0009. Observabilidade: Micrometer, Prometheus e Grafana; métricas de negócio por eventos

## Contexto
Os testes de carga precisam de medições do interior do sistema (latência por rota, pool de conexões, CPU), e as
duas arquiteturas precisam ser medidas do mesmo jeito.

## Decisão
Actuator com registro Prometheus; histogramas de latência HTTP; tag `application` em todas as métricas.
Contadores de negócio (reservas, pagamentos, ingressos) são alimentados por um único listener dos eventos de
integração, sem instrumentar os serviços de domínio. Prometheus e Grafana provisionados por arquivo.

## Consequências
+ O mesmo dashboard serve às duas versões, distinguidas pela tag.
- O contador conta o evento publicado, não o commit: pode superestimar em rollback. A fonte da verdade é o banco.