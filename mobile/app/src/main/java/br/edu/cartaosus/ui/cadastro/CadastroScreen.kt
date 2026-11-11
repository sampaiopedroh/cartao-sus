package br.edu.cartaosus.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.ui.Acao

@Composable
fun CadastroScreen(viewModel: CadastroViewModel, onVoltar: () -> Unit) {
    val form by viewModel.form.collectAsState()
    val erros by viewModel.erros.collectAsState()
    val acao by viewModel.acao.collectAsState()
    val hospedeiro = remember { SnackbarHostState() }
    val area = LocalClipboardManager.current

    LaunchedEffect(acao) {
        when (val a = acao) {
            is Acao.Erro -> {
                val r = hospedeiro.showSnackbar(
                    message = a.mensagem,
                    actionLabel = if (a.mensagem == Erros.SEM_CONEXAO) "Tentar de novo" else null,
                )
                viewModel.consumirAcao()
                if (r == SnackbarResult.ActionPerformed) viewModel.cadastrar()
            }
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hospedeiro) }) { espaco ->
        Column(
            modifier = Modifier.fillMaxSize().padding(espaco).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Criar conta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            val sucesso = acao as? Acao.Sucesso
            if (sucesso != null) {
                // O segredo TOTP aparece uma única vez (SDD 7.2, tela 2).
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(sucesso.valor.mensagem, style = MaterialTheme.typography.bodyMedium)
                        Text("Segredo TOTP", fontWeight = FontWeight.Bold)
                        Text(sucesso.valor.segredoTotp, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { area.setText(AnnotatedString(sucesso.valor.segredoTotp)) }) {
                            Text("Copiar segredo")
                        }
                        Text("Guarde este segredo agora. Ele não será exibido de novo.", style = MaterialTheme.typography.bodySmall)
                        val salvo by viewModel.salvo.collectAsState()
                        if (salvo) {
                            Text("Segredo salvo neste aparelho.", fontWeight = FontWeight.Bold)
                        } else {
                            Button(onClick = { viewModel.salvarNoAparelho(sucesso.valor) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Salvar neste aparelho")
                            }
                        }
                        Button(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) { Text("Concluir") }
                    }
                }
                return@Column
            }

            val carregando = acao is Acao.Carregando
            CampoTexto("E-mail", form.login, erros.mapa["login"], { viewModel.alterar("login", it) }, KeyboardType.Email)
            CampoTexto("Senha (mínimo 8 caracteres)", form.senha, erros.mapa["senha"], { viewModel.alterar("senha", it) }, KeyboardType.Password, senha = true)
            CampoTexto("CPF (11 dígitos)", form.cpf, erros.mapa["cpf"], { viewModel.alterar("cpf", it) }, KeyboardType.Number)
            CampoTexto("CNS (15 dígitos)", form.cns, erros.mapa["numeroCns"], { viewModel.alterar("cns", it) }, KeyboardType.Number)
            CampoTexto("Nome completo", form.nome, erros.mapa["nomeCompleto"], { viewModel.alterar("nome", it) }, KeyboardType.Text)
            CampoTexto("Nascimento (AAAA-MM-DD)", form.nascimento, erros.mapa["dataNascimento"], { viewModel.alterar("nascimento", it) }, KeyboardType.Number)

            Button(
                onClick = viewModel::cadastrar,
                enabled = !carregando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (carregando) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Cadastrar")
            }
            TextButton(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) { Text("Já tenho conta") }
        }
    }
}

@Composable
private fun CampoTexto(
    rotulo: String,
    valor: String,
    erro: String?,
    aoMudar: (String) -> Unit,
    teclado: KeyboardType,
    senha: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = { if (erro != null) Text(erro) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
