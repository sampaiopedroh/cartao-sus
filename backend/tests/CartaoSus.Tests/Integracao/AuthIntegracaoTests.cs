using System.Net;
using System.Net.Http.Json;
using MySqlConnector;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class AuthIntegracaoTests : IntegracaoBase
{
    [Fact(DisplayName = "IT-01 cadastro, login e prontuário do novo cidadão")]
    public async Task IT01_CadastroLoginProntuario()
    {
        var cad = await Cliente.PostAsJsonAsync("/api/v1/auth/cadastro", new
        {
            login = "novo.it01@cartaosus.demo", senha = "SenhaForte123", cpf = "52998224725",
            numeroCns = "700000000000001", nomeCompleto = "Novo Cidadao IT01", dataNascimento = "1990-05-20"
        });
        Assert.Equal(HttpStatusCode.Created, cad.StatusCode);
        var cadJson = await JsonAsync(cad);
        var idCidadao = cadJson.GetProperty("idCidadao").GetInt64();

        var login = await Cliente.PostAsJsonAsync("/api/v1/auth/login",
            new { login = "novo.it01@cartaosus.demo", senha = "SenhaForte123" });
        Assert.Equal(HttpStatusCode.OK, login.StatusCode);
        var token = (await JsonAsync(login)).GetProperty("token").GetString()!;

        Autenticar(token);
        var pront = await Cliente.GetAsync($"/api/v1/cidadao/{idCidadao}/prontuario");
        Assert.Equal(HttpStatusCode.OK, pront.StatusCode);

        // Efeito no banco: o cidadão e o usuário existem, e o CPF ficou só como hash.
        var n = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_CIDADAO c JOIN TB_USUARIO u ON u.ID_USUARIO = c.ID_USUARIO WHERE c.ID_CIDADAO = @id AND u.LOGIN = @l AND LENGTH(c.CPF_HASH) = 64",
            ("@id", idCidadao), ("@l", "novo.it01@cartaosus.demo"));
        Assert.Equal(1, n);
    }

    [Fact(DisplayName = "IT-02 usuário inativo não consegue logar")]
    public async Task IT02_UsuarioInativoNaoLoga()
    {
        // Desativa o João direto no banco de testes.
        await using var c = new MySqlConnector.MySqlConnection(AmbienteTeste.ConexaoRoot() + ";Database=" + AmbienteTeste.BancoTeste);
        await c.OpenAsync();
        await BancoTesteSetup.ExecSqlAsync(c, "UPDATE TB_USUARIO SET ATIVO = 0 WHERE LOGIN = 'joao.pereira@cartaosus.demo'");

        var resp = await Cliente.PostAsJsonAsync("/api/v1/auth/login",
            new { login = "joao.pereira@cartaosus.demo", senha = AmbienteTeste.SenhaDemo });
        Assert.Equal(HttpStatusCode.Unauthorized, resp.StatusCode);
        Assert.Equal("NAO_AUTORIZADO", (await JsonAsync(resp)).GetProperty("erro").GetString());
    }
}
