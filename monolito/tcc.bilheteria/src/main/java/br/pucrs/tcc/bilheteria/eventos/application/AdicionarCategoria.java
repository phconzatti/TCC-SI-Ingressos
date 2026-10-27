package br.pucrs.tcc.bilheteria.eventos.application;

import java.time.Instant;

public record AdicionarCategoria(String nome, long precoEmCentavos, int capacidade, Instant inicioDasVendas, Instant fimDasVendas) {
    
}
