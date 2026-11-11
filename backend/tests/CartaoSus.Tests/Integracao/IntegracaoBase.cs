using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using CartaoSus.Api.Security;

namespace CartaoSus.Tests.Integracao;

// Base comum: cada teste recebe banco recriado e uma API nova.
public abstract class IntegracaoBase : IAsyncLifetime
{
    protected ApiFactory Fabrica = null!;
    protected HttpClient Cliente = null!;

    public async Task InitializeAsync()
    {
        await BancoTesteSetup.RecriarAsync();
        Fabrica = new ApiFactory();
        Cliente = Fabrica.CreateClient();
    }

    public async Task DisposeAsync()
    {
        Cliente.Dispose();
        await Fabrica.DisposeAsync();
    }

    protected async Task<string> LoginAsync(string login)
    {
        var resp = await Cliente.PostAsJsonAsync("/api/v1/auth/login", new { login, senha = AmbienteTeste.SenhaDemo });
        Assert.Equal(System.Net.HttpStatusCode.OK, resp.StatusCode);
        var json = await JsonDocument.ParseAsync(await resp.Content.ReadAsStreamAsync());
        return json.RootElement.GetProperty("token").GetString()!;
    }

    protected void Autenticar(string token) =>
        Cliente.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

    protected void Dispositivo(string chave)
    {
        Cliente.DefaultRequestHeaders.Remove("X-API-KEY");
        Cliente.DefaultRequestHeaders.Add("X-API-KEY", chave);
    }

    protected static string CodigoAgora(string segredo, int deslocamento = 0) =>
        TotpService.Gerar(segredo, TotpService.Contador(DateTimeOffset.UtcNow, deslocamento));

    protected static async Task<JsonElement> JsonAsync(HttpResponseMessage resp) =>
        JsonDocument.Parse(await resp.Content.ReadAsStringAsync()).RootElement.Clone();
}
