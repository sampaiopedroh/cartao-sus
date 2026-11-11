using System.Net;
using System.Text;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class SusIntegracaoTests : IntegracaoBase
{
    private const string EquipamentosOk = """
        {"cnes_equipamentos":[{"co_cnes":"2078015","no_fantasia":"HC TESTE","tp_equipamento":"1 - DIAGNOSTICO POR IMAGEM",
        "ds_equipamento":"04 - Raio X","qt_existente":9,"qt_uso":6,"disp_sus":"SIM"}]}
        """;

    [Fact(DisplayName = "IT-08a proxy do CNES normaliza a resposta do SUS")]
    public async Task IT08a_ProxyNormalizaResposta()
    {
        Fabrica.Sus.Responder = _ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(EquipamentosOk, Encoding.UTF8, "application/json")
        };
        Autenticar(await LoginAsync("maria.souza@cartaosus.demo"));

        var resp = await Cliente.GetAsync("/api/v1/sus/equipamentos/2078015");

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var json = await JsonAsync(resp);
        Assert.Equal("HC TESTE", json.GetProperty("estabelecimento").GetString());
        Assert.Equal(9, json.GetProperty("equipamentos")[0].GetProperty("quantidadeExistente").GetInt32());
    }

    [Fact(DisplayName = "IT-08b SUS indisponível (503) vira 502 FALHA_ORIGEM")]
    public async Task IT08b_SusIndisponivelViraFalhaOrigem()
    {
        Fabrica.Sus.Responder = _ => new HttpResponseMessage(HttpStatusCode.ServiceUnavailable);
        Autenticar(await LoginAsync("maria.souza@cartaosus.demo"));

        var resp = await Cliente.GetAsync("/api/v1/sus/equipamentos/2078015");

        Assert.Equal(HttpStatusCode.BadGateway, resp.StatusCode);
        Assert.Equal("FALHA_ORIGEM", (await JsonAsync(resp)).GetProperty("erro").GetString());
    }
}
