using System.ComponentModel.DataAnnotations;

namespace CartaoSus.Api.Dtos;

public class CadastroRequest
{
    [Required(ErrorMessage = "login é obrigatório")]
    [EmailAddress(ErrorMessage = "login deve ser um e-mail válido")]
    [StringLength(100)]
    public string Login { get; set; } = string.Empty;

    [Required(ErrorMessage = "senha é obrigatória")]
    [MinLength(8, ErrorMessage = "senha deve ter pelo menos 8 caracteres")]
    public string Senha { get; set; } = string.Empty;

    [Required(ErrorMessage = "cpf é obrigatório")]
    [RegularExpression(@"^\d{11}$", ErrorMessage = "cpf deve ter 11 dígitos")]
    public string Cpf { get; set; } = string.Empty;

    [Required(ErrorMessage = "numeroCns é obrigatório")]
    [RegularExpression(@"^\d{15}$", ErrorMessage = "numeroCns deve ter 15 dígitos")]
    public string NumeroCns { get; set; } = string.Empty;

    [Required(ErrorMessage = "nomeCompleto é obrigatório")]
    [MinLength(3, ErrorMessage = "nomeCompleto deve ter pelo menos 3 caracteres")]
    [StringLength(150)]
    public string NomeCompleto { get; set; } = string.Empty;

    [Required(ErrorMessage = "dataNascimento é obrigatória")]
    public DateOnly? DataNascimento { get; set; }
}

public record CadastroResponse(long IdCidadao, string SegredoTotp, string Mensagem);

public class LoginRequest
{
    [Required(ErrorMessage = "login é obrigatório")]
    public string Login { get; set; } = string.Empty;

    [Required(ErrorMessage = "senha é obrigatória")]
    public string Senha { get; set; } = string.Empty;
}

public record LoginResponse(
    string Token,
    string Tipo,
    int ExpiraEmMinutos,
    string Perfil,
    long? IdCidadao,
    string Nome);

public record MeResponse(long IdUsuario, string Login, string Perfil, long? IdCidadao);
