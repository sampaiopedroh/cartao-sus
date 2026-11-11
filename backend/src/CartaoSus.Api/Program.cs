using System.Text;
using System.Text.Json;
using CartaoSus.Api.Data;
using CartaoSus.Api.Exceptions;
using CartaoSus.Api.Middleware;
using CartaoSus.Api.Models;
using CartaoSus.Api.Repositories;
using CartaoSus.Api.Seed;
using CartaoSus.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;

var builder = WebApplication.CreateBuilder(args);

// ---------- Configuração obrigatória (falha rápida) ----------
var jwtSecret = builder.Configuration["JWT_SECRET"];
if (string.IsNullOrWhiteSpace(jwtSecret) || jwtSecret.Length < 32)
    throw new InvalidOperationException(
        "JWT_SECRET não definida ou menor que 32 caracteres. Defina no arquivo .env (ver .env.example).");

var connectionString = builder.Configuration.GetConnectionString("Default") ?? MontarConnectionString(builder.Configuration);

// ---------- Porta padrão ----------
if (string.IsNullOrWhiteSpace(builder.Configuration["ASPNETCORE_URLS"]))
    builder.WebHost.UseUrls("http://localhost:8080");

// ---------- Persistência ----------
builder.Services.AddDbContext<CartaoSusDbContext>(opt =>
    opt.UseMySql(connectionString, new MySqlServerVersion(new Version(8, 0, 36))));

builder.Services.AddSingleton<PasswordHasher<Usuario>>();
builder.Services.AddScoped<IUsuarioRepository, UsuarioRepository>();
builder.Services.AddScoped<ICidadaoRepository, CidadaoRepository>();
builder.Services.AddScoped<IAuthService, AuthService>();

// ---------- Autenticação JWT ----------
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(opt =>
    {
        opt.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidateAudience = true,
            ValidAudience = builder.Configuration["Jwt:Audience"],
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.Zero
        };
        // Resposta 401 no mesmo envelope de erro da API (SDD 5.1).
        opt.Events = new JwtBearerEvents
        {
            OnChallenge = async ctx =>
            {
                ctx.HandleResponse();
                await ErrorHandlingMiddleware.Escrever(ctx.HttpContext, 401, "NAO_AUTORIZADO",
                    "Token ausente, inválido ou expirado.", new Dictionary<string, string>());
            }
        };
    });
builder.Services.AddAuthorization();

// ---------- Validação no envelope de erro ----------
builder.Services.AddControllers()
    .ConfigureApiBehaviorOptions(opt =>
    {
        opt.InvalidModelStateResponseFactory = ctx =>
        {
            var campos = ctx.ModelState
                .Where(e => e.Value?.Errors.Count > 0)
                .ToDictionary(
                    e => JsonNamingPolicy.CamelCase.ConvertName(e.Key),
                    e => e.Value!.Errors[0].ErrorMessage);
            var primeiro = campos.Values.FirstOrDefault() ?? "Dados inválidos.";
            throw new ValidacaoException(primeiro, campos);
        };
    })
    .AddJsonOptions(o => o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase);

// ---------- Swagger ----------
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Cartão SUS Inteligente API",
        Version = "1.0",
        Description = "API do MVP Cartão SUS Inteligente (Global Solution 2026/2)."
    });
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Name = "Authorization",
        Description = "Token obtido em POST /api/v1/auth/login"
    });
});

var app = builder.Build();

// ---------- Seeder de demonstração (idempotente) ----------
try
{
    await SeedDemoService.ExecutarAsync(app.Services, app.Configuration, app.Logger);
}
catch (Exception ex)
{
    app.Logger.LogCritical(ex, "Falha ao aplicar o seed de demonstração.");
    return 1;
}

app.UseMiddleware<ErrorHandlingMiddleware>();
app.UseSwagger();
app.UseSwaggerUI();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
return 0;

static string MontarConnectionString(IConfiguration cfg)
{
    var senha = cfg["MYSQL_APP_PASSWORD"];
    if (string.IsNullOrWhiteSpace(senha))
        throw new InvalidOperationException(
            "MYSQL_APP_PASSWORD não definida. Defina no arquivo .env (ver .env.example).");

    var host = cfg["MYSQL_HOST"] ?? "127.0.0.1";
    var porta = cfg["MYSQL_PORT"] ?? "3306";
    var banco = cfg["MYSQL_DATABASE"] ?? "cartao_sus";
    return $"Server={host};Port={porta};Database={banco};User=cartao_app;Password={senha};SslMode=Preferred;";
}

public partial class Program;
