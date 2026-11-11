using CartaoSus.Api.Data;
using CartaoSus.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace CartaoSus.Api.Repositories;

// Repositórios só fazem consultas e gravações. Nenhuma regra de negócio aqui (SDD 5.4).

public interface IUsuarioRepository
{
    Task<Usuario?> PorLoginAsync(string login);
    Task<bool> LoginExisteAsync(string login);
    Task AdicionarAsync(Usuario usuario);
    Task SalvarAsync();
}

public class UsuarioRepository(CartaoSusDbContext db) : IUsuarioRepository
{
    public Task<Usuario?> PorLoginAsync(string login) =>
        db.Usuarios.FirstOrDefaultAsync(u => u.Login == login);

    public Task<bool> LoginExisteAsync(string login) =>
        db.Usuarios.AnyAsync(u => u.Login == login);

    public async Task AdicionarAsync(Usuario usuario) => await db.Usuarios.AddAsync(usuario);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface ICidadaoRepository
{
    Task<Cidadao?> PorIdAsync(long id);
    Task<Cidadao?> PorUsuarioAsync(long idUsuario);
    Task<Cidadao?> PorCnsAsync(string cns);
    Task<bool> CpfOuCnsExisteAsync(string cpfHash, string cns);
    Task AdicionarAsync(Cidadao cidadao);
    Task SalvarAsync();
}

public class CidadaoRepository(CartaoSusDbContext db) : ICidadaoRepository
{
    public Task<Cidadao?> PorIdAsync(long id) =>
        db.Cidadaos.Include(c => c.Usuario).FirstOrDefaultAsync(c => c.IdCidadao == id);

    public Task<Cidadao?> PorUsuarioAsync(long idUsuario) =>
        db.Cidadaos.FirstOrDefaultAsync(c => c.IdUsuario == idUsuario);

    public Task<Cidadao?> PorCnsAsync(string cns) =>
        db.Cidadaos.Include(c => c.Usuario).FirstOrDefaultAsync(c => c.NumeroCns == cns);

    public Task<bool> CpfOuCnsExisteAsync(string cpfHash, string cns) =>
        db.Cidadaos.AnyAsync(c => c.CpfHash == cpfHash || c.NumeroCns == cns);

    public async Task AdicionarAsync(Cidadao cidadao) => await db.Cidadaos.AddAsync(cidadao);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface IUnidadeRepository
{
    Task<UnidadeSaude?> PorCnesAsync(string cnes);
    Task<UnidadeSaude?> PorIdAsync(long id);
    Task<(List<UnidadeSaude> Itens, long Total)> ListarAtivasAsync(int page, int size);
}

public class UnidadeRepository(CartaoSusDbContext db) : IUnidadeRepository
{
    public Task<UnidadeSaude?> PorCnesAsync(string cnes) =>
        db.Unidades.FirstOrDefaultAsync(u => u.CodigoCnes == cnes && u.Ativo);

    public Task<UnidadeSaude?> PorIdAsync(long id) =>
        db.Unidades.FirstOrDefaultAsync(u => u.IdUnidade == id);

    public async Task<(List<UnidadeSaude> Itens, long Total)> ListarAtivasAsync(int page, int size)
    {
        var q = db.Unidades.Where(u => u.Ativo).OrderBy(u => u.IdUnidade);
        var total = await q.LongCountAsync();
        var itens = await q.Skip(page * size).Take(size).ToListAsync();
        return (itens, total);
    }
}

public interface IDispositivoRepository
{
    Task<DispositivoIot?> PorHashAsync(string hash);
}

public class DispositivoRepository(CartaoSusDbContext db) : IDispositivoRepository
{
    public Task<DispositivoIot?> PorHashAsync(string hash) =>
        db.Dispositivos.FirstOrDefaultAsync(d => d.ChaveApiHash == hash && d.Ativo);
}

public interface ISessaoRepository
{
    Task<int> ContarRejeicoesDesdeAsync(long idCidadao, DateTime desde);
    Task<bool> ExisteAutorizadaDesdeAsync(long idCidadao, DateTime desde);
    Task<List<SessaoAcesso>> UltimasAsync(long idCidadao, int quantidade);
    Task<(List<SessaoAcesso> Itens, long Total)> AuditoriaAsync(string? status, DateTime? de, DateTime? ate, int page, int size);
    Task AdicionarAsync(SessaoAcesso sessao);
    Task SalvarAsync();
}

public class SessaoRepository(CartaoSusDbContext db) : ISessaoRepository
{
    public Task<int> ContarRejeicoesDesdeAsync(long idCidadao, DateTime desde) =>
        db.Sessoes.CountAsync(s => s.IdCidadao == idCidadao
            && s.StatusAutenticacao == "REJEITADO" && s.DataHoraAcesso >= desde);

    public Task<bool> ExisteAutorizadaDesdeAsync(long idCidadao, DateTime desde) =>
        db.Sessoes.AnyAsync(s => s.IdCidadao == idCidadao
            && s.StatusAutenticacao == "AUTORIZADO" && s.DataHoraAcesso >= desde);

    public Task<List<SessaoAcesso>> UltimasAsync(long idCidadao, int quantidade) =>
        db.Sessoes.Where(s => s.IdCidadao == idCidadao)
            .OrderByDescending(s => s.DataHoraAcesso).Take(quantidade).ToListAsync();

