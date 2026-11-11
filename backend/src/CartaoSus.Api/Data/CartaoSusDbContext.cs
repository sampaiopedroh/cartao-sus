using CartaoSus.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace CartaoSus.Api.Data;

public class CartaoSusDbContext(DbContextOptions<CartaoSusDbContext> options) : DbContext(options)
{
    public DbSet<Usuario> Usuarios => Set<Usuario>();
    public DbSet<Cidadao> Cidadaos => Set<Cidadao>();
    public DbSet<UnidadeSaude> Unidades => Set<UnidadeSaude>();
    public DbSet<CarteiraMetadados> CarteiraDocumentos => Set<CarteiraMetadados>();
    public DbSet<HistoricoClinico> Historicos => Set<HistoricoClinico>();
    public DbSet<SinalVital> SinaisVitais => Set<SinalVital>();
    public DbSet<SessaoAcesso> Sessoes => Set<SessaoAcesso>();
    public DbSet<Alerta> Alertas => Set<Alerta>();
    public DbSet<DispositivoIot> Dispositivos => Set<DispositivoIot>();

    protected override void OnModelCreating(ModelBuilder mb)
    {
        mb.Entity<Usuario>(e =>
        {
            e.ToTable("TB_USUARIO");
            e.HasKey(x => x.IdUsuario);
            e.Property(x => x.IdUsuario).HasColumnName("ID_USUARIO");
            e.Property(x => x.Login).HasColumnName("LOGIN");
            e.Property(x => x.SenhaHash).HasColumnName("SENHA_HASH");
            e.Property(x => x.Perfil).HasColumnName("PERFIL");
            e.Property(x => x.Ativo).HasColumnName("ATIVO");
            e.Property(x => x.CriadoEm).HasColumnName("CRIADO_EM");
        });

        mb.Entity<Cidadao>(e =>
        {
            e.ToTable("TB_CIDADAO");
            e.HasKey(x => x.IdCidadao);
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.IdUsuario).HasColumnName("ID_USUARIO");
            e.Property(x => x.CpfHash).HasColumnName("CPF_HASH");
            e.Property(x => x.NumeroCns).HasColumnName("NUMERO_CNS");
            e.Property(x => x.NomeCompleto).HasColumnName("NOME_COMPLETO");
            e.Property(x => x.DataNascimento).HasColumnName("DATA_NASCIMENTO");
            e.Property(x => x.SegredoTotp).HasColumnName("SEGREDO_TOTP");
            e.Property(x => x.CriadoEm).HasColumnName("CRIADO_EM");
            e.HasOne(x => x.Usuario).WithOne().HasForeignKey<Cidadao>(x => x.IdUsuario);
        });

        mb.Entity<UnidadeSaude>(e =>
        {
            e.ToTable("TB_UNIDADE_SAUDE");
            e.HasKey(x => x.IdUnidade);
            e.Property(x => x.IdUnidade).HasColumnName("ID_UNIDADE");
            e.Property(x => x.CodigoCnes).HasColumnName("CODIGO_CNES");
            e.Property(x => x.NomeEstabelecimento).HasColumnName("NOME_ESTABELECIMENTO");
            e.Property(x => x.TipoUnidade).HasColumnName("TIPO_UNIDADE");
            e.Property(x => x.Bairro).HasColumnName("BAIRRO");
            e.Property(x => x.Logradouro).HasColumnName("LOGRADOURO");
            e.Property(x => x.Latitude).HasColumnName("LATITUDE");
            e.Property(x => x.Longitude).HasColumnName("LONGITUDE");
            e.Property(x => x.Ativo).HasColumnName("ATIVO");
        });

