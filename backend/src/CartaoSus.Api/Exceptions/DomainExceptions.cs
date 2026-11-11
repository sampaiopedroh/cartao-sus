namespace CartaoSus.Api.Exceptions;

// Cada exceção carrega o código do envelope e o status HTTP (SDD 5.1).
public abstract class ErroDeNegocio(string codigo, int status, string mensagem) : Exception(mensagem)
{
    public string Codigo { get; } = codigo;
    public int Status { get; } = status;
}

public class ValidacaoException(string mensagem, IDictionary<string, string>? campos = null)
    : ErroDeNegocio("VALIDACAO", 400, mensagem)
{
    public IDictionary<string, string> Campos { get; } = campos ?? new Dictionary<string, string>();
}

public class CredenciaisInvalidasException()
    : ErroDeNegocio("NAO_AUTORIZADO", 401, "Credenciais inválidas.");

public class ConflitoException(string mensagem) : ErroDeNegocio("CONFLITO", 409, mensagem);

public class NaoEncontradoException(string mensagem) : ErroDeNegocio("NAO_ENCONTRADO", 404, mensagem);

public class SemPermissaoException(string mensagem) : ErroDeNegocio("SEM_PERMISSAO", 403, mensagem);

public class TokenInvalidoException(string mensagem = "Token expirado ou inválido.")
    : ErroDeNegocio("TOKEN_INVALIDO", 403, mensagem);

public class BloqueadoException(string mensagem = "Acolhimento bloqueado por 10 min após 3 falhas.")
    : ErroDeNegocio("BLOQUEADO", 403, mensagem);

public class FalhaOrigemException(string mensagem = "Serviço do SUS indisponível no momento.")
    : ErroDeNegocio("FALHA_ORIGEM", 502, mensagem);
