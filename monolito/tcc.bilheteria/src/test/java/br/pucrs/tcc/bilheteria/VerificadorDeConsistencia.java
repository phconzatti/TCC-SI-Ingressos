package br.pucrs.tcc.bilheteria;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.jdbc.core.JdbcTemplate;

final class VerificadorDeConsistencia {

    private static final String SQL = ler();

    private VerificadorDeConsistencia() {
    }

    static Map<Integer, Long> erros(JdbcTemplate jdbc) {
        Map<Integer, Long> resultado = new TreeMap<>();
        for (Map<String, Object> linha : jdbc.queryForList(SQL)) {
            if ("ERRO".equals(linha.get("gravidade"))) {
                resultado.put(((Number) linha.get("ordem")).intValue(),
                        ((Number) linha.get("violacoes")).longValue());
            }
        }
        return resultado;
    }

    static Map<Integer, Long> comViolacao(JdbcTemplate jdbc) {
        Map<Integer, Long> violadas = new TreeMap<>(erros(jdbc));
        violadas.values().removeIf(v -> v == 0);
        return violadas;
    }

    private static String ler() {
        try {
            return Files.readString(Path.of("scripts", "consistencia.sql"));
        } catch (IOException e) {
            throw new UncheckedIOException("scripts/consistencia.sql não encontrado (rode a partir da raiz do projeto)", e);
        }
    }
}