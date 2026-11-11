namespace CartaoSus.Api.Models;

// Entidades espelham database/schema.sql (F1). Enums do banco ficam como string
// (validados pelo ENUM/CHECK do MySQL).

public class Usuario
{
    public long IdUsuario { get; set; }
    public string Login { get; set; } = string.Empty;
    public string SenhaHash { get; set; } = string.Empty;
    public string Perfil { get; set; } = "CIDADAO";
    public bool Ativo { get; set; } = true;
    public DateTime CriadoEm { get; set; }
}

public class Cidadao
{
    public long IdCidadao { get; set; }
    public long IdUsuario { get; set; }
    public string CpfHash { get; set; } = string.Empty;
    public string NumeroCns { get; set; } = string.Empty;
    public string NomeCompleto { get; set; } = string.Empty;
    public DateOnly DataNascimento { get; set; }
    public string SegredoTotp { get; set; } = string.Empty;
    public DateTime CriadoEm { get; set; }
    public Usuario? Usuario { get; set; }
}

public class UnidadeSaude
{
    public long IdUnidade { get; set; }
    public string CodigoCnes { get; set; } = string.Empty;
    public string NomeEstabelecimento { get; set; } = string.Empty;
    public string TipoUnidade { get; set; } = string.Empty;
    public string Bairro { get; set; } = string.Empty;
    public string Logradouro { get; set; } = string.Empty;
    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }
    public bool Ativo { get; set; } = true;
}

public class CarteiraMetadados
{
    public long IdDocumento { get; set; }
    public long IdCidadao { get; set; }
    public string TipoDocumento { get; set; } = string.Empty;
    public string NomeArquivo { get; set; } = string.Empty;
    public string HashIntegridade { get; set; } = string.Empty;
    public DateOnly? DataValidade { get; set; }
    public DateTime DataCadastro { get; set; }
}

public class HistoricoClinico
{
    public long IdHistorico { get; set; }
    public long IdCidadao { get; set; }
    public string TipoRegistro { get; set; } = string.Empty;
    public string Descricao { get; set; } = string.Empty;
    public string GrauSeveridade { get; set; } = "MODERADO";
    public DateOnly DataRegistro { get; set; }
}

public class SinalVital
{
    public long IdSinal { get; set; }
    public long IdCidadao { get; set; }
    public string TipoMedicao { get; set; } = string.Empty;
    public decimal ValorMedido { get; set; }
    public string Origem { get; set; } = "APP";
    public DateTime DataHora { get; set; }
}

public class SessaoAcesso
{
    public long IdSessao { get; set; }
    public long IdCidadao { get; set; }
    public long IdUnidade { get; set; }
    public long? IdUsuarioAtendente { get; set; }
    public string TokenUtilizado { get; set; } = string.Empty;
    public string StatusAutenticacao { get; set; } = string.Empty;
    public string? Motivo { get; set; }
    public DateTime DataHoraAcesso { get; set; }
}

public class Alerta
{
    public long IdAlerta { get; set; }
    public long IdCidadao { get; set; }
    public string TipoAlerta { get; set; } = string.Empty;
    public string Severidade { get; set; } = string.Empty;
    public string Mensagem { get; set; } = string.Empty;
    public string StatusEnvio { get; set; } = "PENDENTE";
    public DateTime CriadoEm { get; set; }
}

public class DispositivoIot
{
    public long IdDispositivo { get; set; }
    public long? IdUnidade { get; set; }
    public long? IdCidadao { get; set; }
    public string Tipo { get; set; } = string.Empty;
    public string ChaveApiHash { get; set; } = string.Empty;
    public bool Ativo { get; set; } = true;
}
