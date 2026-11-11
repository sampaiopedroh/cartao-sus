using System.Security.Claims;
using CartaoSus.Api.Dtos;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CartaoSus.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController(IAuthService auth) : ControllerBase
{
    /// <summary>E1 — Cria conta de cidadão (usuário + cidadão). Segredo TOTP é exibido uma única vez.</summary>
    [HttpPost("cadastro")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(CadastroResponse), StatusCodes.Status201Created)]
    public async Task<IActionResult> Cadastro([FromBody] CadastroRequest req)
    {
        var resp = await auth.CadastrarAsync(req);
        return StatusCode(StatusCodes.Status201Created, resp);
    }

    /// <summary>E2 — Autentica e emite JWT (30 minutos).</summary>
    [HttpPost("login")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(LoginResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> Login([FromBody] LoginRequest req) =>
        Ok(await auth.LoginAsync(req));

    /// <summary>Extra — devolve os dados contidos no token. Prova a proteção por JWT.</summary>
    [HttpGet("me")]
    [Authorize]
    [ProducesResponseType(typeof(MeResponse), StatusCodes.Status200OK)]
    public IActionResult Me()
    {
        var idUsuario = long.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var idCidadao = User.FindFirstValue("idCidadao");
        return Ok(new MeResponse(
            idUsuario,
            User.FindFirstValue("login") ?? string.Empty,
            User.FindFirstValue(ClaimTypes.Role) ?? string.Empty,
            idCidadao is null ? null : long.Parse(idCidadao)));
    }
}