    public async Task<(List<SessaoAcesso> Itens, long Total)> AuditoriaAsync(
        string? status, DateTime? de, DateTime? ate, int page, int size)
    {
        var q = db.Sessoes.AsQueryable();
        if (!string.IsNullOrWhiteSpace(status)) q = q.Where(s => s.StatusAutenticacao == status);
        if (de is not null) q = q.Where(s => s.DataHoraAcesso >= de);
        if (ate is not null) q = q.Where(s => s.DataHoraAcesso < ate);
        q = q.OrderByDescending(s => s.DataHoraAcesso);
        var total = await q.LongCountAsync();
        var itens = await q.Skip(page * size).Take(size).ToListAsync();
        return (itens, total);
    }

    public async Task AdicionarAsync(SessaoAcesso sessao) => await db.Sessoes.AddAsync(sessao);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface IAlertaRepository
{
    Task<bool> TentativaIndevidaRecenteAsync(long idCidadao, DateTime desde);
    Task<List<Alerta>> PendentesAsync(long idCidadao);
    Task<(List<Alerta> Itens, long Total)> ListarAsync(long idCidadao, string? status, int page, int size);
    Task<Alerta?> PorIdAsync(long idCidadao, long idAlerta);
    Task AdicionarAsync(Alerta alerta);
    Task SalvarAsync();
}

public class AlertaRepository(CartaoSusDbContext db) : IAlertaRepository
{
    public Task<bool> TentativaIndevidaRecenteAsync(long idCidadao, DateTime desde) =>
        db.Alertas.AnyAsync(a => a.IdCidadao == idCidadao
            && a.TipoAlerta == "TENTATIVA_INDEVIDA" && a.CriadoEm >= desde);

    public Task<List<Alerta>> PendentesAsync(long idCidadao) =>
        db.Alertas.Where(a => a.IdCidadao == idCidadao && a.StatusEnvio == "PENDENTE")
            .OrderByDescending(a => a.CriadoEm).ToListAsync();

    public async Task<(List<Alerta> Itens, long Total)> ListarAsync(long idCidadao, string? status, int page, int size)
    {
        var q = db.Alertas.Where(a => a.IdCidadao == idCidadao);
        if (!string.IsNullOrWhiteSpace(status)) q = q.Where(a => a.StatusEnvio == status);
        q = q.OrderByDescending(a => a.CriadoEm);
        var total = await q.LongCountAsync();
        var itens = await q.Skip(page * size).Take(size).ToListAsync();
        return (itens, total);
    }

    public Task<Alerta?> PorIdAsync(long idCidadao, long idAlerta) =>
        db.Alertas.FirstOrDefaultAsync(a => a.IdCidadao == idCidadao && a.IdAlerta == idAlerta);

    public async Task AdicionarAsync(Alerta alerta) => await db.Alertas.AddAsync(alerta);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface ISinalVitalRepository
{
    Task<List<SinalVital>> ListarAsync(long idCidadao, string? tipo, DateTime? de, DateTime? ate);
    Task AdicionarAsync(SinalVital sinal);
    Task SalvarAsync();
}

public class SinalVitalRepository(CartaoSusDbContext db) : ISinalVitalRepository
{
    public Task<List<SinalVital>> ListarAsync(long idCidadao, string? tipo, DateTime? de, DateTime? ate)
    {
        var q = db.SinaisVitais.Where(s => s.IdCidadao == idCidadao);
        if (!string.IsNullOrWhiteSpace(tipo)) q = q.Where(s => s.TipoMedicao == tipo);
        if (de is not null) q = q.Where(s => s.DataHora >= de);
        if (ate is not null) q = q.Where(s => s.DataHora < ate);
        return q.OrderBy(s => s.DataHora).ToListAsync();
    }

    public async Task AdicionarAsync(SinalVital sinal) => await db.SinaisVitais.AddAsync(sinal);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface IHistoricoRepository
{
    Task<List<HistoricoClinico>> DoCidadaoAsync(long idCidadao);
    Task<HistoricoClinico?> PorIdAsync(long idCidadao, long idHistorico);
    Task SalvarAsync();
}

public class HistoricoRepository(CartaoSusDbContext db) : IHistoricoRepository
{
    public Task<List<HistoricoClinico>> DoCidadaoAsync(long idCidadao) =>
        db.Historicos.Where(h => h.IdCidadao == idCidadao)
            .OrderBy(h => h.DataRegistro).ToListAsync();

    public Task<HistoricoClinico?> PorIdAsync(long idCidadao, long idHistorico) =>
        db.Historicos.FirstOrDefaultAsync(h => h.IdCidadao == idCidadao && h.IdHistorico == idHistorico);

    public Task SalvarAsync() => db.SaveChangesAsync();
}

public interface IDocumentoRepository
{
    Task<List<CarteiraMetadados>> DoCidadaoAsync(long idCidadao);
    Task<CarteiraMetadados?> PorIdAsync(long idDocumento);
    Task AdicionarAsync(CarteiraMetadados doc);
    Task RemoverAsync(CarteiraMetadados doc);
    Task SalvarAsync();
}

public class DocumentoRepository(CartaoSusDbContext db) : IDocumentoRepository
{
    public Task<List<CarteiraMetadados>> DoCidadaoAsync(long idCidadao) =>
        db.CarteiraDocumentos.Where(d => d.IdCidadao == idCidadao).OrderBy(d => d.IdDocumento).ToListAsync();

    public Task<CarteiraMetadados?> PorIdAsync(long idDocumento) =>
        db.CarteiraDocumentos.FirstOrDefaultAsync(d => d.IdDocumento == idDocumento);

    public async Task AdicionarAsync(CarteiraMetadados doc) => await db.CarteiraDocumentos.AddAsync(doc);

    public Task RemoverAsync(CarteiraMetadados doc)
    {
        db.CarteiraDocumentos.Remove(doc);
        return Task.CompletedTask;
    }

    public Task SalvarAsync() => db.SaveChangesAsync();
}
