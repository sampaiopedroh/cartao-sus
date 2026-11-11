using CartaoSus.Api.Security;

namespace CartaoSus.Tests.Unitarios;

public class TotpTests
{
    // Segredo de teste da RFC 6238 ("12345678901234567890" em Base32).
    private const string SegredoRfc = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    [Fact(DisplayName = "UT-01 código válido no passo atual é aceito")]
    public void UT01_CodigoValidoNoPassoAtualEhAceito()
    {
        // Vetor oficial da RFC 6238: instante 59 s, SHA-1 → 287082 (6 dígitos).
        var agoraRfc = DateTimeOffset.FromUnixTimeSeconds(59);
        Assert.Equal("287082", TotpService.Gerar(SegredoRfc, TotpService.Contador(agoraRfc)));
        Assert.Equal(1L, TotpService.ValidarJanela(SegredoRfc, "287082", agoraRfc));

        // Código gerado para um instante qualquer também é aceito no mesmo instante.
        var agora = DateTimeOffset.FromUnixTimeSeconds(1_700_000_000);
        var codigo = TotpService.Gerar(SegredoRfc, TotpService.Contador(agora));
        Assert.NotNull(TotpService.ValidarJanela(SegredoRfc, codigo, agora));
    }

    [Fact(DisplayName = "UT-02 código de 5 minutos atrás é recusado")]
    public void UT02_CodigoDe5MinutosAtrasEhRecusado()
    {
        var agora = DateTimeOffset.FromUnixTimeSeconds(1_700_000_000);
        var antigo = TotpService.Gerar(SegredoRfc, TotpService.Contador(agora.AddMinutes(-5)));
        Assert.Null(TotpService.ValidarJanela(SegredoRfc, antigo, agora));
    }

    [Fact(DisplayName = "UT-03 mesmo código usado duas vezes é recusado na segunda")]
    public void UT03_CodigoReutilizadoEhRecusado()
    {
        var anti = new AntiReusoTotp();
        var agora = DateTimeOffset.FromUnixTimeSeconds(1_700_000_000);
        var contador = TotpService.ValidarJanela(SegredoRfc, TotpService.Gerar(SegredoRfc, TotpService.Contador(agora)), agora);
        Assert.NotNull(contador);

        Assert.True(anti.Registrar(7, contador!.Value));
        Assert.False(anti.Registrar(7, contador.Value));
        // Outro cidadão não é afetado pelo uso do primeiro.
        Assert.True(anti.Registrar(8, contador.Value));
    }

    [Fact(DisplayName = "Código com tamanho errado é recusado sem calcular HMAC")]
    public void CodigoComTamanhoErradoEhRecusado()
    {
        var agora = DateTimeOffset.FromUnixTimeSeconds(1_700_000_000);
        Assert.Null(TotpService.ValidarJanela(SegredoRfc, "12345", agora));
    }
}
