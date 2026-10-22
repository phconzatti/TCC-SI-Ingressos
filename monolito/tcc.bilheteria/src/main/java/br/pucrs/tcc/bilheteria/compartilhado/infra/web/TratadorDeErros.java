package br.pucrs.tcc.bilheteria.compartilhado.infra.web;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.ExcecaoDeDominio;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;



// Converte exceções de negócio em respostas padronizadas
@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regraDeNegocio(RegraDeNegocioException e) {
        return problema(422, "Regra de negócio violada", e);
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException e) {
        return problema(409, "Conflito", e);
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ProblemDetail naoEncontrado(NaoEncontradoException e) {
        return problema(404, "Não encontrado", e);
    }

    private static ProblemDetail problema(int status, String titulo, ExcecaoDeDominio e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), e.getMessage());
        problema.setTitle(titulo);
        problema.setProperty("codigo", e.codigo());
        return problema;
    }
}
