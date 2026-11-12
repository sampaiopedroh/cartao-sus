package br.edu.cartaosus.ui.cadastro

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import br.edu.cartaosus.domain.Fala
import br.edu.cartaosus.ui.comum.anunciar
import br.edu.cartaosus.ui.comum.titulo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.ui.Acao
import br.edu.cartaosus.ui.comum.Aviso
import br.edu.cartaosus.ui.comum.BotaoPrincipal
import br.edu.cartaosus.ui.comum.BotaoSecundario
import br.edu.cartaosus.ui.comum.BotaoTexto
import br.edu.cartaosus.ui.comum.CampoTexto
import br.edu.cartaosus.ui.comum.CartaoSecao
import br.edu.cartaosus.ui.comum.MascaraVisual
import br.edu.cartaosus.ui.comum.TomAviso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(viewModel: CadastroViewModel, onVoltar: () -> Unit) {
    val form by viewModel.form.collectAsState()
    val erros by viewModel.erros.collectAsState()
    val acao by viewModel.acao.collectAsState()
    var senhaVisivel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar conta", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar para o login")
                    }
                },
            )
        },
    ) { espaco ->
        Column(
            modifier = Modifier.fillMaxSize().padding(espaco).imePadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val sucesso = acao as? Acao.Sucesso
            if (sucesso != null) {
                ConcluidoConta(viewModel, sucesso.valor.segredoTotp, sucesso.valor.mensagem, sucesso.valor, onVoltar)
                return@Column
            }

            Text("Preencha seus dados. Leva menos de um minuto.", style = MaterialTheme.typography.bodyLarge)

            CartaoSecao {
                Text("Seus dados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
                CampoTexto(form.nome, { viewModel.alterar("nome", it) }, "Nome completo", erro = erros.mapa["nomeCompleto"])
                CampoTexto(
                    form.nascimento, { viewModel.alterar("nascimento", it) }, "Data de nascimento",
                    erro = erros.mapa["dataNascimento"], ajuda = "Dia, mês e ano. Exemplo: 15/03/1950",
                    teclado = KeyboardType.Number, mascara = MascaraVisual(MascaraVisual.DATA),
                )
                CampoTexto(
                    form.cpf, { viewModel.alterar("cpf", it) }, "CPF",
                    erro = erros.mapa["cpf"], teclado = KeyboardType.Number, mascara = MascaraVisual(MascaraVisual.CPF),
                )
                CampoTexto(
                    form.cns, { viewModel.alterar("cns", it) }, "Número do Cartão SUS (CNS)",
                    erro = erros.mapa["numeroCns"], ajuda = "São 15 números, impressos no seu Cartão SUS",
                    teclado = KeyboardType.Number, mascara = MascaraVisual(MascaraVisual.CNS),
                )
            }

            CartaoSecao {
                Text("Seu acesso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
                CampoTexto(form.login, { viewModel.alterar("login", it) }, "E-mail", erro = erros.mapa["login"], teclado = KeyboardType.Email)
                CampoTexto(
                    form.senha, { viewModel.alterar("senha", it) }, "Senha",
                    erro = erros.mapa["senha"], ajuda = "Pelo menos 8 caracteres",
                    teclado = KeyboardType.Password, acaoIme = ImeAction.Done, aoConfirmar = viewModel::cadastrar,
                    senha = true, senhaVisivel = senhaVisivel,
                )
                BotaoTexto(if (senhaVisivel) "Ocultar senha" else "Mostrar senha", { senhaVisivel = !senhaVisivel })
            }

            val erroGeral = (acao as? Acao.Erro)?.mensagem
            if (erroGeral != null) {
                Aviso(
                    texto = erroGeral,
                    tom = TomAviso.ERRO,
                    acao = if (erroGeral == Erros.SEM_CONEXAO) ({ BotaoTexto("Tentar de novo", viewModel::cadastrar) }) else null,
                )
            }

            BotaoPrincipal("Criar conta", viewModel::cadastrar, carregando = acao is Acao.Carregando)
            BotaoSecundario("Já tenho conta", onVoltar)
        }
    }
}

@Composable
private fun ConcluidoConta(
    viewModel: CadastroViewModel,
    segredo: String,
    mensagem: String,
    resposta: br.edu.cartaosus.data.CadastroResponse,
    onVoltar: () -> Unit,
) {
    val area = LocalClipboardManager.current
    val salvo by viewModel.salvo.collectAsState()

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
        Text("Conta criada!", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.titulo().anunciar())
    }
    Text(mensagem, style = MaterialTheme.typography.bodyLarge)

    CartaoSecao(corFundo = MaterialTheme.colorScheme.primaryContainer) {
        Text("Seu código secreto", style = MaterialTheme.typography.titleMedium, modifier = Modifier.titulo())
        Text(
            segredo.chunked(4).joinToString(" "),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            // Lido letra por letra, em blocos de 4, para dar para anotar ouvindo.
            modifier = Modifier.semantics { contentDescription = "Código secreto: " + Fala.segredo(segredo) },
        )
        Text(
            "Ele gera o código que você mostra no atendimento. Aparece só agora, por isso salve neste aparelho.",
            style = MaterialTheme.typography.bodyMedium,
        )
        var copiado by remember { mutableStateOf(false) }
        BotaoSecundario(if (copiado) "Código copiado" else "Copiar código secreto", { area.setText(AnnotatedString(segredo)); copiado = true },
            modifier = Modifier.anunciar())
    }

    if (salvo) {
        Aviso("Código salvo neste aparelho. Você já pode entrar.", TomAviso.SUCESSO)
    } else {
        BotaoPrincipal("Salvar neste aparelho", { viewModel.salvarNoAparelho(resposta) })
    }
    BotaoSecundario("Ir para o login", onVoltar, modifier = Modifier.fillMaxWidth())
}
