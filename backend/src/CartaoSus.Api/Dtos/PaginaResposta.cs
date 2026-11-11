namespace CartaoSus.Api.Dtos;

// Paginação padrão da API (SDD 5.1): { content, page, size, totalElements }.
public record PaginaResposta<T>(IReadOnlyList<T> Content, int Page, int Size, long TotalElements);

public static class Paginacao
{
    public static (int Page, int Size) Validar(int page, int size)
    {
        if (page < 0) throw new Exceptions.ValidacaoException("Parâmetros inválidos.",
            new Dictionary<string, string> { ["page"] = "deve ser 0 ou maior" });
        if (size is < 1 or > 100) throw new Exceptions.ValidacaoException("Parâmetros inválidos.",
            new Dictionary<string, string> { ["size"] = "deve estar entre 1 e 100" });
        return (page, size);
    }
}
