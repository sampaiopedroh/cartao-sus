package br.edu.cartaosus.ui.login

import br.edu.cartaosus.ui.comum.titulo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.ui.Acao
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.BotaoTexto
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.TomAviso

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onEntrou: () -> Unit,
    onCriarConta: () -> Unit,
) {
    val form by viewModel.form.collectAsState()
    val acao by viewModel.acao.collectAsState()
    var senhaVisivel by remember { mutableStateOf(false) }

    LaunchedEffect(acao) {
        if (acao is Acao.Sucesso) {
            viewModel.consumirAcao()
            onEntrou()
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Cartão SUS", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.titulo())
            Text(
                "Seu cartão de saúde no celular",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CampoTexto(
                    valor = form.login,
                    aoMudar = viewModel::alterarLogin,
                    rotulo = "E-mail",
                    teclado = KeyboardType.Email,
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        CampoTexto(
                            valor = form.senha,
                            aoMudar = viewModel::alterarSenha,
                            rotulo = "Senha",
                            teclado = KeyboardType.Password,
                            acaoIme = ImeAction.Done,
                            aoConfirmar = viewModel::entrar,
                            senha = true,
                            senhaVisivel = senhaVisivel,
                        )
                    }
                }
                BotaoTexto(
                    texto = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                    onClick = { senhaVisivel = !senhaVisivel },
                )

                val erro = (acao as? Acao.Erro)?.mensagem
                if (erro != null) {
                    Aviso(
                        texto = erro,
                        tom = TomAviso.ERRO,
                        acao = if (erro == Erros.SEM_CONEXAO) ({ BotaoTexto("Tentar de novo", viewModel::entrar) }) else null,
                    )
                }

                Spacer(Modifier.height(4.dp))
                BotaoPrincipal("Entrar", viewModel::entrar, carregando = acao is Acao.Carregando)
                Spacer(Modifier.height(4.dp))
                BotaoSecundario("Criar conta", onCriarConta)
            }
        }
    }
}
