package br.pucrs.tcc.bilheteria.eventos.domain;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

import java.util.Objects;
import java.util.UUID;

// Categoria do ingresso, preço, capacidade própria e janela de vendas
public record Categoria (
    UUID id,
    String nome,
    Dinheiro preco, 
    int capacidade,
    Periodo janelaDeVendas) {
    
        public Categoria {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(preco, "preco");
            Objects.requireNonNull(janelaDeVendas, "janelaDeVendas");
            if (nome == null || nome.isBlank()) {
                throw new RegraDeNegocioException("categoria.nome-obrigatorio", "O nome da categoria é obrigatório");
            }
            if (capacidade <= 0) {
                throw new RegraDeNegocioException("categoria.capacidade-invalida", "A capacidade da categoria deve ser maior que zero");
            }
        }
}
