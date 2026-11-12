package br.edu.cartaosus.ui

import androidx.compose.runtime.Composable
import br.edu.cartaosus.ui.comum.EstadoCarregando
import br.edu.cartaosus.ui.comum.EstadoErro
import br.edu.cartaosus.ui.comum.EstadoVazio

// Os três estados de cada tela (SDD 7.2): carregando, vazio/sucesso e erro com "Tentar de novo".
data class Estado<T>(val carregando: Boolean = true, val erro: String? = null, val dados: T? = null)

@Composable
fun <T> ConteudoEstado(
    estado: Estado<T>,
    vazio: Boolean,
    mensagemVazio: String,
    onTentar: () -> Unit,
    textoVazio: String? = null,
    conteudo: @Composable (T) -> Unit,
) {
    when {
        estado.carregando && estado.dados == null -> EstadoCarregando()
        estado.erro != null -> EstadoErro(estado.erro, onTentar)
        vazio || estado.dados == null -> EstadoVazio(mensagemVazio, textoVazio)
        else -> conteudo(estado.dados)
    }
}
