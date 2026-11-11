using System.Net;
using System.Security.Claims;
using System.Text.Json;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Controllers;
using CartaoSus.Api.Middleware;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace CartaoSus.Tests.Contrato;

// Testes de contrato (SDD 6.3): o formato das respostas que o app e os sensores consomem
// é comparado, campo a campo, com os snapshots em Contrato/Snapshots. Valores não são comparados.
public class ContratoTests
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    private static JsonElement Serializar<T>(T valor) =>
        JsonDocument.Parse(JsonSerializer.Serialize(valor, Json)).RootElement.Clone();

    private static JsonElement Snapshot(string nome)
    {
        var caminho = Path.Combine(AppContext.BaseDirectory, "Contrato", "Snapshots", nome);
        return JsonDocument.Parse(File.ReadAllText(caminho)).RootElement.Clone();
    }

    private static void AssertFormato(JsonElement real, JsonElement modelo, string caminho = "$")
    {
        switch (modelo.ValueKind)
        {
            case JsonValueKind.Object:
                Assert.True(real.ValueKind == JsonValueKind.Object, $"{caminho}: esperado objeto");
                var chavesEsperadas = modelo.EnumerateObject().Select(p => p.Name).OrderBy(x => x).ToList();
                var chavesReais = real.EnumerateObject().Select(p => p.Name).OrderBy(x => x).ToList();
                Assert.True(chavesEsperadas.SequenceEqual(chavesReais),
                    $"{caminho}: campos esperados [{string.Join(",", chavesEsperadas)}] mas vieram [{string.Join(",", chavesReais)}]");
                foreach (var prop in modelo.EnumerateObject())
                    AssertFormato(real.GetProperty(prop.Name), prop.Value, $"{caminho}.{prop.Name}");
                break;

            case JsonValueKind.Array:
                Assert.True(real.ValueKind == JsonValueKind.Array, $"{caminho}: esperado array");
                if (modelo.GetArrayLength() == 1)
                    foreach (var item in real.EnumerateArray())
                        AssertFormato(item, modelo[0], $"{caminho}[]");
                break;

            case JsonValueKind.String:
                var permitidos = modelo.GetString()!.Split('|');
                var tipo = TipoDe(real);
                Assert.True(permitidos.Contains(tipo),
                    $"{caminho}: tipo {tipo} não permitido (esperado {string.Join(" ou ", permitidos)})");
                break;
        }
    }

    private static string TipoDe(JsonElement e) => e.ValueKind switch
    {
        JsonValueKind.String => "string",
        JsonValueKind.Number => "number",
        JsonValueKind.True or JsonValueKind.False => "boolean",
        JsonValueKind.Null => "null",
        JsonValueKind.Object => "object",
        JsonValueKind.Array => "array",
        _ => "desconhecido"
    };

    // Executa o middleware de erro e devolve o status e o corpo JSON do envelope.
    private static async Task<(int Status, JsonElement Corpo)> EnvelopeDe(Exception erro)
    {
        var ctx = new DefaultHttpContext();
        ctx.Response.Body = new MemoryStream();
        var middleware = new ErrorHandlingMiddleware(_ => throw erro,
            Microsoft.Extensions.Logging.Abstractions.NullLogger<ErrorHandlingMiddleware>.Instance);
        await middleware.InvokeAsync(ctx);
        ctx.Response.Body.Seek(0, SeekOrigin.Begin);
        var corpo = JsonDocument.Parse(ctx.Response.Body).RootElement.Clone();
        return (ctx.Response.StatusCode, corpo);
    }

    [Fact(DisplayName = "CT-01 login 200: formato do token e do perfil")]
    public void CT01_LoginFormato()
    {
        var resposta = new LoginResponse("eyJ.exemplo", "Bearer", 30, "CIDADAO", 1, "Maria Souza Lima");
        AssertFormato(Serializar(resposta), Snapshot("login-200.json"));
    }

    [Fact(DisplayName = "CT-02 login 401: envelope de erro com NAO_AUTORIZADO")]
    public async Task CT02_LoginNaoAutorizadoEnvelope()
    {
        var (status, corpo) = await EnvelopeDe(new CredenciaisInvalidasException());
        Assert.Equal(401, status);
        AssertFormato(corpo, Snapshot("erro-envelope.json"));
        Assert.Equal("NAO_AUTORIZADO", corpo.GetProperty("erro").GetString());
    }

    [Fact(DisplayName = "CT-03 prontuário 200: listas de histórico e alertas")]
    public void CT03_ProntuarioFormato()
    {
        var resposta = new ProntuarioResposta(1, "Maria Souza Lima", "898000000000000",
            [new ProntuarioHistorico("ALERGIA", "Penicilina", "CRITICO", new DateOnly(2024, 5, 10))],
            [new AlertaResumo(3, "PRESSAO_CRITICA", "CRITICA", "Procure a UPA", "PENDENTE", DateTime.Now)]);
        AssertFormato(Serializar(resposta), Snapshot("prontuario-200.json"));
    }

    [Fact(DisplayName = "CT-04 prontuário 403: envelope com SEM_PERMISSAO")]
    public async Task CT04_ProntuarioSemPermissaoEnvelope()
    {
        var (status, corpo) = await EnvelopeDe(new SemPermissaoException("Acesso negado aos dados de outro cidadão."));
        Assert.Equal(403, status);
        AssertFormato(corpo, Snapshot("erro-envelope.json"));
        Assert.Equal("SEM_PERMISSAO", corpo.GetProperty("erro").GetString());
    }

    [Fact(DisplayName = "CT-05 validar NFC 200: paciente com alertas e últimas medições")]
    public void CT05_ValidarNfcFormato()
    {
        var resposta = new ValidarNfcResponse("AUTORIZADO", "Identificação e autorização validadas com sucesso.",
            new PacienteResumo("Maria Souza Lima", "898000000000000", ["RG", "CPF"],
                [new AlertaCritico("ALERGIA", "Penicilina", "CRITICO")],
                new UltimasMedicoes(112m, "124/80", new DateTime(2026, 11, 9, 7, 15, 0))));
        AssertFormato(Serializar(resposta), Snapshot("validar-nfc-200.json"));
    }

    [Fact(DisplayName = "CT-06 validar NFC 403: envelope com TOKEN_INVALIDO")]
    public async Task CT06_ValidarNfcTokenInvalidoEnvelope()
    {
        var (status, corpo) = await EnvelopeDe(new TokenInvalidoException());
        Assert.Equal(403, status);
        AssertFormato(corpo, Snapshot("erro-envelope.json"));
        Assert.Equal("TOKEN_INVALIDO", corpo.GetProperty("erro").GetString());
    }

    [Fact(DisplayName = "CT-07 telemetria 201: alertaDisparado e alerta")]
    public void CT07_TelemetriaFormato()
    {
        var resposta = new SinalVitalResposta(10, true, new AlertaDisparado("PRESSAO_CRITICA", "Alerta de Pressão Crítica: Procure a UPA mais próxima"));
        AssertFormato(Serializar(resposta), Snapshot("telemetria-201.json"));
    }

    [Fact(DisplayName = "CT-08 excluir documento 204: sem corpo de resposta")]
    public async Task CT08_ExcluirDocumentoSemCorpo()
    {
        var servico = new Mock<IDocumentoService>();
        var controller = new DocumentosController(servico.Object);

        var resultado = await controller.Excluir(5);

        Assert.IsType<NoContentResult>(resultado);
    }
}
