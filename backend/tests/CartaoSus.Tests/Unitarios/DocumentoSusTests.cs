using System.Net;
using System.Security.Claims;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using Moq.Protected;

namespace CartaoSus.Tests.Unitarios;

public class DocumentoTests
{
    [Fact(DisplayName = "UT-11 excluir documento de outro cidadão lança SemPermissaoException")]
    public async Task UT11_ExcluirDocumentoDeOutroEhRecusado()
    {
        var repo = new Mock<IDocumentoRepository>();
        repo.Setup(r => r.PorIdAsync(10)).ReturnsAsync(new CarteiraMetadados { IdDocumento = 10, IdCidadao = 1 });
        var servico = new DocumentoService(repo.Object);
        var quemPede = new ClaimsPrincipal(new ClaimsIdentity(
            [new Claim(System.Security.Claims.ClaimTypes.Role, "CIDADAO"), new Claim("idCidadao", "2")], "teste"));

        await Assert.ThrowsAsync<SemPermissaoException>(() => servico.ExcluirAsync(10, quemPede));
        repo.Verify(r => r.RemoverAsync(It.IsAny<CarteiraMetadados>()), Times.Never);
    }
}

public class SusApiClientTests
{
    [Fact(DisplayName = "UT-12 API do SUS fora do ar (503) vira FalhaOrigemException")]
    public async Task UT12_SusForaDoArViraFalhaOrigem()
    {
        var handler = new Mock<HttpMessageHandler>();
        handler.Protected()
            .Setup<Task<HttpResponseMessage>>("SendAsync", ItExpr.IsAny<HttpRequestMessage>(), ItExpr.IsAny<CancellationToken>())
            // Uma resposta nova a cada tentativa (uma instância só seria descartada na 1ª leitura).
            .Returns(() => Task.FromResult(new HttpResponseMessage(HttpStatusCode.ServiceUnavailable)));
        var http = new HttpClient(handler.Object) { BaseAddress = new Uri("https://sus.teste/") };
        var cliente = new CartaoSus.Api.Services.SusApiClient(http, NullLogger<CartaoSus.Api.Services.SusApiClient>.Instance);

        await Assert.ThrowsAsync<FalhaOrigemException>(() =>
            cliente.GetJsonAsync("/assistencia-a-saude/cnes-equipamentos?co_cnes=1"));
    }
}
