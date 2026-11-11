using System.Security.Claims;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Services;
using Moq;

namespace CartaoSus.Tests.Unitarios;

public class TelemetriaTests
{
    private readonly Mock<ICidadaoRepository> _cidadaos = new();
    private readonly Mock<ISinalVitalRepository> _sinais = new();
    private readonly Mock<IAlertaRepository> _alertas = new();

    private TelemetriaService Servico() => new(_cidadaos.Object, _sinais.Object, _alertas.Object);

    private static ClaimsPrincipal Tensiometro(long idCidadao) => new(new ClaimsIdentity(
        [new Claim(ClaimTypes.Role, "DISPOSITIVO_TENSIOMETRO"), new Claim("idCidadao", idCidadao.ToString())], "teste"));

    [Fact(DisplayName = "UT-04 sistólica de 185 gera alerta PRESSAO_CRITICA")]
    public async Task UT04_PressaoSistolicaCriticaGeraAlerta()
    {
        var resultado = await Servico().IngerirAsync(
            new SinalVitalRequest { TipoMedicao = "PRESSAO_SISTOLICA", Valor = 185m }, Tensiometro(2));

        Assert.True(resultado.AlertaDisparado);
        Assert.Equal("PRESSAO_CRITICA", resultado.Alerta!.Tipo);
        _alertas.Verify(a => a.AdicionarAsync(It.Is<Alerta>(x => x.TipoAlerta == "PRESSAO_CRITICA" && x.Severidade == "CRITICA")), Times.Once);
    }

    [Fact(DisplayName = "UT-05 pressão normal de 120 não gera alerta")]
    public async Task UT05_PressaoNormalNaoGeraAlerta()
    {
        var resultado = await Servico().IngerirAsync(
            new SinalVitalRequest { TipoMedicao = "PRESSAO_SISTOLICA", Valor = 120m }, Tensiometro(2));

        Assert.False(resultado.AlertaDisparado);
        Assert.Null(resultado.Alerta);
        _alertas.Verify(a => a.AdicionarAsync(It.IsAny<Alerta>()), Times.Never);
    }

    [Fact(DisplayName = "UT-06 glicemia de 310 gera alerta GLICEMIA_CRITICA")]
    public async Task UT06_GlicemiaCriticaGeraAlerta()
    {
        var principal = new ClaimsPrincipal(new ClaimsIdentity(
            [new Claim(ClaimTypes.Role, "CIDADAO"), new Claim("idCidadao", "1")], "teste"));

        var resultado = await Servico().IngerirAsync(
            new SinalVitalRequest { TipoMedicao = "GLICEMIA", Valor = 310m }, principal);

        Assert.True(resultado.AlertaDisparado);
        Assert.Equal("GLICEMIA_CRITICA", resultado.Alerta!.Tipo);
        _alertas.Verify(a => a.AdicionarAsync(It.Is<Alerta>(x => x.TipoAlerta == "GLICEMIA_CRITICA")), Times.Once);
    }

    [Fact(DisplayName = "Tensiômetro com CNS de outra pessoa é recusado")]
    public async Task TensiometroComCnsDeOutroEhRecusado()
    {
        _cidadaos.Setup(c => c.PorCnsAsync("898000000000000"))
            .ReturnsAsync(new Cidadao { IdCidadao = 1, NumeroCns = "898000000000000" });

        await Assert.ThrowsAsync<SemPermissaoException>(() => Servico().IngerirAsync(
            new SinalVitalRequest { TipoMedicao = "PRESSAO_SISTOLICA", Valor = 120m, NumeroCns = "898000000000000" },
            Tensiometro(2)));
    }
}
