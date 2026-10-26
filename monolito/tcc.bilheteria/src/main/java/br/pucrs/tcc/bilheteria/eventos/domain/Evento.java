package br.pucrs.tcc.bilheteria.eventos.domain;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/* 
Agregado raiz do contexto Eventos
A soma das capacidades das categorias não excede a capacidade do evento;
Só é possível publicar com ao menos uma categoria;
Após publicado, as categorias não mudam;
A vanda de cada categoria deve encerrar até a data do evento.
*/ 
public class Evento {
    
    private final UUID id;
    private final String nome;
    private final String local;
    private final Instant dataDoEvento;
    private final int capacidade;
    private StatusEvento status;
    private final List<Categoria> categorias;


    public Evento(UUID id, String nome, String local, Instant dataDoEvento, int capacidade, StatusEvento status, List<Categoria> categorias) {
        this.id = id;
        this.nome = nome;
        this.local = local;
        this.dataDoEvento = dataDoEvento;
        this.capacidade = capacidade;
        this.status = status;
        this.categorias = new ArrayList<>(categorias);
    }

    public static Evento criar(String nome, String local, Instant dataDoEvento, int capacidade, Instant agora) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("evento.nome-obrigatorio", "O nome do evento é obrigatório");
        }
        if (local == null || local.isBlank()) {
            throw new RegraDeNegocioException("evento.local-obrigatorio", "O local do evento é obrigatório");
        }
        if (dataDoEvento == null || !dataDoEvento.isAfter(agora)) {
            throw new RegraDeNegocioException("evento.data-invalida", "A data do evento deve estar no futuro");
        }
        if (capacidade <= 0) {
            throw new RegraDeNegocioException("evento.capacidade-invalida", "A capacidade do evento deve ser maior do que zero");
        }
        return new Evento(Ids.novo(), nome, local, dataDoEvento, capacidade, StatusEvento.RASCUNHO, List.of());
    }


    // Usado pelo adaptador de persistência, não valida as regras de criação
    public static Evento reconstruir(UUID id, String nome, String local, Instant dataDoEvento, int capacidade, StatusEvento status, List<Categoria> categorias) {
        return new Evento(id, nome, local, dataDoEvento, capacidade, status, categorias);
    }

    public Categoria adicionarCategoria(String nomeCategoria, Dinheiro preco, int capacidadeCategoria, Periodo janelaDeVendas) {
        if (status != StatusEvento.RASCUNHO) {
            throw new ConflitoException("evento.nao-editavel", "Só é possível alterar categorias de um rascunho");
        }
        if (janelaDeVendas.fim().isAfter(dataDoEvento)) {
            throw new RegraDeNegocioException("categoria.janela-apos-evento", "A venda deve encerrar até a data do evento");
        }
        if (capacidadeAlocada() + capacidadeCategoria > capacidade) {
            throw new RegraDeNegocioException("evento.capacidade-excedida", "A soma das capacidades das categorias excede a capacidade do evento");
        }
        if (categorias.stream().anyMatch(c -> c.nome().equalsIgnoreCase(nomeCategoria))) {
            throw new ConflitoException("categoria.nome-duplicado", "Já existe categoria com esse nome");
        }
        Categoria categoria = new Categoria(Ids.novo(), nomeCategoria, preco, capacidadeCategoria, janelaDeVendas);
        categorias.add(categoria);
        return categoria;
    }

    public void publicar() {
        if (status != StatusEvento.RASCUNHO) {
            throw new ConflitoException("evento.ja-publicado", "O evento já foi publicado ou cancelado");
        }
        if (categorias.isEmpty()) {
            throw new RegraDeNegocioException("evento.sem-categorias", "É necessário ao menos uma categoria para publicar"); 
        }
        status = StatusEvento.PUBLICADO;
    }

    public int capacidadeAlocada() {
        return categorias.stream().mapToInt(Categoria::capacidade).sum();
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public String local() { return local; }
    public Instant dataDoEvento() { return dataDoEvento; }
    public int capacidade() { return capacidade; }
    public StatusEvento status() { return status; }
    public List<Categoria> categorias() { return Collections.unmodifiableList(categorias); }
}