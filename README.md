# TCC: Monólito × Microsserviços em um sistema de venda de ingressos

Trabalho de Conclusão de Curso em Sistemas de Informação (PUCRS) que **compara uma arquitetura monolítica com uma
de microsserviços**, implementando o mesmo domínio nas duas: um sistema de reserva e venda de ingressos inspirado
no [Alf.io](https://alf.io). A comparação considera desempenho sob carga, consistência dos dados e complexidade
de construção e operação.

- **Autor:** Pedro Henrique Schneider Conzatti
- **Orientadora:** Profa. Dra. Ana Paula Terra Bacelo
- **Instituição:** PUCRS, curso de Sistemas de Informação

## Ideia central

As duas versões compartilham o **mesmo domínio**: as regras de negócio não mudam; só mudam a forma de implantar e a
forma como os contextos conversam (chamadas em memória e transação única no monólito; mensagens assíncronas e
bancos separados nos microsserviços). Dois requisitos guiam a avaliação:

1. nunca vender mais ingressos que a capacidade, mesmo com muitos pedidos simultâneos;
2. nunca cobrar nem emitir ingressos em duplicidade.

## Estado atual

| Etapa | Conteúdo | Situação |
|---|---|---|
| TC1 | Modelagem das duas versões e **monólito implementado** (cinco contextos, segurança, containers, observabilidade, verificação de consistência) | Concluída (tag `v0.1-tc1`) |
| TC2 | Versão de **microsserviços** e **testes de carga** com k6 comparando as duas | Planejada |

## Mapa do repositório

| Pasta | Conteúdo |
|---|---|
| [`monolito/`](monolito/README.md) | A versão monolítica: código, `docker-compose`, Prometheus e Grafana. **Comece por aqui para executar.** |
| [`docs/adr/`](docs/adr) | Decisões arquiteturais, com contexto, consequências e alternativas |
| [`docs/api/`](docs/api) | Contratos OpenAPI |
| [`docs/diagramas/`](docs/diagramas) | Diagramas e figuras |
| [`docs/RASTREABILIDADE.md`](docs/RASTREABILIDADE.md) | Onde, no código e nos testes, está cada conceito da modelagem |
| [`docs/EVOLUCAO-MICROSSERVICOS.md`](docs/EVOLUCAO-MICROSSERVICOS.md) | O que muda e o que permanece ao derivar os microsserviços; plano dos testes de carga |
| `infra/` | Reservada para a infraestrutura comum às duas versões (TC2) |

## Começo rápido

Requisitos: JDK 21 e Docker.

```powershell
cd monolito
docker compose up -d --build        # app + PostgreSQL + Prometheus + Grafana
./tcc.bilheteria/scripts/demonstracao.ps1   # percorre o fluxo completo e confere a consistência
```

Detalhes (testes, credenciais de laboratório, endereços, variáveis dos experimentos, limitações) em
[`monolito/README.md`](monolito/README.md).

## Tecnologias

Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Docker Compose, Micrometer, Prometheus, Grafana, Testcontainers,
ArchUnit, Spring Modulith e k6 (previsto para o TC2).