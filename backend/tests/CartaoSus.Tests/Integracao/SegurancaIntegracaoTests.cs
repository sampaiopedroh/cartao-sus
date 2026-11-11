using System.Net;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class SegurancaIntegracaoTests : IntegracaoBase
{
    [Fact(DisplayName = "IT-10a rota protegida sem token retorna 401")]
    public async Task IT10a_SemTokenRetorna401()
    {
        var resp = await Cliente.GetAsync("/api/v1/cidadao/1/prontuario");
        Assert.Equal(HttpStatusCode.Unauthorized, resp.StatusCode);
        Assert.Equal("NAO_AUTORIZADO", (await JsonAsync(resp)).GetProperty("erro").GetString());
    }

    [Fact(DisplayName = "IT-10b cidadão em rota de admin retorna 403")]
    public async Task IT10b_CidadaoEmRotaDeAdminRetorna403()
    {
        Autenticar(await LoginAsync("maria.souza@cartaosus.demo"));
        var resp = await Cliente.GetAsync("/api/v1/admin/auditoria");
        Assert.Equal(HttpStatusCode.Forbidden, resp.StatusCode);
        Assert.Equal("SEM_PERMISSAO", (await JsonAsync(resp)).GetProperty("erro").GetString());
    }
}
