using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using CartaoSus.Api;
using CartaoSus.Api.Security;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using MySqlConnector;

// Os testes de integração compartilham o mesmo banco: rodam um por vez.
[assembly: CollectionBehavior(DisableTestParallelization = true)]

namespace CartaoSus.Tests.Integracao;

// Variáveis lidas do ambiente (carregadas do .env pelo run-integration.sh).
public static class AmbienteTeste
{
    public const string BancoTeste = "cartao_sus_test";
    public const string SenhaDemo = "SenhaDemo#2026";
    public const string JwtSecret = "chave-de-teste-integracao-com-mais-de-32-caracteres";
    public const string ChaveTerminalUbs = "chave-teste-terminal-ubs-0001";
    public const string ChaveTerminalUpa = "chave-teste-terminal-upa-0002";
    public const string ChaveTensiometro = "chave-teste-tensiometro-0003";
    public const string SegredoMaria = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    public const string SegredoJoao = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

    public static string Variavel(string nome, string? padrao = null) =>
        Environment.GetEnvironmentVariable(nome) ?? padrao
        ?? throw new InvalidOperationException($"Variável {nome} ausente. Rode backend/tests/run-integration.sh com o .env preenchido.");

    public static string RaizDoRepositorio()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir is not null && !File.Exists(Path.Combine(dir.FullName, "database", "schema.sql")))
            dir = dir.Parent;
        return dir?.FullName ?? throw new InvalidOperationException("Pasta cartao-sus/ não encontrada a partir do teste.");
    }

    public static string ConexaoRoot() =>
        $"Server={Variavel("MYSQL_HOST", "127.0.0.1")};Port={Variavel("MYSQL_PORT", "3306")};User=root;Password={Variavel("MYSQL_ROOT_PASSWORD")};SslMode=None;AllowUserVariables=true";

    public static string ConexaoApp() =>
        $"Server={Variavel("MYSQL_HOST", "127.0.0.1")};Port={Variavel("MYSQL_PORT", "3306")};Database={BancoTeste};User=cartao_app;Password={Variavel("MYSQL_APP_PASSWORD")};SslMode=None";

    public static string Sha256(string s) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(s))).ToLowerInvariant();
}

// Recria cartao_sus_test a partir de schema.sql e seed.sql, e prepara dados determinísticos.
// Usa root só para esta preparação (SDD 6.4); a API roda com cartao_app.
public static class BancoTesteSetup
{
    public static async Task RecriarAsync()
    {
        var raiz = AmbienteTeste.RaizDoRepositorio();
        await using var conn = new MySqlConnection(AmbienteTeste.ConexaoRoot());
        await conn.OpenAsync();

        await ExecutarArquivoAsync(conn, Path.Combine(raiz, "database", "schema.sql"));
        await ExecutarArquivoAsync(conn, Path.Combine(raiz, "database", "seed.sql"));

        var hasher = new PasswordHasher<CartaoSus.Api.Models.Usuario>();
        var hashSenha = hasher.HashPassword(new CartaoSus.Api.Models.Usuario(), AmbienteTeste.SenhaDemo);
        await ExecSqlAsync(conn, "UPDATE TB_USUARIO SET SENHA_HASH = @h", ("@h", hashSenha));
        await ExecSqlAsync(conn, "UPDATE TB_CIDADAO SET SEGREDO_TOTP = @s WHERE ID_CIDADAO = 1", ("@s", AmbienteTeste.SegredoMaria));
        await ExecSqlAsync(conn, "UPDATE TB_CIDADAO SET SEGREDO_TOTP = @s WHERE ID_CIDADAO = 2", ("@s", AmbienteTeste.SegredoJoao));

        // Dispositivos com chaves conhecidas (o banco guarda só o hash).
        await ExecSqlAsync(conn,
            "INSERT INTO TB_DISPOSITIVO_IOT (ID_UNIDADE, ID_CIDADAO, TIPO, CHAVE_API_HASH, ATIVO) VALUES (1, NULL, 'TERMINAL_NFC', @h1, 1)",
            ("@h1", AmbienteTeste.Sha256(AmbienteTeste.ChaveTerminalUbs)));
        await ExecSqlAsync(conn,
            "INSERT INTO TB_DISPOSITIVO_IOT (ID_UNIDADE, ID_CIDADAO, TIPO, CHAVE_API_HASH, ATIVO) VALUES (2, NULL, 'TERMINAL_NFC', @h2, 1)",
            ("@h2", AmbienteTeste.Sha256(AmbienteTeste.ChaveTerminalUpa)));
        await ExecSqlAsync(conn,
            "INSERT INTO TB_DISPOSITIVO_IOT (ID_UNIDADE, ID_CIDADAO, TIPO, CHAVE_API_HASH, ATIVO) VALUES (NULL, 2, 'TENSIOMETRO', @h3, 1)",
            ("@h3", AmbienteTeste.Sha256(AmbienteTeste.ChaveTensiometro)));
    }

