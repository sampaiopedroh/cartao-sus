using System.ComponentModel.DataAnnotations;

namespace CartaoSus.Api.Dtos;

// ---------- Acolhimento (E3) ----------
public class ValidarNfcRequest
{
    [Required(ErrorMessage = "numeroCns é obrigatório")]
    [RegularExpression(@"^\d{15}$", ErrorMessage = "numeroCns deve ter 15 dígitos")]
    public string NumeroCns { get; set; } = string.Empty;

    [Required(ErrorMessage = "codigoCnesUnidade é obrigatório")]
    [RegularExpression(@"^\d{7}$", ErrorMessage = "codigoCnesUnidade deve ter 7 dígitos")]
    public string CodigoCnesUnidade { get; set; } = string.Empty;

    [Required(ErrorMessage = "tokenRotativo é obrigatório")]
    [RegularExpression(@"^\d{6}$", ErrorMessage = "tokenRotativo deve ter exatamente 6 dígitos")]
    public string TokenRotativo { get; set; } = string.Empty;
}

public record AlertaCritico(string Tipo, string Substancia, string Severidade);
public record UltimasMedicoes(decimal? Glicemia, string? PressaoArterial, DateTime? DataRegistro);
public record PacienteResumo(
    string NomeCompleto,
    string Cns,
    IReadOnlyList<string> DocumentosDisponiveis,
    IReadOnlyList<AlertaCritico> AlertasCriticos,
    UltimasMedicoes UltimasMedicoes);
public record ValidarNfcResponse(string Status, string Mensagem, PacienteResumo Paciente);

// ---------- Telemetria (E6) ----------
public class SinalVitalRequest
{
    [Required(ErrorMessage = "tipoMedicao é obrigatório")]
    public string TipoMedicao { get; set; } = string.Empty;

    [Required(ErrorMessage = "valor é obrigatório")]
    public decimal? Valor { get; set; }

    public DateTime? DataHora { get; set; }

    [RegularExpression(@"^\d{15}$", ErrorMessage = "numeroCns deve ter 15 dígitos")]
    public string? NumeroCns { get; set; }
}

public record AlertaDisparado(string Tipo, string Mensagem);
public record SinalVitalResposta(long IdSinal, bool AlertaDisparado, AlertaDisparado? Alerta);

// ---------- Cidadão (E4, E5, E7, alertas, acessos) ----------
public record ProntuarioHistorico(string Tipo, string Descricao, string Severidade, DateOnly DataRegistro);
public record AlertaResumo(long IdAlerta, string Tipo, string Severidade, string Mensagem, string StatusEnvio, DateTime CriadoEm);
public record ProntuarioResposta(long IdCidadao, string NomeCompleto, string Cns,
    IReadOnlyList<ProntuarioHistorico> Historico, IReadOnlyList<AlertaResumo> AlertasPendentes);

public record MedicaoResumo(decimal Valor, string Origem, DateTime DataHora);
public record ResumoMedicoes(decimal? Media, decimal? Minima, decimal? Maxima, int Total);
public record SinaisVitaisResposta(string? Tipo, IReadOnlyList<MedicaoResumo> Medicoes, ResumoMedicoes Resumo);

public class AtualizarHistoricoRequest
{
    [Required(ErrorMessage = "descricao é obrigatória")]
    [StringLength(255, ErrorMessage = "descricao deve ter no máximo 255 caracteres")]
    public string Descricao { get; set; } = string.Empty;

    [Required(ErrorMessage = "grauSeveridade é obrigatório")]
    public string GrauSeveridade { get; set; } = string.Empty;
}

public record HistoricoResposta(long IdHistorico, string Tipo, string Descricao, string GrauSeveridade, DateOnly DataRegistro);
public record AcessoResumo(long IdSessao, string StatusAutenticacao, string? Motivo, DateTime DataHoraAcesso, long IdUnidade);

// ---------- Documentos (E8) ----------
public class DocumentoRequest
{
    [Required(ErrorMessage = "tipoDocumento é obrigatório")]
    public string TipoDocumento { get; set; } = string.Empty;

    [Required(ErrorMessage = "nomeArquivo é obrigatório")]
    [StringLength(200, ErrorMessage = "nomeArquivo deve ter no máximo 200 caracteres")]
    public string NomeArquivo { get; set; } = string.Empty;

    [Required(ErrorMessage = "hashIntegridade é obrigatório")]
    [RegularExpression(@"^[a-fA-F0-9]{64}$", ErrorMessage = "hashIntegridade deve ter 64 caracteres hexadecimais")]
    public string HashIntegridade { get; set; } = string.Empty;

    public DateOnly? DataValidade { get; set; }
}

public record DocumentoCriadoResposta(long IdDocumento, string Mensagem);

// ---------- SUS (E11, E12) ----------
public record EquipamentoItem(string Tipo, string Descricao, int? QuantidadeExistente, int? QuantidadeEmUso);
public record EquipamentosResposta(string CoCnes, string? Estabelecimento, IReadOnlyList<EquipamentoItem> Equipamentos);
public record MedicamentoItem(string Nome, string Catmat, string? Municipio, string? Estabelecimento,
    decimal? Quantidade, string? DataPosicao, decimal? Latitude, decimal? Longitude);
public record CatalogoItem(string Nome, string Apresentacao, string Catmat);

// ---------- Unidades (E13) ----------
public record UnidadeResumo(long Id, string CodigoCnes, string Nome, string Tipo, string Bairro,
    decimal? Latitude, decimal? Longitude);

// ---------- Auditoria (E14) ----------
public record AuditoriaItem(long IdSessao, string Cidadao, string? Login, string Unidade,
    string? TokenUtilizado, string StatusAutenticacao, string? Motivo, DateTime DataHoraAcesso);
