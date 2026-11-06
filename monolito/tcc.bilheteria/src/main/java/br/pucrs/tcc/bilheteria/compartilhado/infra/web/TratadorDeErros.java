package br.pucrs.tcc.bilheteria.compartilhado.infra.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.AcessoNegadoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.ExcecaoDeDominio;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoAutenticadoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;

@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ExcecaoDeDominio.class)
    ProblemDetail aoViolarRegra(ExcecaoDeDominio e) {
        int status = statusDe(e);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), e.getMessage());
        problema.setTitle(HttpStatus.valueOf(status).getReasonPhrase());
        problema.setProperty("codigo", e.codigo());
        return problema;
    }

    private static int statusDe(ExcecaoDeDominio e) {
        if (e instanceof NaoEncontradoException) {
            return 404;
        }
        if (e instanceof ConflitoException) {
            return 409;
        }
        if (e instanceof NaoAutenticadoException) {
            return 401;
        }
        if (e instanceof AcessoNegadoException) {
            return 403;
        }
        return 422; // RegraDeNegocioException
    }
}