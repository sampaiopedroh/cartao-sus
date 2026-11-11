namespace CartaoSus.Api.Exceptions;

// Cada exceção vira um código do envelope de erro (SDD 5.1).
public abstract class ErroDeNegocio(string mensagem) : Exception(mensagem);

public class ValidacaoException(string mensagem, IDictionary<string, string>? campos = null)
    : ErroDeNegocio(mensagem)
{
    public IDictionary<string, string> Campos { get; } =
        campos ?? new Dictionary<string, string>();
}

public class CredenciaisInvalidasException() : ErroDeNegocio("Credenciais inválidas.");

public class ConflitoException(string mensagem) : ErroDeNegocio(mensagem);

public class NaoEncontradoException(string mensagem) : ErroDeNegocio(mensagem);

public class SemPermissaoException(string mensagem) : ErroDeNegocio(mensagem);
