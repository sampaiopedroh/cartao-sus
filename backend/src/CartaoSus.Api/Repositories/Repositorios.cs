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
    Task<Cidadao?> PorUsuarioAsync(long idUsuario);
    Task<bool> CpfOuCnsExisteAsync(string cpfHash, string cns);
    Task AdicionarAsync(Cidadao cidadao);
    Task SalvarAsync();
}

public class CidadaoRepository(CartaoSusDbContext db) : ICidadaoRepository
{
    public Task<Cidadao?> PorUsuarioAsync(long idUsuario) =>
        db.Cidadaos.FirstOrDefaultAsync(c => c.IdUsuario == idUsuario);

    public Task<bool> CpfOuCnsExisteAsync(string cpfHash, string cns) =>
        db.Cidadaos.AnyAsync(c => c.CpfHash == cpfHash || c.NumeroCns == cns);

    public async Task AdicionarAsync(Cidadao cidadao) => await db.Cidadaos.AddAsync(cidadao);

    public Task SalvarAsync() => db.SaveChangesAsync();
}
