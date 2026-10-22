package br.pucrs.tcc.bilheteria.compartilhado.kernel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// Relógio controlável para testes, permite avançar o tempo sem ter espera real
public class RelogioDeTeste extends Clock {
     
    private volatile Instant agora;
    private final ZoneId zona;

    public RelogioDeTeste(Instant inicial) {
        this(inicial, ZoneOffset.UTC);
    }

    private RelogioDeTeste(Instant inicial, ZoneId zona) {
        this.agora = inicial;
        this.zona = zona;
    }

    public void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new RelogioDeTeste(agora, novaZona);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
