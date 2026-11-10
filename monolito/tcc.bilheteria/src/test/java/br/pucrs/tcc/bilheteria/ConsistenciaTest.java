package br.pucrs.tcc.bilheteria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "bilheteria.reservas.expiracao-ativa=false")
@Transactional 
class ConsistenciaTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void bancoRecemMigradoEstaConsistente() {
        assertThat(VerificadorDeConsistencia.comViolacao(jdbc)).isEmpty();
    }

    @Test
    void detectaCorrupcaoInseridaDeProposito() {
        UUID evento = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();
        UUID reserva = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO reservas.disponibilidade
                  (id, evento_id, nome_categoria, preco_centavos, capacidade, reservados, inicio_vendas, fim_vendas)
                VALUES (?, ?, 'Pista', 1000, 10, 3, now(), now())""", categoria, evento);
        jdbc.update("""
                INSERT INTO reservas.reserva
                  (id, usuario_id, evento_id, categoria_id, quantidade, valor_total_centavos, status, criada_em, expira_em)
                VALUES (?, ?, ?, ?, 2, 2000, 'CONFIRMADA', now(), now())""", reserva, usuario, evento, categoria);
        jdbc.update("""
                INSERT INTO ingressos.ingresso
                  (id, reserva_id, numero, evento_id, categoria_id, usuario_id, codigo, status, emitido_em, utilizado_em)
                VALUES (?, ?, 1, ?, ?, ?, 'ORFAO', 'UTILIZADO', now(), NULL)""",
                UUID.randomUUID(), UUID.randomUUID(), evento, categoria, usuario);

        Map<Integer, Long> violadas = VerificadorDeConsistencia.comViolacao(jdbc);

        assertThat(violadas).containsKeys(
                2,   // reservados difere da soma das reservas ativas
                4,   // confirmada sem os ingressos
                5,   // ingresso órfão
                8,   // UTILIZADO sem utilizado_em
                9,   // confirmada sem pagamento aprovado
                14); // sem reserva_ref
    }
}