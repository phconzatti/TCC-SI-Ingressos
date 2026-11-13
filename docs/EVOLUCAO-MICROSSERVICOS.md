# Evolução para microsserviços (plano do TC2)

Princípio: o **domínio e as regras de negócio não mudam**. Mudam os adaptadores e a infraestrutura.

## O que permanece
- Pacotes `domain` e `application` de cada contexto.
- Contratos de eventos e rotas REST externas (conferidas pelo `docs/openapi-monolito.json`).
- Testes de domínio e de serviço (fakes de repositório e de gateway).
- Invariantes de negócio.

## O que muda, porta por porta

| Hoje (monólito) | Nos microsserviços | Consequência a medir |
|---|---|---|
| `PublicadorDeEventosSpring` (memória) | Publicação em broker com **padrão outbox** (grava o evento na mesma transação do dado) | Latência extra; garantia de entrega |
| Listeners `@EventListener` | Consumidores do broker, **idempotentes** (entrega ao menos uma vez) | Duplicatas toleradas |
| Cadeia síncrona numa transação | **Saga**: pagamento → reserva confirma → ingressos emitem, com **compensação** (estorno se a confirmação falhar) | Janela de consistência eventual |
| Um banco, cinco schemas | Um banco (ou schema) **por serviço** | Verificador de consistência executa por serviço e cruza os resultados |
| `RepositorioDe*Jpa` | Idem, apontando para o banco do serviço | Sem mudança de código |
| Cópias locais (`Disponibilidade`, `ReservaRef`) | Idem; agora há atraso real até chegarem | 409 transitório em `POST /pagamentos` (já previsto) |
| JWT HS256, segredo único | **RS256**: Usuários assina, os demais validam com a chave pública (JWKS); validação na borda | Sem segredo compartilhado |
| Filtro único | **API gateway** na frente | Salto extra de rede |
| Métricas por tag `application` | Uma tag por serviço, mesmo dashboard | Comparação direta |
| `docker-compose` com 1 app | Um serviço por contexto, **mesmos limites de CPU e memória por serviço** | Igualdade de condições |

## Decisões em aberto
- Escolha do broker (por exemplo RabbitMQ ou Kafka): decidir pesando operação e semântica de entrega. Não decidida.
- Estratégia de gateway: reaproveitar o filtro atual ou usar um gateway dedicado.

## Plano dos testes de carga (k6)

Variáveis controladas: CPU e memória por serviço, `POOL_DE_CONEXOES`, `LATENCIA_DO_GATEWAY`, mesma massa de dados.

| Cenário | Pergunta |
|---|---|
| Corrida pelo último ingresso (N clientes, estoque pequeno) | Há overselling? Qual a vazão? |
| Abertura de vendas (rajada de reservas) | Latência p95/p99 e erros 409 |
| Pagamento lento (`LATENCIA_DO_GATEWAY` alta) | O pool de conexões vira gargalo? (no monólito a transação segura a conexão durante a cobrança) |
| Reservas expirando enquanto são pagas | Estado consistente? Quantas compensações? |
| Repetição de requisições com a mesma `Idempotency-Key` | Alguma cobrança duplicada? |

Ao fim de cada execução: `scripts/verificar-consistencia.ps1`. No monólito espera-se zero violações em qualquer
momento; nos microsserviços, zero após a drenagem das filas, e o **tempo de convergência** entra como métrica.