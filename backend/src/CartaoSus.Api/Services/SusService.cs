using System.Globalization;
using System.Text;
using System.Text.Json;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;

namespace CartaoSus.Api.Services;

// Cliente HTTP das APIs públicas do SUS. Timeout de 5 s configurado no Program.cs; uma nova tentativa.
public class SusApiClient(HttpClient http, ILogger<SusApiClient> log)
{
    public async Task<JsonDocument> GetJsonAsync(string caminho)
    {
        Exception? ultimo = null;
        for (int tentativa = 1; tentativa <= 2; tentativa++)
        {
            try
            {
                using var resp = await http.GetAsync(caminho);
                if (resp.IsSuccessStatusCode)
                    return JsonDocument.Parse(await resp.Content.ReadAsStringAsync());
                ultimo = new HttpRequestException($"Status {(int)resp.StatusCode}");
            }
            catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException or JsonException)
            {
                ultimo = ex;
            }
            log.LogWarning("Falha na API do SUS (tentativa {N}): {Erro}", tentativa, ultimo?.Message);
        }
        throw new FalhaOrigemException();
    }
}

public interface ISusService
{
    Task<EquipamentosResposta> EquipamentosAsync(string cnes);
    Task<PaginaResposta<MedicamentoItem>> MedicamentosAsync(string municipio, string catmat, int page, int size);
    IReadOnlyList<CatalogoItem> Catalogo(string? nome);
}

public class SusService(SusApiClient client, CatalogoMedicamentos catalogo) : ISusService
{
    public async Task<EquipamentosResposta> EquipamentosAsync(string cnes)
    {
        using var doc = await client.GetJsonAsync(
            $"/assistencia-a-saude/cnes-equipamentos?co_cnes={cnes}&limit=1000");
        var itens = doc.RootElement.GetProperty("cnes_equipamentos").EnumerateArray().ToList();
        if (itens.Count == 0)
            throw new NaoEncontradoException("Unidade sem registro de equipamentos no CNES.");

        var lista = itens.Select(e => new EquipamentoItem(
            Texto(e, "tp_equipamento") ?? "",
            Texto(e, "ds_equipamento") ?? "",
            Inteiro(e, "qt_existente"),
            Inteiro(e, "qt_uso"))).ToList();
        return new EquipamentosResposta(cnes, Texto(itens[0], "no_fantasia"), lista);
    }

    public async Task<PaginaResposta<MedicamentoItem>> MedicamentosAsync(string municipio, string catmat, int page, int size)
    {
        using var doc = await client.GetJsonAsync(
            $"/daf/estoque-medicamentos-bnafar-horus?codigo_municipio={municipio}&codigo_catmat={Uri.EscapeDataString(catmat)}&limit={size}&offset={page * size}");
        var parametros = doc.RootElement.GetProperty("parametros").EnumerateArray().ToList();
        if (parametros.Count == 0 && page == 0)
            throw new NaoEncontradoException("Nenhum estoque encontrado para este medicamento no município.");

        var nomeCatalogo = catalogo.NomePorCatmat(catmat);
        var lista = parametros.Select(p => new MedicamentoItem(
            nomeCatalogo ?? Texto(p, "descricao_produto") ?? "Medicamento sem descrição",
            Texto(p, "codigo_catmat") ?? catmat,
            Texto(p, "municipio"),
            Texto(p, "nome_fantasia"),
            Decimal(p, "quantidade_estoque"),
            Texto(p, "data_posicao_estoque"),
            Decimal(p, "latitude"),
            Decimal(p, "longitude"))).ToList();

        // A API não devolve total: totalElements = registros até aqui (documentado no README).
        long total = page * size + lista.Count;
        return new PaginaResposta<MedicamentoItem>(lista, page, size, total);
    }

    public IReadOnlyList<CatalogoItem> Catalogo(string? nome) => catalogo.Buscar(nome);

    private static string? Texto(JsonElement e, string prop) =>
        e.TryGetProperty(prop, out var v) && v.ValueKind == JsonValueKind.String ? v.GetString()?.Trim()
        : e.TryGetProperty(prop, out var n) && n.ValueKind == JsonValueKind.Number ? n.ToString()
        : null;

    private static int? Inteiro(JsonElement e, string prop) =>
        e.TryGetProperty(prop, out var v) && v.ValueKind == JsonValueKind.Number && v.TryGetInt32(out var i) ? i : null;

    private static decimal? Decimal(JsonElement e, string prop) =>
        e.TryGetProperty(prop, out var v) && v.ValueKind == JsonValueKind.Number ? v.GetDecimal() : null;
}

// Catálogo local (SDD 5.3): nome de busca → CATMAT, validado nas planilhas oficiais.
public class CatalogoMedicamentos
{
    private readonly List<CatalogoItem> _itens;

    public CatalogoMedicamentos()
    {
        var caminho = Path.Combine(AppContext.BaseDirectory, "Data", "catalogo-medicamentos.json");
        using var doc = JsonDocument.Parse(File.ReadAllText(caminho, Encoding.UTF8));
        _itens = doc.RootElement.EnumerateArray().Select(e => new CatalogoItem(
            e.GetProperty("nome").GetString()!,
            e.GetProperty("apresentacao").GetString()!,
            e.GetProperty("catmat").GetString()!)).ToList();
    }

    public string? NomePorCatmat(string catmat) =>
        _itens.FirstOrDefault(i => i.Catmat == catmat)?.Nome;

    public IReadOnlyList<CatalogoItem> Buscar(string? nome)
    {
        if (string.IsNullOrWhiteSpace(nome)) return _itens;
        var termo = Normalizar(nome);
        return _itens.Where(i => Normalizar(i.Nome).Contains(termo)).ToList();
    }

    private static string Normalizar(string s)
    {
        var sem = new string(s.Normalize(NormalizationForm.FormD)
            .Where(c => CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark).ToArray());
        return sem.ToUpperInvariant().Trim();
    }
}
