using System.Security.Claims;

namespace CartaoSus.Api.Security;

public static class PrincipalExtensions
{
    public static string Papel(this ClaimsPrincipal user) => user.FindFirstValue(ClaimTypes.Role) ?? string.Empty;

    public static bool EhCidadao(this ClaimsPrincipal user) => user.Papel() == "CIDADAO";
    public static bool EhProfissional(this ClaimsPrincipal user) => user.Papel() == "PROFISSIONAL_SAUDE";
    public static bool EhAdmin(this ClaimsPrincipal user) => user.Papel() == "ADMIN";
    public static bool EhDispositivo(this ClaimsPrincipal user) => user.Papel().StartsWith("DISPOSITIVO_");

    public static long? IdCidadao(this ClaimsPrincipal user)
    {
        var v = user.FindFirstValue("idCidadao");
        return v is null ? null : long.Parse(v);
    }

    public static long? IdUnidade(this ClaimsPrincipal user)
    {
        var v = user.FindFirstValue("idUnidade");
        return v is null ? null : long.Parse(v);
    }

    public static long IdUsuario(this ClaimsPrincipal user) =>
        long.Parse(user.FindFirstValue(ClaimTypes.NameIdentifier)!);
}
