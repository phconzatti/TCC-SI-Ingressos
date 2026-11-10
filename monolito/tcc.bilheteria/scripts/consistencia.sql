-- Codigos de saida: 0 = consistente, 1 = invariante violado, 2 = falha ao executar.
SELECT ordem, gravidade, verificacao, violacoes FROM (

  SELECT 1 AS ordem, 'ERRO' AS gravidade,
         'reservados fora de [0, capacidade]' AS verificacao, count(*) AS violacoes
    FROM reservas.disponibilidade
   WHERE reservados < 0 OR reservados > capacidade

  UNION ALL
  SELECT 2, 'ERRO', 'reservados difere da soma das reservas ativas (aguardando + confirmada)', count(*)
    FROM reservas.disponibilidade d
   WHERE d.reservados <> COALESCE((SELECT SUM(r.quantidade) FROM reservas.reserva r
                                    WHERE r.categoria_id = d.id
                                      AND r.status IN ('AGUARDANDO_PAGAMENTO', 'CONFIRMADA')), 0)

  UNION ALL
  SELECT 3, 'ERRO', 'ingressos emitidos acima da capacidade da categoria (overselling)', count(*)
    FROM reservas.disponibilidade d
   WHERE (SELECT count(*) FROM ingressos.ingresso i WHERE i.categoria_id = d.id) > d.capacidade

  UNION ALL
  SELECT 4, 'ERRO', 'reserva CONFIRMADA sem exatamente um ingresso por unidade', count(*)
    FROM reservas.reserva r
   WHERE r.status = 'CONFIRMADA'
     AND (SELECT count(*) FROM ingressos.ingresso i WHERE i.reserva_id = r.id) <> r.quantidade

  UNION ALL
  SELECT 5, 'ERRO', 'ingresso de reserva inexistente ou nao confirmada', count(*)
    FROM ingressos.ingresso i
   WHERE NOT EXISTS (SELECT 1 FROM reservas.reserva r
                      WHERE r.id = i.reserva_id AND r.status = 'CONFIRMADA')

  UNION ALL
  SELECT 6, 'ERRO', 'numero do ingresso acima da quantidade da reserva', count(*)
    FROM ingressos.ingresso i
    JOIN reservas.reserva r ON r.id = i.reserva_id
   WHERE i.numero > r.quantidade

  UNION ALL
  SELECT 7, 'ERRO', 'ingresso com usuario, evento ou categoria diferente da reserva', count(*)
    FROM ingressos.ingresso i
    JOIN reservas.reserva r ON r.id = i.reserva_id
   WHERE i.usuario_id <> r.usuario_id OR i.evento_id <> r.evento_id OR i.categoria_id <> r.categoria_id

  UNION ALL
  SELECT 8, 'ERRO', 'status do ingresso incoerente com utilizado_em', count(*)
    FROM ingressos.ingresso
   WHERE (status = 'UTILIZADO' AND utilizado_em IS NULL)
      OR (status = 'EMITIDO' AND utilizado_em IS NOT NULL)

  UNION ALL
  SELECT 9, 'ERRO', 'reserva CONFIRMADA sem exatamente um pagamento APROVADO', count(*)
    FROM reservas.reserva r
   WHERE r.status = 'CONFIRMADA'
     AND (SELECT count(*) FROM pagamentos.pagamento p
           WHERE p.reserva_id = r.id AND p.status = 'APROVADO') <> 1

  UNION ALL
  SELECT 10, 'ERRO', 'pagamento APROVADO de reserva nao confirmada', count(*)
    FROM pagamentos.pagamento p
   WHERE p.status = 'APROVADO'
     AND NOT EXISTS (SELECT 1 FROM reservas.reserva r
                      WHERE r.id = p.reserva_id AND r.status = 'CONFIRMADA')

  UNION ALL
  SELECT 11, 'ERRO', 'valor do pagamento diferente do valor da reserva', count(*)
    FROM pagamentos.pagamento p
    JOIN reservas.reserva r ON r.id = p.reserva_id
   WHERE p.valor_centavos <> r.valor_total_centavos

  UNION ALL
  SELECT 12, 'ERRO', 'reserva com mais de um pagamento vivo (INICIADO ou APROVADO)', count(*)
    FROM (SELECT reserva_id FROM pagamentos.pagamento
           WHERE status IN ('INICIADO', 'APROVADO')
           GROUP BY reserva_id HAVING count(*) > 1) t

  UNION ALL
  SELECT 13, 'ERRO', 'pagamento preso em INICIADO (no monolito nunca persiste nesse estado)', count(*)
    FROM pagamentos.pagamento
   WHERE status = 'INICIADO'

  UNION ALL
  SELECT 14, 'ERRO', 'reserva sem a copia local (reserva_ref) em Pagamentos', count(*)
    FROM reservas.reserva r
   WHERE NOT EXISTS (SELECT 1 FROM pagamentos.reserva_ref f WHERE f.id = r.id)

  UNION ALL
  SELECT 15, 'ERRO', 'copia local (reserva_ref) divergente da reserva', count(*)
    FROM pagamentos.reserva_ref f
    JOIN reservas.reserva r ON r.id = f.id
   WHERE f.usuario_id <> r.usuario_id
      OR f.valor_centavos <> r.valor_total_centavos
      OR f.expirada <> (r.status = 'EXPIRADA')

  UNION ALL
  SELECT 16, 'AVISO', 'reservas vencidas ha mais de 2 min ainda aguardando pagamento (job de expiracao desligado?)', count(*)
    FROM reservas.reserva
   WHERE status = 'AGUARDANDO_PAGAMENTO' AND expira_em < now() - interval '2 minutes'

) v
ORDER BY ordem;