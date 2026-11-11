package br.edu.cartaosus.data

// Consulta de remédios, unidades e equipamentos (tela 5).
class SusRepository(private val api: SusApi) {
    suspend fun catalogo(nome: String): List<CatalogoItem> = api.catalogo(nome)

    suspend fun estoque(catmat: String): List<MedicamentoItem> =
        api.medicamentos(MUNICIPIO_SP, catmat, size = 20).content

    suspend fun unidades(): List<UnidadeResumo> = api.unidades().content

    suspend fun equipamentos(cnes: String): EquipamentosResposta = api.equipamentos(cnes)

    companion object {
        const val MUNICIPIO_SP = "355030"
    }
}
