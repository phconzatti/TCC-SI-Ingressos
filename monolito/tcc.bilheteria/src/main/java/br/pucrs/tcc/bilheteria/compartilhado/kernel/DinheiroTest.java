package br.pucrs.tcc.bilheteria.compartilhado.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void somaEMultiplica() {
        assertThat(Dinheiro.deCentavos(1050).somar(Dinheiro.deCentavos(250))).isEqualTo(Dinheiro.deCentavos(1300));
        assertThat(Dinheiro.deCentavos(1050).multiplicar(3)).isEqualTo(Dinheiro.deCentavos(3150));
        assertThat(Dinheiro.deCentavos(1050).multiplicar(0)).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    void subtrai() {
        assertThat(Dinheiro.deCentavos(500).subtrair(Dinheiro.deCentavos(200))).isEqualTo(Dinheiro.deCentavos(300));
    }

    @Test
    void rejeitaValorNegativo() {
        assertThatExceptionOfType(RegraDeNegocioException.class)
                .isThrownBy(() -> Dinheiro.deCentavos(-1))
                .extracting(RegraDeNegocioException::codigo)
                .isEqualTo("dinheiro.negativo");
    }

    @Test
    void subtracaoNaoPodeFicarNegativa() {
        assertThatExceptionOfType(RegraDeNegocioException.class)
                .isThrownBy(() -> Dinheiro.deCentavos(100).subtrair(Dinheiro.deCentavos(101)))
                .extracting(RegraDeNegocioException::codigo)
                .isEqualTo("dinheiro.negativo");
    }

    @Test
    void rejeitaQuantidadeNegativa() {
        assertThatExceptionOfType(RegraDeNegocioException.class)
                .isThrownBy(() -> Dinheiro.deCentavos(100).multiplicar(-1))
                .extracting(RegraDeNegocioException::codigo)
                .isEqualTo("dinheiro.quantidade-negativa");
    }

    @Test
    void detectaEstouro() {
        assertThatThrownBy(() -> Dinheiro.deCentavos(Long.MAX_VALUE).somar(Dinheiro.deCentavos(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void comparaValores() {
        assertThat(Dinheiro.deCentavos(100)).isLessThan(Dinheiro.deCentavos(200));
        assertThat(Dinheiro.deCentavos(200).maiorQue(Dinheiro.deCentavos(100))).isTrue();
    }

    @Test
    void formataEmReais() {
        assertThat(Dinheiro.deCentavos(123456)).hasToString("R$ 1.234,56");
        assertThat(Dinheiro.deCentavos(5)).hasToString("R$ 0,05");
        assertThat(Dinheiro.ZERO).hasToString("R$ 0,00");
    }
}