using System.Text.Json;
using CartaoSus.Api.Exceptions;

namespace CartaoSus.Api.Middleware;

// Converte exceções no envelope ErrorResponse do SDD 5.1.
public class ErrorHandlingMiddleware(RequestDelegate next, ILogger<ErrorHandlingMiddleware> log)
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    public async Task InvokeAsync(HttpContext ctx)
    {
        try
        {
            await next(ctx);
        }
        catch (ErroDeNegocio ex)
        {
            var (status, codigo) = Mapear(ex);
            var campos = ex is ValidacaoException v ? v.Campos : new Dictionary<string, string>();
            await Escrever(ctx, status, codigo, ex.Message, campos);
        }
        catch (Exception ex)
        {
            log.LogError(ex, "Erro não tratado");
            await Escrever(ctx, 500, "ERRO_INTERNO", "Erro interno do servidor.",
                new Dictionary<string, string>());
        }
    }

    public static (int Status, string Codigo) Mapear(ErroDeNegocio ex) => ex switch
    {
        ValidacaoException => (400, "VALIDACAO"),
        CredenciaisInvalidasException => (401, "NAO_AUTORIZADO"),
        SemPermissaoException => (403, "SEM_PERMISSAO"),
        NaoEncontradoException => (404, "NAO_ENCONTRADO"),
        ConflitoException => (409, "CONFLITO"),
        _ => (500, "ERRO_INTERNO")
    };

    public static Task Escrever(HttpContext ctx, int status, string codigo, string mensagem,
        IDictionary<string, string> campos)
    {
        ctx.Response.StatusCode = status;
        ctx.Response.ContentType = "application/json; charset=utf-8";
        var corpo = new
        {
            timestamp = DateTime.UtcNow.ToString("o"),
            status,
            erro = codigo,
            mensagem,
            campos
        };
        return ctx.Response.WriteAsync(JsonSerializer.Serialize(corpo, Json));
    }
}
