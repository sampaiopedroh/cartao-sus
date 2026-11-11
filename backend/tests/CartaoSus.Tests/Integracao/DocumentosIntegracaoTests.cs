using System.Net;
using System.Net.Http.Json;

namespace CartaoSus.Tests.Integracao;

[Trait("Category", "Integracao")]
public class DocumentosIntegracaoTests : IntegracaoBase
{
    [Fact(DisplayName = "IT-07 documento criado aparece no handshake e some após a exclusão")]
    public async Task IT07_DocumentoCriadoEExcluidoAparecemNoHandshake()
    {
        var token = await LoginAsync("joao.pereira@cartaosus.demo");
        Autenticar(token);
        var hash = new string('c', 64);

        // 1. Cria o documento.
        var criado = await Cliente.PostAsJsonAsync("/api/v1/documentos",
            new { tipoDocumento = "LAUDO", nomeArquivo = "laudo_joao.pdf", hashIntegridade = hash });
        Assert.Equal(HttpStatusCode.Created, criado.StatusCode);
        var idDoc = (await JsonAsync(criado)).GetProperty("idDocumento").GetInt64();
        Assert.Equal(1, await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_CARTEIRA_METADADOS WHERE ID_DOCUMENTO = @id", ("@id", idDoc)));

        // 2. O handshake do João lista o tipo LAUDO entre os documentos disponíveis.
        Dispositivo(AmbienteTeste.ChaveTerminalUpa);
        var com = await Cliente.PostAsJsonAsync("/api/v1/acolhimento/validar-nfc",
            new { numeroCns = "182345678901234", codigoCnesUnidade = "8530378", tokenRotativo = CodigoAgora(AmbienteTeste.SegredoJoao) });
        Assert.Equal(HttpStatusCode.OK, com.StatusCode);
        Assert.Contains("LAUDO", (await JsonAsync(com)).GetProperty("paciente").GetProperty("documentosDisponiveis")
            .EnumerateArray().Select(e => e.GetString()));

        // 3. Exclui o documento (token do próximo passo de 30 s: janela seguinte, sem reuso).
        Autenticar(token);
        Cliente.DefaultRequestHeaders.Remove("X-API-KEY");
        var excluido = await Cliente.DeleteAsync($"/api/v1/documentos/{idDoc}");
        Assert.Equal(HttpStatusCode.NoContent, excluido.StatusCode);
        Assert.Equal(0, await BancoTesteSetup.ContarAsync(
            "SELECT COUNT(*) FROM TB_CARTEIRA_METADADOS WHERE ID_DOCUMENTO = @id", ("@id", idDoc)));

        // 4. Nova leitura do terminal não lista mais o LAUDO. Usa a janela seguinte para não reutilizar código.
        Dispositivo(AmbienteTeste.ChaveTerminalUpa);
        var depois = await Cliente.PostAsJsonAsync("/api/v1/acolhimento/validar-nfc",
            new { numeroCns = "182345678901234", codigoCnesUnidade = "8530378", tokenRotativo = CodigoAgora(AmbienteTeste.SegredoJoao, 1) });
        Assert.Equal(HttpStatusCode.OK, depois.StatusCode);
        Assert.DoesNotContain("LAUDO", (await JsonAsync(depois)).GetProperty("paciente").GetProperty("documentosDisponiveis")
            .EnumerateArray().Select(e => e.GetString()));
    }
}
