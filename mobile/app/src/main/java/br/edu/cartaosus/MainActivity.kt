package br.edu.cartaosus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import br.edu.cartaosus.navegacao.NavegacaoApp
import br.edu.cartaosus.ui.tema.TemaCartaoSus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaCartaoSus {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavegacaoApp()
                }
            }
        }
    }
}
