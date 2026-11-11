package br.edu.cartaosus.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import br.edu.cartaosus.data.Erros
import br.edu.cartaosus.ui.Acao

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onEntrou: () -> Unit,
    onCriarConta: () -> Unit,
) {
    val form by viewModel.form.collectAsState()
    val acao by viewModel.acao.collectAsState()
    val hospedeiro = remember { SnackbarHostState() }

    LaunchedEffect(acao) {
        when (val a = acao) {
            is Acao.Sucesso -> {
                viewModel.consumirAcao()
                onEntrou()
            }
            is Acao.Erro -> {
                val resultado = hospedeiro.showSnackbar(
                    message = a.mensagem,
                    actionLabel = if (a.mensagem == Erros.SEM_CONEXAO) "Tentar de novo" else null,
                )
                viewModel.consumirAcao()
                if (resultado == androidx.compose.material3.SnackbarResult.ActionPerformed) viewModel.entrar()
            }
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hospedeiro) }) { espaco ->
        Column(
            modifier = Modifier.fillMaxSize().padding(espaco).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Cartão SUS", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Entre com seu e-mail e senha", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Spacer16()

            OutlinedTextField(
                value = form.login,
                onValueChange = viewModel::alterarLogin,
                label = { Text("E-mail") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer16()
            OutlinedTextField(
                value = form.senha,
                onValueChange = viewModel::alterarSenha,
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer16()

            val carregando = acao is Acao.Carregando
            Button(
                onClick = viewModel::entrar,
                enabled = !carregando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (carregando) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text("Entrar")
                }
            }
            TextButton(onClick = onCriarConta, modifier = Modifier.fillMaxWidth()) {
                Text("Criar conta")
            }
        }
    }
}

@Composable
private fun Spacer16() = androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
