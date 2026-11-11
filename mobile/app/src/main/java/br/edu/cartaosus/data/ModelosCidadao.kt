package br.edu.cartaosus.data

// DTOs das telas 3 a 6 (SDD 5.3). Datas chegam como texto ISO ("2026-11-09T07:15:00").

data class ProntuarioHistorico(val tipo: String, val descricao: String, val severidade: String, val dataRegistro: String)

data class AlertaResumo(
    val idAlerta: Long,
    val tipo: String,
    val severidade: String,
    val mensagem: String,
    val statusEnvio: String,
    val criadoEm: String,
)

data class ProntuarioResposta(
    val idCidadao: Long,
    val nomeCompleto: String,
    val cns: String,
    val historico: List<ProntuarioHistorico>,
    val alertasPendentes: List<AlertaResumo>,
)

data class PaginaAlertas(val content: List<AlertaResumo>, val page: Int, val size: Int, val totalElements: Long)

data class AcessoResumo(
    val idSessao: Long,
    val statusAutenticacao: String,
    val motivo: String?,
    val dataHoraAcesso: String,
    val idUnidade: Long,
)

data class MedicaoResumo(val valor: Double, val origem: String, val dataHora: String)

data class ResumoMedicoes(val media: Double?, val minima: Double?, val maxima: Double?, val total: Int)

data class SinaisResposta(val tipo: String?, val medicoes: List<MedicaoResumo>, val resumo: ResumoMedicoes)

// Envio de medição (E6). Campos nulos não são enviados (o Gson os omite).
data class SinalEnvio(val tipoMedicao: String, val valor: Double, val dataHora: String? = null)

data class AlertaDisparado(val tipo: String, val mensagem: String)

data class SinalVitalResposta(val idSinal: Long, val alertaDisparado: Boolean, val alerta: AlertaDisparado?)

data class UnidadeResumo(
    val id: Long,
    val codigoCnes: String,
    val nome: String,
    val tipo: String,
    val bairro: String,
    val latitude: Double?,
    val longitude: Double?,
)

data class PaginaUnidades(val content: List<UnidadeResumo>, val page: Int, val size: Int, val totalElements: Long)

data class CatalogoItem(val nome: String, val apresentacao: String, val catmat: String)

data class MedicamentoItem(
    val nome: String,
    val catmat: String,
    val municipio: String?,
    val estabelecimento: String?,
    val quantidade: Double?,
    val dataPosicao: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class PaginaMedicamentos(val content: List<MedicamentoItem>, val page: Int, val size: Int, val totalElements: Long)

data class EquipamentoItem(val tipo: String, val descricao: String, val quantidadeExistente: Int?, val quantidadeEmUso: Int?)

data class EquipamentosResposta(val coCnes: String, val estabelecimento: String?, val equipamentos: List<EquipamentoItem>)
