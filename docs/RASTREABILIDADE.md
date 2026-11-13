# Rastreabilidade: modelagem, código e testes

`B` = `src/main/java/br/pucrs/tcc/bilheteria`. Testes em `src/test/java/br/pucrs/tcc/bilheteria`.

## Contextos

| Contexto | Responsabilidade | Pacote | Agregados / serviço | Entrada (REST, eventos, jobs) | Testes principais | ADR |
|---|---|---|---|---|---|---|
| Eventos | Cadastrar e publicar eventos e categorias | `B/eventos` | `Evento`, `Categoria`, `EventoService` | `EventoController`; publica `EventoPublicado` | `EventoTest`, `EventoServiceTest`, `EventoApiTest` | 0001, 0003 |
| Reservas | Reservar, confirmar e expirar; controlar estoque | `B/reservas` | `Reserva`, `Disponibilidade`, `ReservaService` | `ReservaController`; listeners de `EventoPublicado` e `PagamentoAprovado`; `ExpiracaoDeReservasJob` | `ReservaTest`, `ReservaServiceTest`, `ReservasIntegracaoTest` | 0005, 0006 |
| Pagamentos | Cobrar com idempotência; estornar | `B/pagamentos` | `Pagamento`, `ReservaRef`, `PagamentoService`, `GatewayDePagamento` | `PagamentoController`; listener de `ReservaCriada`/`ReservaExpirada` | `PagamentoTest`, `PagamentoServiceTest`, `PagamentosIntegracaoTest` | 0004, 0007 |
| Ingressos | Emitir um ingresso por unidade; check-in | `B/ingressos` | `Ingresso`, `IngressoService`, `GeradorDeCodigo` | `IngressoController`; listener de `ReservaConfirmada` | `IngressoTest`, `IngressoServiceTest`, `IngressosIntegracaoTest` | 0006 |
| Usuários | Cadastro, login e token | `B/usuarios` | `Usuario`, `UsuarioService` | `AutenticacaoController`; `UsuariosIniciaisRunner` (só laboratório) | `UsuarioTest`, `UsuarioServiceTest`, `SegurancaIntegracaoTest` | 0008 |
| Compartilhado | Tipos básicos, contratos, infraestrutura | `B/compartilhado` | `Dinheiro`, `Periodo`, eventos de integração, `TratadorDeErros`, segurança, métricas, OpenAPI | n/a | `DinheiroTest`, `PeriodoTest`, `ObservabilidadeIntegracaoTest`, `OpenApiIntegracaoTest` | 0003, 0009 |

## Garantias do sistema

| Garantia | Mecanismo no código | Teste que a comprova |
|---|---|---|
| Nunca vender além da capacidade | Lock em `Disponibilidade` + `CHECK reservados <= capacidade` | 50 threads para 10 ingressos = 10 vendas (`ReservasIntegracaoTest`) |
| Check-in vale uma vez | Lock em `Ingresso` + status | 10 check-ins simultâneos = 1 (`IngressosIntegracaoTest`) |
| Pagamento não duplica | `Idempotency-Key`, `UNIQUE`, índice parcial | `PagamentosIntegracaoTest`, `PagamentoServiceTest` |
| Pagar × expirar não deixa estado inconsistente | Rollback da cadeia (monólito) | `PagamentosIntegracaoTest` (reserva expirada na confirmação) |
| Emissão de ingressos idempotente | Verificação antes de emitir + `UNIQUE(reserva, numero)` | `IngressoServiceTest` |
| Só o dono acessa seus recursos | Posse nos serviços, identidade do token | `SegurancaIntegracaoTest`, `PagamentoServiceTest` |
| Papéis restringem as rotas | Filtro de segurança por URL | `SegurancaIntegracaoTest` |
| Invariantes valem no banco | `scripts/consistencia.sql` | `ConsistenciaTest`, `SegurancaIntegracaoTest` |
| Fronteiras entre contextos | ArchUnit e Modulith | `ArquiteturaTest`, `ModulithTest` |

## Eventos de integração

| Evento | Quem publica | Quem consome | Efeito |
|---|---|---|---|
| `EventoPublicado` | Eventos | Reservas | Cria as `Disponibilidade` |
| `ReservaCriada` | Reservas | Pagamentos | Cria a `ReservaRef` |
| `ReservaExpirada` | Reservas | Pagamentos | Marca `expirada`; estorna se houver pagamento aprovado |
| `PagamentoAprovado` | Pagamentos | Reservas | Confirma a reserva |
| `ReservaConfirmada` | Reservas | Ingressos | Emite os ingressos |

## Rotas e acesso

| Rota | Acesso |
|---|---|
| `POST /usuarios`, `POST /auth/login`, `GET /eventos`, `GET /eventos/{id}`, `GET /disponibilidades` | Público |
| `POST /eventos`, `/eventos/{id}/categorias`, `/eventos/{id}/publicacao`, `GET /pagamentos/{id}` | ORGANIZADOR |
| `POST /reservas`, `GET /reservas/{id}`, `POST /pagamentos` | CLIENTE (e dono da reserva) |
| `GET /ingressos`, `GET /ingressos/{codigo}` | Dono do ingresso ou equipe |
| `POST /ingressos/{codigo}/check-in` | PORTARIA ou ORGANIZADOR |
| `GET /usuarios/eu` | Qualquer autenticado |