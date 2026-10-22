package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Porta de saída para publicar eventos. Cada versão da aplicação fornece seu adaptador.
public interface PublicadorDeEventos {
    
    void publicar(EventoDeIntegracao evento);
}
