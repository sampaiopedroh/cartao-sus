using System.Net;
using System.Net.Http.Json;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class AcolhimentoIntegracaoTests : IntegracaoBase
{
    private const string CnsMaria = "898000000000000";
    private const string CnsJoao = "182345678901234";
    private const string CnesUbs = "9186492";
    private const string CnesUpa = "8530378";

    [Fact(DisplayName = "IT-03 handshake com TOTP válido grava sessão AUTORIZADA")]
    public async Task IT03_HandshakeAutorizaEGravaSessao()
    {
        Dispositivo(AmbienteTeste.ChaveTerminalUbs);
        var antes = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_SESSAO_ACESSO WHERE ID_CIDADAO = 1 AND STATUS_AUTENTICACAO = 'AUTORIZADO'");

        var resp = await Cliente.PostAsJsonAsync("/api/v1/acolhimento/validar-nfc",
            new { numeroCns = CnsMaria, codigoCnesUnidade = CnesUbs, tokenRotativo = CodigoAgora(AmbienteTeste.SegredoMaria) });

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        Assert.Equal("AUTORIZADO", (await JsonAsync(resp)).GetProperty("status").GetString());

        var depois = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_SESSAO_ACESSO WHERE ID_CIDADAO = 1 AND STATUS_AUTENTICACAO = 'AUTORIZADO'");
        Assert.Equal(antes + 1, depois);
    }

    [Fact(DisplayName = "IT-04 três falhas bloqueiam a quarta tentativa e geram alerta")]
    public async Task IT04_TresFalhasBloqueiamEGeramAlerta()
    {
        Dispositivo(AmbienteTeste.ChaveTerminalUpa);
        var alertasAntes = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_ALERTA WHERE ID_CIDADAO = 2 AND TIPO_ALERTA = 'TENTATIVA_INDEVIDA'");

        for (int i = 0; i < 3; i++)
        {
            var falha = await Cliente.PostAsJsonAsync("/api/v1/acolhimento/validar-nfc",
                new { numeroCns = CnsJoao, codigoCnesUnidade = CnesUpa, tokenRotativo = ErradoPara(AmbienteTeste.SegredoJoao, i) });
            Assert.Equal(HttpStatusCode.Forbidden, falha.StatusCode);
            Assert.Equal("TOKEN_INVALIDO", (await JsonAsync(falha)).GetProperty("erro").GetString());
        }

        // Quarta tentativa com o código certo: bloqueada mesmo assim.
        var quarta = await Cliente.PostAsJsonAsync("/api/v1/acolhimento/validar-nfc",
            new { numeroCns = CnsJoao, codigoCnesUnidade = CnesUpa, tokenRotativo = CodigoAgora(AmbienteTeste.SegredoJoao) });
        Assert.Equal(HttpStatusCode.Forbidden, quarta.StatusCode);
        Assert.Equal("BLOQUEADO", (await JsonAsync(quarta)).GetProperty("erro").GetString());

        var alertasDepois = await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_ALERTA WHERE ID_CIDADAO = 2 AND TIPO_ALERTA = 'TENTATIVA_INDEVIDA'");
        Assert.Equal(alertasAntes + 1, alertasDepois);
    }

    // Código de janela diferente do atual, sempre errado para o segredo.
    private static string ErradoPara(string segredo, int i)
    {
        var certo = CodigoAgora(segredo);
        for (int n = 0; ; n++)
        {
            var candidato = (100000 + ((n + i * 7919) % 900000)).ToString();
            if (candidato != certo && candidato != CodigoAgora(segredo, -1) && candidato != CodigoAgora(segredo, 1))
                return candidato;
        }
    }
}