        mb.Entity<CarteiraMetadados>(e =>
        {
            e.ToTable("TB_CARTEIRA_METADADOS");
            e.HasKey(x => x.IdDocumento);
            e.Property(x => x.IdDocumento).HasColumnName("ID_DOCUMENTO");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.TipoDocumento).HasColumnName("TIPO_DOCUMENTO");
            e.Property(x => x.NomeArquivo).HasColumnName("NOME_ARQUIVO");
            e.Property(x => x.HashIntegridade).HasColumnName("HASH_INTEGRIDADE");
            e.Property(x => x.DataValidade).HasColumnName("DATA_VALIDADE");
            e.Property(x => x.DataCadastro).HasColumnName("DATA_CADASTRO");
        });

        mb.Entity<HistoricoClinico>(e =>
        {
            e.ToTable("TB_HISTORICO_CLINICO");
            e.HasKey(x => x.IdHistorico);
            e.Property(x => x.IdHistorico).HasColumnName("ID_HISTORICO");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.TipoRegistro).HasColumnName("TIPO_REGISTRO");
            e.Property(x => x.Descricao).HasColumnName("DESCRICAO");
            e.Property(x => x.GrauSeveridade).HasColumnName("GRAU_SEVERIDADE");
            e.Property(x => x.DataRegistro).HasColumnName("DATA_REGISTRO");
        });

        mb.Entity<SinalVital>(e =>
        {
            e.ToTable("TB_SINAL_VITAL");
            e.HasKey(x => x.IdSinal);
            e.Property(x => x.IdSinal).HasColumnName("ID_SINAL");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.TipoMedicao).HasColumnName("TIPO_MEDICAO");
            e.Property(x => x.ValorMedido).HasColumnName("VALOR_MEDIDO");
            e.Property(x => x.Origem).HasColumnName("ORIGEM");
            e.Property(x => x.DataHora).HasColumnName("DATA_HORA");
        });

        mb.Entity<SessaoAcesso>(e =>
        {
            e.ToTable("TB_SESSAO_ACESSO");
            e.HasKey(x => x.IdSessao);
            e.Property(x => x.IdSessao).HasColumnName("ID_SESSAO");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.IdUnidade).HasColumnName("ID_UNIDADE");
            e.Property(x => x.IdUsuarioAtendente).HasColumnName("ID_USUARIO_ATENDENTE");
            e.Property(x => x.TokenUtilizado).HasColumnName("TOKEN_UTILIZADO");
            e.Property(x => x.StatusAutenticacao).HasColumnName("STATUS_AUTENTICACAO");
            e.Property(x => x.Motivo).HasColumnName("MOTIVO");
            e.Property(x => x.DataHoraAcesso).HasColumnName("DATA_HORA_ACESSO");
        });

        mb.Entity<Alerta>(e =>
        {
            e.ToTable("TB_ALERTA");
            e.HasKey(x => x.IdAlerta);
            e.Property(x => x.IdAlerta).HasColumnName("ID_ALERTA");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.TipoAlerta).HasColumnName("TIPO_ALERTA");
            e.Property(x => x.Severidade).HasColumnName("SEVERIDADE");
            e.Property(x => x.Mensagem).HasColumnName("MENSAGEM");
            e.Property(x => x.StatusEnvio).HasColumnName("STATUS_ENVIO");
            e.Property(x => x.CriadoEm).HasColumnName("CRIADO_EM");
        });

        mb.Entity<DispositivoIot>(e =>
        {
            e.ToTable("TB_DISPOSITIVO_IOT");
            e.HasKey(x => x.IdDispositivo);
            e.Property(x => x.IdDispositivo).HasColumnName("ID_DISPOSITIVO");
            e.Property(x => x.IdUnidade).HasColumnName("ID_UNIDADE");
            e.Property(x => x.IdCidadao).HasColumnName("ID_CIDADAO");
            e.Property(x => x.Tipo).HasColumnName("TIPO");
            e.Property(x => x.ChaveApiHash).HasColumnName("CHAVE_API_HASH");
            e.Property(x => x.Ativo).HasColumnName("ATIVO");
        });
    }
}
