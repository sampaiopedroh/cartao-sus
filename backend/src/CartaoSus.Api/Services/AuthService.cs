using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Security;
using Microsoft.AspNetCore.Identity;
using Microsoft.IdentityModel.Tokens;

namespace CartaoSus.Api.Services;

public interface IAuthService
{
    Task<CadastroResponse> CadastrarAsync(CadastroRequest req);
    Task<LoginResponse> LoginAsync(LoginRequest req);
}

public class AuthService(
    IUsuarioRepository usuarios,
    ICidadaoRepository cidadaos,
    PasswordHasher<Usuario> hasher,
    IConfiguration config) : IAuthService
{
    private const string MensagemLoginInvalido = "Credenciais inválidas.";

    public async Task<CadastroResponse> CadastrarAsync(CadastroRequest req)
    {
        if (req.DataNascimento is null || req.DataNascimento >= DateOnly.FromDateTime(DateTime.UtcNow))
            throw new ValidacaoException("Dados inválidos.",
                new Dictionary<string, string> { ["dataNascimento"] = "deve ser uma data no passado" });

        var cpfHash = SegurancaUtils.HashCpf(req.Cpf);
        if (await usuarios.LoginExisteAsync(req.Login) || await cidadaos.CpfOuCnsExisteAsync(cpfHash, req.NumeroCns))
            throw new ConflitoException("Já existe conta com este CPF, CNS ou login.");

        // Um único SaveChanges: o EF grava Usuario e Cidadao na mesma transação.
        var segredo = SegurancaUtils.GerarSegredoTotp();
        var usuario = new Usuario
        {
            Login = req.Login,
            Perfil = "CIDADAO",
            Ativo = true,
            CriadoEm = DateTime.UtcNow
        };
        usuario.SenhaHash = hasher.HashPassword(usuario, req.Senha);
        var cidadao = new Cidadao
        {
            Usuario = usuario,
            CpfHash = cpfHash,
            NumeroCns = req.NumeroCns,
            NomeCompleto = req.NomeCompleto,
            DataNascimento = req.DataNascimento.Value,
            SegredoTotp = segredo,
            CriadoEm = DateTime.UtcNow
        };
        await cidadaos.AdicionarAsync(cidadao);
        await cidadaos.SalvarAsync();

        return new CadastroResponse(cidadao.IdCidadao, segredo,
            "Conta criada. Guarde o segredo TOTP no app (exibido uma única vez).");
    }

    public async Task<LoginResponse> LoginAsync(LoginRequest req)
    {
        var usuario = await usuarios.PorLoginAsync(req.Login);
        if (usuario is null || !usuario.Ativo) throw new CredenciaisInvalidasException();

        var resultado = hasher.VerifyHashedPassword(usuario, usuario.SenhaHash, req.Senha);
        if (resultado == PasswordVerificationResult.Failed) throw new CredenciaisInvalidasException();

        var cidadao = await cidadaos.PorUsuarioAsync(usuario.IdUsuario);
        var nome = cidadao?.NomeCompleto ?? usuario.Login;
        var minutos = config.GetValue<int>("Jwt:ExpiracaoMinutos", 30);

        return new LoginResponse(
            Token: EmitirToken(usuario, cidadao?.IdCidadao, nome, minutos),
            Tipo: "Bearer",
            ExpiraEmMinutos: minutos,
            Perfil: usuario.Perfil,
            IdCidadao: cidadao?.IdCidadao,
            Nome: nome);
    }

    private string EmitirToken(Usuario usuario, long? idCidadao, string nome, int minutos)
    {
        var segredo = config["JWT_SECRET"]!; // validado no Program.cs
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, usuario.IdUsuario.ToString()),
            new("login", usuario.Login),
            new("nome", nome),
            new(ClaimTypes.Role, usuario.Perfil)
        };
        if (idCidadao is not null) claims.Add(new Claim("idCidadao", idCidadao.Value.ToString()));

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(segredo));
        var token = new JwtSecurityToken(
            issuer: config["Jwt:Issuer"],
            audience: config["Jwt:Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(minutos),
            signingCredentials: new SigningCredentials(key, SecurityAlgorithms.HmacSha256));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
