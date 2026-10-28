package br.pucrs.tcc.bilheteria.reservas.domain;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

/* 
Cópia local, dentto de Reservas, do que interessa de uma categoria publicada (preço, capacidade, janela), mais o contador de ingressos reservados
O id é o id da categoria
Invariante central do sistema: 0 <= reservados <= capacidade
*/ 
public class Disponibilidade {
    
    private final UUID id;
    private final UUID eventoId;
    private final String nomeCategoria;
    private final Dinheiro preco;
    private final int capacidade;
    private final Periodo janelaDeVendas;
    private int reservados;


    private Disponibilidade(UUID id, UUID eventoId, String nomeCategoria, Dinheiro preco, int capacidade, Periodo janelaDeVendas, int reservados) {
        this.id = id;
        this.eventoId = eventoId;
        this.nomeCategoria = nomeCategoria;
        this.preco = preco;
        this.capacidade = capacidade;
        this.janelaDeVendas = janelaDeVendas;
        this.reservados = reservados;
    }
    
    public static Disponibilidade criar(UUID id, UUID eventoId, String nomeCategoria, Dinheiro preco, int capacidade, Periodo janelaDeVendas) {
        if (capacidade <= 0) {
            throw new RegraDeNegocioException("disponibilidade.capacidade-invalida", "A capacidade deve ser maior que zero");
        }
        return new Disponibilidade(id, eventoId, nomeCategoria, preco, capacidade, janelaDeVendas, 0);
    }

    public static Disponibilidade reconstituir(UUID id, UUID eventoId, String nomeCategoria, Dinheiro preco, int capacidade, Periodo janelaDeVendas, int reservados) {
        return new Disponibilidade(id, eventoId, nomeCategoria, preco, capacidade, janelaDeVendas, reservados);
    }

    public void reservar(int quantidade, Instant agora) {
        if (!janelaDeVendas.contem(agora)) {
            throw new RegraDeNegocioException("venda.fora-da-janela", "A categoria não está em período de vendas");
        }
        if (quantidade > disponiveis()) {
            throw new ConflitoException("disponibilidade.esgotada", "Não há ingressos suficientes nesta categoria");
        }
        reservados += quantidade;
    }

    public void liberar(int quantidade) {
        if (quantidade <= 0 || quantidade > reservados) {
            throw new RegraDeNegocioException("disponibilidade.liberacao-invalida", "Quantidade a liberar insonsistente com o que está reservado");
        }
        reservados -= quantidade;
    }

    public int disponiveis() {
        return capacidade - reservados;
    }

    public UUID id() { return id; }
    public UUID eventoId() { return eventoId; }
    public String nomeCategoria() { return nomeCategoria; }
    public Dinheiro preco() { return preco; }
    public int capacidade() { return capacidade; }
    public Periodo janelaDeVendas() { return janelaDeVendas; }
    public int reservados() { return reservados; }
}
