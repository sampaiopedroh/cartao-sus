using System.Security.Claims;
using System.Text.Encodings.Web;
using CartaoSus.Api.Middleware;
using CartaoSus.Api.Repositories;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace CartaoSus.Api.Security;

// Autenticação de dispositivo IoT por X-API-KEY (SDD 5.1). O banco guarda só o SHA-256 da chave.
public class ApiKeyAuthHandler(
    IOptionsMonitor<AuthenticationSchemeOptions> options,
    ILoggerFactory logger,
    UrlEncoder encoder,
    IDispositivoRepository dispositivos)
    : AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)
{
    public new const string Scheme = "ApiKey";

    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        if (!Request.Headers.TryGetValue("X-API-KEY", out var chaveHeader) || string.IsNullOrWhiteSpace(chaveHeader))
            return AuthenticateResult.NoResult();

        var hash = SegurancaUtils.HashChave(chaveHeader.ToString());
        var dispositivo = await dispositivos.PorHashAsync(hash);
        if (dispositivo is null)
            return AuthenticateResult.Fail("Chave de dispositivo inválida ou inativa.");

        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, dispositivo.IdDispositivo.ToString()),
            new(ClaimTypes.Role, $"DISPOSITIVO_{dispositivo.Tipo}"),
        };
        if (dispositivo.IdUnidade is not null) claims.Add(new Claim("idUnidade", dispositivo.IdUnidade.Value.ToString()));
        if (dispositivo.IdCidadao is not null) claims.Add(new Claim("idCidadao", dispositivo.IdCidadao.Value.ToString()));

        var identity = new ClaimsIdentity(claims, Scheme);
        var ticket = new AuthenticationTicket(new ClaimsPrincipal(identity), Scheme);
        return AuthenticateResult.Success(ticket);
    }

    // Resposta 401 no envelope de erro da API (SDD 5.1).
    protected override async Task HandleChallengeAsync(AuthenticationProperties properties)
    {
        Response.StatusCode = StatusCodes.Status401Unauthorized;
        await ErrorHandlingMiddleware.Escrever(Context, 401, "NAO_AUTORIZADO",
            "Chave de dispositivo ausente ou inválida.", new Dictionary<string, string>());
    }
}
