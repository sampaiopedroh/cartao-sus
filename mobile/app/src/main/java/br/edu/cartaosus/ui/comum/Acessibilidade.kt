package br.edu.cartaosus.ui.comum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics

// Título de seção: o TalkBack permite pular de título em título (navegação por cabeçalhos).
fun Modifier.titulo(): Modifier = semantics { heading() }

// Agrupa um cartão para ser lido de uma vez, em vez de pedaço por pedaço.
fun Modifier.grupo(): Modifier = semantics(mergeDescendants = true) {}

// Mudança importante que deve ser anunciada sem a pessoa precisar procurar (erro, confirmação).
fun Modifier.anunciar(urgente: Boolean = false): Modifier =
    semantics { liveRegion = if (urgente) LiveRegionMode.Assertive else LiveRegionMode.Polite }

// Cada tela tem um nome: ao trocar de tela, o TalkBack anuncia onde a pessoa está.
@Composable
fun TelaNomeada(nome: String, conteudo: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().semantics { paneTitle = nome }) { conteudo() }
}
