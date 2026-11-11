using System.Text.RegularExpressions;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Security;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CartaoSus.Api.Controllers;

// Controllers só recebem, validam o formato e delegam aos services (SDD 5.4).

[ApiController]
[Route("api/v1/acolhimento")]
public class AcolhimentoController(IAcolhimentoService acolhimento) : ControllerBase
{
    /// <summary>E3 — Handshake NFC + TOTP. Chamado pelo terminal da recepção (X-API-KEY).</summary>
    [HttpPost("validar-nfc")]
    [Authorize(AuthenticationSchemes = "ApiKey")]
    [ProducesResponseType(typeof(ValidarNfcResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> ValidarNfc([FromBody] ValidarNfcRequest req) =>
        Ok(await acolhimento.ValidarNfcAsync(req, User));
}

[ApiController]
[Route("api/v1/telemetria")]
public class TelemetriaController(ITelemetriaService telemetria) : ControllerBase
{
    /// <summary>E6 — Ingestão de medição: cidadão (JWT) ou tensiômetro (X-API-KEY).</summary>
    [HttpPost("sinais-vitais")]
    [Authorize(AuthenticationSchemes = "Bearer,ApiKey")]
    [ProducesResponseType(typeof(SinalVitalResposta), StatusCodes.Status201Created)]
    public async Task<IActionResult> Ingerir([FromBody] SinalVitalRequest req) =>
        StatusCode(StatusCodes.Status201Created, await telemetria.IngerirAsync(req, User));
}

[ApiController]
[Route("api/v1/cidadao/{id:long}")]
[Authorize]
public class CidadaoController(ICidadaoService cidadao) : ControllerBase
{
    /// <summary>E4 — Prontuário: histórico clínico e alertas pendentes.</summary>
    [HttpGet("prontuario")]
    public async Task<IActionResult> Prontuario(long id) =>
        Ok(await cidadao.ProntuarioAsync(id, User));

    /// <summary>E5 — Sinais vitais filtrados por tipo e período.</summary>
    [HttpGet("sinais-vitais")]
    public async Task<IActionResult> Sinais(long id, [FromQuery] string? tipo,
        [FromQuery] DateOnly? de, [FromQuery] DateOnly? ate) =>
        Ok(await cidadao.SinaisAsync(id, tipo, de, ate, User));

    /// <summary>E7 — Atualiza registro clínico do próprio cidadão.</summary>
    [HttpPut("historico/{idHistorico:long}")]
    public async Task<IActionResult> AtualizarHistorico(long id, long idHistorico, [FromBody] AtualizarHistoricoRequest req) =>
        Ok(await cidadao.AtualizarHistoricoAsync(id, idHistorico, req, User));

    /// <summary>E10 — Lista alertas do cidadão.</summary>
    [HttpGet("alertas")]
    public async Task<IActionResult> Alertas(long id, [FromQuery] string? status,
        [FromQuery] int page = 0, [FromQuery] int size = 20)
    {
        var (p, s) = Paginacao.Validar(page, size);
        return Ok(await cidadao.AlertasAsync(id, status, p, s, User));
    }

    /// <summary>Extra — marca alerta como ENTREGUE (usado pelo app).</summary>
    [HttpPut("alertas/{idAlerta:long}")]
    public async Task<IActionResult> MarcarEntregue(long id, long idAlerta)
    {
        await cidadao.MarcarAlertaEntregueAsync(id, idAlerta, User);
        return NoContent();
    }

    /// <summary>Extra — últimos acessos concedidos (tela 4 do app).</summary>
    [HttpGet("acessos")]
    public async Task<IActionResult> Acessos(long id) =>
        Ok(await cidadao.AcessosAsync(id, User));
}

[ApiController]
[Route("api/v1/documentos")]
[Authorize(Roles = "CIDADAO")]
public class DocumentosController(IDocumentoService documentos) : ControllerBase
{
    /// <summary>E8 — Registra metadado de documento (o arquivo nunca trafega).</summary>
    [HttpPost]
    [ProducesResponseType(typeof(DocumentoCriadoResposta), StatusCodes.Status201Created)]
    public async Task<IActionResult> Criar([FromBody] DocumentoRequest req) =>
        StatusCode(StatusCodes.Status201Created, await documentos.CriarAsync(req, User));

    /// <summary>E9 — Exclui metadado de documento.</summary>
    [HttpDelete("{idDoc:long}")]
    public async Task<IActionResult> Excluir(long idDoc)
    {
        await documentos.ExcluirAsync(idDoc, User);
        return NoContent();
    }
}

[ApiController]
[Route("api/v1/sus")]
[Authorize(Roles = "CIDADAO,PROFISSIONAL_SAUDE")]
public partial class SusController(ISusService sus) : ControllerBase
{
    /// <summary>E11 — Equipamentos de uma unidade (proxy do CNES).</summary>
    [HttpGet("equipamentos/{cnes}")]
    public async Task<IActionResult> Equipamentos(string cnes)
    {
        if (!CnesRegex().IsMatch(cnes))
            throw new ValidacaoException("Parâmetros inválidos.", new Dictionary<string, string> { ["cnes"] = "deve ter 7 dígitos" });
        return Ok(await sus.EquipamentosAsync(cnes));
    }

    /// <summary>E12 — Estoque de medicamento por município e CATMAT (proxy do BNAFAR).</summary>
    [HttpGet("medicamentos")]
    public async Task<IActionResult> Medicamentos([FromQuery] string municipio, [FromQuery] string catmat,
        [FromQuery] int page = 0, [FromQuery] int size = 20)
    {
        var campos = new Dictionary<string, string>();
        if (!IbgeRegex().IsMatch(municipio ?? "")) campos["municipio"] = "código IBGE com 6 ou 7 dígitos";
        if (string.IsNullOrWhiteSpace(catmat)) campos["catmat"] = "obrigatório";
        if (campos.Count > 0) throw new ValidacaoException("Parâmetros inválidos.", campos);
        var (p, s) = Paginacao.Validar(page, size);
        return Ok(await sus.MedicamentosAsync(municipio!, catmat, p, s));
    }

    /// <summary>Extra — busca por nome no catálogo local (retorna os CATMAT).</summary>
    [HttpGet("medicamentos/catalogo")]
    public IActionResult Catalogo([FromQuery] string? nome) => Ok(sus.Catalogo(nome));

    [GeneratedRegex(@"^\d{7}$")]
    private static partial Regex CnesRegex();

    [GeneratedRegex(@"^\d{6,7}$")]
    private static partial Regex IbgeRegex();
}

[ApiController]
[Route("api/v1/unidades")]
[Authorize(Roles = "CIDADAO,PROFISSIONAL_SAUDE,ADMIN")]
public class UnidadesController(IUnidadeService unidades) : ControllerBase
{
    /// <summary>E13 — Unidades ativas (para mapa e rota).</summary>
    [HttpGet]
    public async Task<IActionResult> Listar([FromQuery] int page = 0, [FromQuery] int size = 20)
    {
        var (p, s) = Paginacao.Validar(page, size);
        return Ok(await unidades.ListarAsync(p, s));
    }
}

[ApiController]
[Route("api/v1/admin")]
[Authorize(Roles = "ADMIN")]
public class AdminController(IAuditoriaService auditoria) : ControllerBase
{
    private static readonly HashSet<string> StatusValidos = ["AUTORIZADO", "EXPIRADO", "REJEITADO"];

    /// <summary>E14 — Auditoria de sessões de acesso, com filtros e paginação.</summary>
    [HttpGet("auditoria")]
    public async Task<IActionResult> Auditoria([FromQuery] string? status, [FromQuery] DateOnly? de,
        [FromQuery] DateOnly? ate, [FromQuery] int page = 0, [FromQuery] int size = 20)
    {
        if (status is not null && !StatusValidos.Contains(status))
            throw new ValidacaoException("Parâmetros inválidos.",
                new Dictionary<string, string> { ["status"] = "deve ser AUTORIZADO, EXPIRADO ou REJEITADO" });
        var (p, s) = Paginacao.Validar(page, size);
        return Ok(await auditoria.ListarAsync(status, de, ate, p, s));
    }
}
