using System.Security.Cryptography;
using System.Text;

namespace CartaoSus.Api.Security;

public static class SegurancaUtils
{
    private const string Base32Alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    // SHA-256 em hexadecimal minúsculo (LGPD: o CPF nunca é armazenado em texto).
    public static string HashCpf(string cpf) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(cpf))).ToLowerInvariant();

    // Segredo TOTP: 20 bytes aleatórios (160 bits) codificados em Base32 (RFC 4648, sem padding).
    public static string GerarSegredoTotp()
    {
        var bytes = RandomNumberGenerator.GetBytes(20);
        var sb = new StringBuilder();
        int buffer = 0, bits = 0;
        foreach (var b in bytes)
        {
            buffer = (buffer << 8) | b;
            bits += 8;
            while (bits >= 5)
            {
                sb.Append(Base32Alfabeto[(buffer >> (bits - 5)) & 31]);
                bits -= 5;
            }
        }
        if (bits > 0) sb.Append(Base32Alfabeto[(buffer << (5 - bits)) & 31]);
        return sb.ToString();
    }
}
