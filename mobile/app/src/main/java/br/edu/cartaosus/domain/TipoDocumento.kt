package br.edu.cartaosus.domain

// Tipos de documento que a pessoa pode anexar. O nome salvo no banco local é o `name` do enum.
enum class TipoDocumento(val rotulo: String) {
    CARTEIRINHA_SUS("Carterinha do SUS"),
    RG("RG"),
    CPF("CPF"),
    LAUDO("Laudo médico"),
    CIRURGIA("Cirurgia"),
    RECEITA("Receita de remédio"),
    CONSULTA("Consulta"),
    OUTRO("Outro");

    companion object {
        fun de(nome: String): TipoDocumento = entries.firstOrNull { it.name == nome } ?: OUTRO

        // Sugere um título quando o nome do arquivo é "scan_0001.pdf" ou parecido.
        fun tituloSugerido(nomeArquivo: String?, tipo: TipoDocumento): String {
            val base = nomeArquivo?.substringBeforeLast('.')?.trim().orEmpty()
            return if (base.length >= 3) base else tipo.rotulo
        }
    }
}
