using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Configuration;
using Moq;

namespace CartaoSus.Tests.Unitarios;

public class AuthTests
{
    private readonly Mock<IUsuarioRepository> _usuarios = new();
    private readonly Mock<ICidadaoRepository> _cidadaos = new();
    private readonly PasswordHasher<Usuario> _hasher = new();
    private readonly IConfiguration _config = new ConfigurationBuilder().AddInMemoryCollection(new Dictionary<string, string?>
    {
        ["JWT_SECRET"] = "chave-de-teste-com-mais-de-32-caracteres-ok",
        ["Jwt:Issuer"] = "cartao-sus-api",
        ["Jwt:Audience"] = "cartao-sus-app",
        ["Jwt:ExpiracaoMinutos"] = "30"
    }).Build();

    private AuthService Servico() => new(_usuarios.Object, _cidadaos.Object, _hasher, _config);

    [Fact(DisplayName = "UT-09 senha errada lança CredenciaisInvalidasException")]
    public async Task UT09_SenhaErradaEhRecusada()
    {
        var usuario = new Usuario { IdUsuario = 5, Login = "maria@teste", Ativo = true, Perfil = "CIDADAO" };
        usuario.SenhaHash = _hasher.HashPassword(usuario, "SenhaCorreta1");
        _usuarios.Setup(u => u.PorLoginAsync("maria@teste")).ReturnsAsync(usuario);

        await Assert.ThrowsAsync<CredenciaisInvalidasException>(() =>
            Servico().LoginAsync(new LoginRequest { Login = "maria@teste", Senha = "SenhaErrada1" }));
    }

    [Fact(DisplayName = "UT-10 senha no cadastro é gravada como hash, nunca em texto puro")]
    public async Task UT10_CadastroGravaHashDaSenha()
    {
        Cidadao? gravado = null;
        _usuarios.Setup(u => u.LoginExisteAsync(It.IsAny<string>())).ReturnsAsync(false);
        _cidadaos.Setup(c => c.CpfOuCnsExisteAsync(It.IsAny<string>(), It.IsAny<string>())).ReturnsAsync(false);
        _cidadaos.Setup(c => c.AdicionarAsync(It.IsAny<Cidadao>())).Callback<Cidadao>(c => gravado = c).Returns(Task.CompletedTask);

        const string senha = "SenhaForte123";
        var resposta = await Servico().CadastrarAsync(new CadastroRequest
        {
            Login = "nova@teste.com", Senha = senha, Cpf = "52998224725", NumeroCns = "700000000000001",
            NomeCompleto = "Nova Pessoa", DataNascimento = new DateOnly(1990, 5, 20)
        });

        Assert.NotNull(gravado);
        var hash = gravado!.Usuario!.SenhaHash;
        Assert.NotEqual(senha, hash);
        Assert.Equal(PasswordVerificationResult.Success, _hasher.VerifyHashedPassword(gravado.Usuario, hash, senha));
        Assert.Equal(64, gravado.CpfHash.Length); // CPF guardado só como SHA-256
        Assert.Equal(32, resposta.SegredoTotp.Length); // 160 bits em Base32
    }
}
