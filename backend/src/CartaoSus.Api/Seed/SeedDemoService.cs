using CartaoSus.Api.Data;
using CartaoSus.Api.Models;
using CartaoSus.Api.Security;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace CartaoSus.Api.Seed;

// Preenche os placeholders "__SERA_GERADO__" do seed.sql (SDD 8.5).
// Idempotente: só altera valores que ainda são placeholder.
public static class SeedDemoService
{
    public const string Placeholder = "__SERA_GERADO__";

    public static async Task ExecutarAsync(IServiceProvider sp, IConfiguration config, ILogger log)
    {
        if (!string.Equals(config["SEED_DEMO"], "true", StringComparison.OrdinalIgnoreCase))
            return;

        var senha = config["SENHA_DEMO"];
        if (string.IsNullOrWhiteSpace(senha) || senha.Length < 8)
            throw new InvalidOperationException(
                "SEED_DEMO=true exige SENHA_DEMO no .env com pelo menos 8 caracteres.");

        using var scope = sp.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<CartaoSusDbContext>();
        var hasher = scope.ServiceProvider.GetRequiredService<PasswordHasher<Usuario>>();

        var usuariosPendentes = await db.Usuarios.Where(u => u.SenhaHash == Placeholder).ToListAsync();
        foreach (var u in usuariosPendentes)
            u.SenhaHash = hasher.HashPassword(u, senha);

        var cidadaosPendentes = await db.Cidadaos.Include(c => c.Usuario)
            .Where(c => c.SegredoTotp == Placeholder).ToListAsync();
        foreach (var c in cidadaosPendentes)
        {
            c.SegredoTotp = SegurancaUtils.GerarSegredoTotp();
            // Exibido uma única vez, no console, para cadastrar no app. Não é gravado em arquivo.
            Console.WriteLine($"[seed] Segredo TOTP demo de {c.NomeCompleto} ({c.Usuario?.Login}): {c.SegredoTotp}");
        }

        if (usuariosPendentes.Count + cidadaosPendentes.Count == 0)
        {
            log.LogInformation("Seed demo já aplicado; nada a fazer.");
            return;
        }

        await db.SaveChangesAsync();
        log.LogInformation("Seed demo aplicado: {Usuarios} senhas e {Cidadaos} segredos TOTP gerados.",
            usuariosPendentes.Count, cidadaosPendentes.Count);
    }
}
