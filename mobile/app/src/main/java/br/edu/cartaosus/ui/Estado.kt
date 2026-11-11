package br.edu.cartaosus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Os três estados de cada tela (SDD 7.2): carregando, vazio/sucesso e erro com "Tentar de novo".
data class Estado<T>(val carregando: Boolean = true, val erro: String? = null, val dados: T? = null)

@Composable
fun <T> ConteudoEstado(
    estado: Estado<T>,
    vazio: Boolean,
    mensagemVazio: String,
    onTentar: () -> Unit,
    conteudo: @Composable (T) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        when {
            estado.carregando -> CircularProgressIndicator()
            estado.erro != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(estado.erro)
                Button(onClick = onTentar) { Text("Tentar de novo") }
            }
            vazio || estado.dados == null -> Text(mensagemVazio)
            else -> conteudo(estado.dados)
        }
    }
}
