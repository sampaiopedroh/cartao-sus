package br.edu.cartaosus.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Tela provisória (F7). As telas 3 a 6 do SDD entram na F8.
@Composable
fun InicioScreen(nome: String, onSair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Olá, $nome", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Seu Cartão SUS está pronto para uso.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onSair, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("Sair")
        }
    }
}
