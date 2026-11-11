using System.Net;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class AuditoriaIntegracaoTests : IntegracaoBase
{
    [Fact(DisplayName = "IT-09 admin lista rejeições paginadas, batendo com o banco")]
    public async Task IT09_AdminListaRejeicoesComoOBanco()
    {
        Autenticar(await LoginAsync("admin@cartaosus.demo"));

        var resp = await Cliente.GetAsync("/api/v1/admin/auditoria?status=REJEITADO&page=0&size=2");

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var json = await JsonAsync(resp);
        var total = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_SESSAO_ACESSO WHERE STATUS_AUTENTICACAO = 'REJEITADO'");
        Assert.Equal(total, json.GetProperty("totalElements").GetInt64());
        Assert.Equal(2, json.GetProperty("content").GetArrayLength());
        Assert.Equal(0, json.GetProperty("page").GetInt32());
    }
}