    public static async Task<long> ContarAsync(string sql, params (string Nome, object Valor)[] parametros)
    {
        await using var conn = new MySqlConnection(AmbienteTeste.ConexaoApp());
        await conn.OpenAsync();
        await using var cmd = new MySqlCommand(sql, conn);
        foreach (var (nome, valor) in parametros) cmd.Parameters.AddWithValue(nome, valor);
        return Convert.ToInt64(await cmd.ExecuteScalarAsync());
    }

    public static async Task<string?> TextoAsync(string sql, params (string Nome, object Valor)[] parametros)
    {
        await using var conn = new MySqlConnection(AmbienteTeste.ConexaoApp());
        await conn.OpenAsync();
        await using var cmd = new MySqlCommand(sql, conn);
        foreach (var (nome, valor) in parametros) cmd.Parameters.AddWithValue(nome, valor);
        return (await cmd.ExecuteScalarAsync())?.ToString();
    }

    public static async Task ExecSqlAsync(MySqlConnection conn, string sql, params (string Nome, object Valor)[] parametros)
    {
        await using var cmd = new MySqlCommand(sql, conn);
        foreach (var (nome, valor) in parametros) cmd.Parameters.AddWithValue(nome, valor);
        await cmd.ExecuteNonQueryAsync();
    }

    private static async Task ExecutarArquivoAsync(MySqlConnection conn, string caminho)
    {
        var texto = File.ReadAllText(caminho, Encoding.UTF8)
            .Replace("USE cartao_sus;", $"USE {AmbienteTeste.BancoTeste};");
        var semComentarios = string.Join("\n", texto.Split('\n').Where(l => !l.TrimStart().StartsWith("--")));
        foreach (var bloco in Regex.Split(semComentarios, @";\s*\n"))
        {
            var sql = bloco.Trim().TrimEnd(';').Trim();
            if (sql.Length == 0) continue;
            await ExecSqlAsync(conn, sql);
        }
    }
}

// Fábrica da API apontando para cartao_sus_test. Uma por teste: o anti-reuso do TOTP é em memória.
public class ApiFactory : WebApplicationFactory<Program>
{
    public SusFakeHandler Sus { get; } = new();

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.UseSetting("ConnectionStrings:Default", AmbienteTeste.ConexaoApp());
        builder.UseSetting("JWT_SECRET", AmbienteTeste.JwtSecret);
        builder.UseSetting("SEED_DEMO", "false");
        builder.UseSetting("SUS_API_BASE", "https://sus.teste.invalido");
        builder.ConfigureTestServices(services =>
        {
            services.AddHttpClient<SusApiClient>().ConfigurePrimaryHttpMessageHandler(() => Sus);
        });
    }
}

// Substitui a API pública do SUS nos testes: nenhum teste chama o servidor real.
public class SusFakeHandler : HttpMessageHandler
{
    public Func<HttpRequestMessage, HttpResponseMessage> Responder { get; set; } =
        _ => new HttpResponseMessage(HttpStatusCode.ServiceUnavailable);

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct) =>
        Task.FromResult(Responder(request));
}
