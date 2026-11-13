# 0008. Autenticação por JWT stateless; autorização por papel na borda e por posse nos serviços

## Contexto
Era preciso identificar usuários sem sessão no servidor e de forma que continue valendo na versão distribuída.

## Decisão
Login devolve um JWT (HS256) com `sub` (id), `papel`, `iss`, `iat`, `exp`. Papéis: CLIENTE, ORGANIZADOR, PORTARIA.
O filtro de segurança aplica a regra grossa por URL e papel; os serviços aplicam a regra fina de posse ("é a
sua reserva?"). A identidade vem só do token, nunca do corpo. Senhas com BCrypt, atrás da porta
`CodificadorDeSenha`. Falha de login responde igual para e-mail inexistente e senha errada. 401 sem identidade,
403 com identidade sem permissão.

## Consequências
+ Sem sessão: escala e valida em qualquer instância. A posse não consulta outro contexto (usa as cópias locais).
- HS256 exige compartilhar o segredo com quem valida. Nos microsserviços o caminho é RS256 com chave pública
  (JWKS), validação no gateway e Usuários como emissor.
- Sem refresh, revogação, limite de tentativas ou recuperação de senha.