using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;

namespace CartaoSus.Api.Security;

// RFC 6238 (TOTP): HMAC-SHA1, código de 6 dígitos, passo de 30 segundos (SDD 8.4).
public static class TotpService
{
    private const int Digitos = 6;
    private const int PassoSegundos = 30;
    private const string Base32Alfabeto = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static long Contador(DateTimeOffset instante, int deslocamento = 0) =>
        instante.ToUnixTimeSeconds() / PassoSegundos + deslocamento;

    public static string Gerar(string segredoBase32, long contador)
    {
        var chave = Base32Decode(segredoBase32);
        Span<byte> dados = stackalloc byte[8];
        System.Buffers.Binary.BinaryPrimitives.WriteInt64BigEndian(dados, contador);
        var hash = HMACSHA1.HashData(chave, dados);
        int offset = hash[^1] & 0x0F;
        int binario = ((hash[offset] & 0x7F) << 24) | (hash[offset + 1] << 16)
                      | (hash[offset + 2] << 8) | hash[offset + 3];
        return (binario % 1_000_000).ToString().PadLeft(Digitos, '0');
    }

    // Retorna o contador que validou o código (janela de -1, 0, +1), ou null se nenhum bater.
    public static long? ValidarJanela(string segredoBase32, string codigo, DateTimeOffset agora)
    {
        if (codigo.Length != Digitos) return null;
        var recebido = Encoding.ASCII.GetBytes(codigo);
        foreach (var delta in new[] { 0, -1, 1 })
        {
            var contador = Contador(agora, delta);
            var esperado = Encoding.ASCII.GetBytes(Gerar(segredoBase32, contador));
            if (CryptographicOperations.FixedTimeEquals(esperado, recebido)) return contador;
        }
        return null;
    }

    private static byte[] Base32Decode(string s)
    {
        var bytes = new List<byte>();
        int buffer = 0, bits = 0;
        foreach (var c in s.ToUpperInvariant().TrimEnd('='))
        {
            int val = Base32Alfabeto.IndexOf(c);
            if (val < 0) throw new FormatException("Segredo TOTP inválido.");
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8)
            {
                bytes.Add((byte)((buffer >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        return bytes.ToArray();
    }
}

// Anti-reuso: um código de uma janela de tempo só pode ser aceito uma vez por cidadão.
// Em memória, válido para uma única instância da API (limitação registrada no README).
public class AntiReusoTotp
{
    private readonly ConcurrentDictionary<long, long> _ultimoContador = new();

    public bool Registrar(long idCidadao, long contador)
    {
        while (true)
        {
            if (!_ultimoContador.TryGetValue(idCidadao, out var anterior))
            {
                if (_ultimoContador.TryAdd(idCidadao, contador)) return true;
                continue;
            }
            if (contador <= anterior) return false;
            if (_ultimoContador.TryUpdate(idCidadao, contador, anterior)) return true;
        }
    }
}
