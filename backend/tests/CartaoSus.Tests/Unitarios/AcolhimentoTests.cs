using System.Security.Claims;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Security;
using CartaoSus.Api.Services;
using Moq;

namespace CartaoSus.Tests.Unitarios;

public class AcolhimentoTests
{
    private const string Segredo = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private const string Cns = "898000000000000";
    private const string CnesUbs = "9186492";

    private readonly Mock<ICidadaoRepository> _cidadaos = new();
    private readonly Mock<IUnidadeRepository> _unidades = new();
    private readonly Mock<ISessaoRepository> _sessoes = new();
    private readonly Mock<IAlertaRepository> _alertas = new();
    private readonly Mock<IDocumentoRepository> _documentos = new();
    private readonly Mock<IHistoricoRepository> _historicos = new();
    private readonly Mock<ISinalVitalRepository> _sinais = new();

    public AcolhimentoTests()
    {
        _unidades.Setup(u => u.PorCnesAsync(CnesUbs)).ReturnsAsync(new UnidadeSaude { IdUnidade = 1, CodigoCnes = CnesUbs, NomeEstabelecimento = "UBS Teste" });
        _cidadaos.Setup(c => c.PorCnsAsync(Cns)).ReturnsAsync(new Cidadao { IdCidadao = 1, NumeroCns = Cns, SegredoTotp = Segredo, NomeCompleto = "Maria" });
        _documentos.Setup(d => d.DoCidadaoAsync(1)).ReturnsAsync(new List<CarteiraMetadados>());
        _historicos.Setup(h => h.DoCidadaoAsync(1)).ReturnsAsync(new List<HistoricoClinico>());
        _sinais.Setup(s => s.ListarAsync(1, null, null, null)).ReturnsAsync(new List<SinalVital>());
    }

    private AcolhimentoService Servico() => new(
        _cidadaos.Object, _unidades.Object, _sessoes.Object, _alertas.Object,
        _documentos.Object, _historicos.Object, _sinais.Object, new AntiReusoTotp());

    private static ClaimsPrincipal Terminal() => new(new ClaimsIdentity(
        [new Claim(ClaimTypes.Role, "DISPOSITIVO_TERMINAL_NFC"), new Claim("idUnidade", "1")], "teste"));

    private static ValidarNfcRequest Req(string token) =>
        new() { NumeroCns = Cns, CodigoCnesUnidade = CnesUbs, TokenRotativo = token };

    [Fact(DisplayName = "UT-07 três falhas bloqueiam a próxima tentativa; a 3ª gera TENTATIVA_INDEVIDA")]
    public async Task UT07_TresFalhasBloqueiamEGeramAlerta()
    {
        // Terceira falha: 2 rejeições anteriores + token errado.
        _sessoes.Setup(s => s.ContarRejeicoesDesdeAsync(1, It.IsAny<DateTime>())).ReturnsAsync(2);
        await Assert.ThrowsAsync<TokenInvalidoException>(() => Servico().ValidarNfcAsync(Req("000000"), Terminal()));
        _alertas.Verify(a => a.AdicionarAsync(It.Is<Alerta>(x => x.TipoAlerta == "TENTATIVA_INDEVIDA")), Times.Once);

        // Quarta tentativa: 3 rejeições na janela → bloqueio, mesmo com o código certo.
        _sessoes.Setup(s => s.ContarRejeicoesDesdeAsync(1, It.IsAny<DateTime>())).ReturnsAsync(3);
        var certo = TotpService.Gerar(Segredo, TotpService.Contador(DateTimeOffset.UtcNow));
        await Assert.ThrowsAsync<BloqueadoException>(() => Servico().ValidarNfcAsync(Req(certo), Terminal()));
        _sessoes.Verify(s => s.AdicionarAsync(It.Is<SessaoAcesso>(x => x.StatusAutenticacao == "AUTORIZADO")), Times.Never);
    }

    [Fact(DisplayName = "UT-08 duas falhas seguidas e depois um código válido autorizam")]
    public async Task UT08_SucessoAposDuasFalhas()
    {
        _sessoes.Setup(s => s.ContarRejeicoesDesdeAsync(1, It.IsAny<DateTime>())).ReturnsAsync(2);
        var valido = TotpService.Gerar(Segredo, TotpService.Contador(DateTimeOffset.UtcNow));

        var resposta = await Servico().ValidarNfcAsync(Req(valido), Terminal());

        Assert.Equal("AUTORIZADO", resposta.Status);
        Assert.Equal("Maria", resposta.Paciente.NomeCompleto);
        _sessoes.Verify(s => s.AdicionarAsync(It.Is<SessaoAcesso>(x => x.StatusAutenticacao == "AUTORIZADO")), Times.Once);
    }

    [Fact(DisplayName = "Terminal de outra unidade é recusado")]
    public async Task TerminalDeOutraUnidadeEhRecusado()
    {
        var terminalOutro = new ClaimsPrincipal(new ClaimsIdentity(
            [new Claim(ClaimTypes.Role, "DISPOSITIVO_TERMINAL_NFC"), new Claim("idUnidade", "2")], "teste"));
        await Assert.ThrowsAsync<SemPermissaoException>(() => Servico().ValidarNfcAsync(Req("123456"), terminalOutro));
    }
}
