using System.Net;
using System.Net.Http.Json;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class TelemetriaIntegracaoTests : IntegracaoBase
{
    [Fact(DisplayName = "IT-05 tensiômetro acima do limiar grava alerta e origem IOT")]
    public async Task IT05_TensiometroCriticoGravaAlertaEOrigemIot()
    {
        Dispositivo(AmbienteTeste.ChaveTensiometro);
        var alertasAntes = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_ALERTA WHERE ID_CIDADAO = 2 AND TIPO_ALERTA = 'PRESSAO_CRITICA'");

        var resp = await Cliente.PostAsJsonAsync("/api/v1/telemetria/sinais-vitais",
            new { tipoMedicao = "PRESSAO_SISTOLICA", valor = 185.0 });

        Assert.Equal(HttpStatusCode.Created, resp.StatusCode);
        var json = await JsonAsync(resp);
        Assert.True(json.GetProperty("alertaDisparado").GetBoolean());
        var idSinal = json.GetProperty("idSinal").GetInt64();

        Assert.Equal("IOT", await BancoTesteSetup.TextoAsync(
            "SELECT ORIGEM FROM TB_SINAL_VITAL WHERE ID_SINAL = @id", ("@id", idSinal)));
        var alertasDepois = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_ALERTA WHERE ID_CIDADAO = 2 AND TIPO_ALERTA = 'PRESSAO_CRITICA'");
        Assert.Equal(alertasAntes + 1, alertasDepois);
    }

    [Fact(DisplayName = "IT-06 medição enviada pelo cidadão grava origem APP")]
    public async Task IT06_MedicaoDoAppGravaOrigemApp()
    {
        var token = await LoginAsync("joao.pereira@cartaosus.demo");
        Autenticar(token);

        var resp = await Cliente.PostAsJsonAsync("/api/v1/telemetria/sinais-vitais",
            new { tipoMedicao = "PESO", valor = 91.4 });

        Assert.Equal(HttpStatusCode.Created, resp.StatusCode);
        var idSinal = (await JsonAsync(resp)).GetProperty("idSinal").GetInt64();
        Assert.Equal("APP", await BancoTesteSetup.TextoAsync(
            "SELECT ORIGEM FROM TB_SINAL_VITAL WHERE ID_SINAL = @id", ("@id", idSinal)));
    }
}
