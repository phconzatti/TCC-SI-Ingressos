package br.pucrs.tcc.bilheteria.eventos.application;

import java.time.Instant;

public record CriarEvento(String nome, String local, Instant dataDoEvento, int capacidade) {
    
}
