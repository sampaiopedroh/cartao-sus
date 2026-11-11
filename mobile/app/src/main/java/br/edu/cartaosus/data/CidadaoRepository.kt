package br.edu.cartaosus.data

import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Telas 3 a 6. Medições que não chegam ao servidor vão para a fila local (SDD 7.6).
class CidadaoRepository(
    private val api: SusApi,
    private val sessao: SessionStore,
    private val banco: AppDatabase,
) {
    private val dao get() = banco.medicoes()

    fun id(): Long = sessao.idCidadao

    suspend fun prontuario(): ProntuarioResposta = api.prontuario(id())

    suspend fun alertas(): List<AlertaResumo> = api.alertas(id(), size = 20).content

    suspend fun marcarEntregue(idAlerta: Long) {
        api.marcarEntregue(id(), idAlerta)
    }

    suspend fun acessos(): List<AcessoResumo> = api.acessos(id())

    suspend fun sinais(tipo: String?): SinaisResposta = api.sinais(id(), tipo)

    fun pendentes(): Flow<List<MedicaoPendente>> = dao.observar()

    // Retorna true se o servidor recebeu; false se foi guardado na fila por falta de conexão.
    suspend fun registrarMedicao(tipo: String, valor: Double): Resultado {
        val agora = LocalDateTime.now().format(FORMATO)
        return try {
            val resposta = api.enviarSinal(SinalEnvio(tipo, valor, agora))
            Resultado.Enviado(resposta)
        } catch (_: IOException) {
            dao.inserir(MedicaoPendente(tipo = tipo, valor = valor, dataHora = agora))
            Resultado.Pendente
        }
    }

    // Envia a fila ao abrir a tela e ao voltar a conexão. Para na primeira falha de rede.
    suspend fun sincronizarPendentes(): Int {
        var enviados = 0
        for (item in dao.todas()) {
            try {
                api.enviarSinal(SinalEnvio(item.tipo, item.valor, item.dataHora))
                dao.remover(item)
                enviados++
            } catch (_: IOException) {
                break
            } catch (_: Exception) {
                dao.remover(item) // rejeitada pelo servidor (ex.: valor inválido): não fica presa na fila
            }
        }
        return enviados
    }

    sealed class Resultado {
        data class Enviado(val resposta: SinalVitalResposta) : Resultado()
        data object Pendente : Resultado()
    }

    private companion object {
        val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}
