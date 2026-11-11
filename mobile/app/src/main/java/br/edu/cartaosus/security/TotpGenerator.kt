package br.edu.cartaosus.security

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// TOTP no aparelho (RFC 6238, SDD 7.5): mesmo algoritmo do servidor.
object TotpGenerator {
    private const val PASSO_SEGUNDOS = 30L
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun codigo(segredoBase32: String, agoraMs: Long = System.currentTimeMillis()): String {
        val contador = agoraMs / 1000 / PASSO_SEGUNDOS
        val dados = ByteBuffer.allocate(8).putLong(contador).array()
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(base32(segredoBase32), "HmacSHA1")) }
        val hash = mac.doFinal(dados)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binario = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        return (binario % 1_000_000).toString().padStart(6, '0')
    }

    // Segundos até o próximo código (1 a 30).
    fun restanteSegundos(agoraMs: Long = System.currentTimeMillis()): Int =
        (PASSO_SEGUNDOS - (agoraMs / 1000) % PASSO_SEGUNDOS).toInt()

    // Aceita só letras A–Z e dígitos 2–7 (Base32), com tamanho mínimo de 16.
    fun segredoValido(texto: String): Boolean {
        val limpo = texto.trim().uppercase().replace(" ", "")
        return limpo.length >= 16 && limpo.all { it in BASE32 }
    }

    fun base32(texto: String): ByteArray {
        val bytes = ArrayList<Byte>()
        var buffer = 0
        var bits = 0
        for (c in texto.uppercase().trimEnd('=')) {
            val valor = BASE32.indexOf(c)
            require(valor >= 0) { "Segredo TOTP inválido." }
            buffer = (buffer shl 5) or valor
            bits += 5
            if (bits >= 8) {
                bytes.add(((buffer shr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return bytes.toByteArray()
    }
}
