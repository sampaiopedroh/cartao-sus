package br.edu.cartaosus.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta inspirada no azul institucional, com contraste alto para leitura (WCAG AA).
object Cores {
    val Azul = Color(0xFF0B4FA8)
    val AzulClaro = Color(0xFFD9E6FA)
    val Verde = Color(0xFF0F7B3F)
    val VerdeClaro = Color(0xFFD8F3E1)
    val Alerta = Color(0xFFB3261E)
    val AlertaClaro = Color(0xFFFDE8E6)
    val Atencao = Color(0xFF8A5200)
    val AtencaoClaro = Color(0xFFFFEFD1)
}

private val Claro = lightColorScheme(
    primary = Cores.Azul,
    onPrimary = Color.White,
    primaryContainer = Cores.AzulClaro,
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Cores.Verde,
    onSecondary = Color.White,
    secondaryContainer = Cores.VerdeClaro,
    onSecondaryContainer = Color(0xFF00210E),
    tertiary = Cores.Atencao,
    tertiaryContainer = Cores.AtencaoClaro,
    onTertiaryContainer = Color(0xFF2B1700),
    error = Cores.Alerta,
    onError = Color.White,
    errorContainer = Cores.AlertaClaro,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF14181F),
    surface = Color.White,
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFE6EBF3),
    onSurfaceVariant = Color(0xFF394150),
    outline = Color(0xFF5F6B7A),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF0B4FA8),
    onPrimaryContainer = Color(0xFFD9E6FA),
    secondary = Color(0xFF8EDBA4),
    onSecondary = Color(0xFF00391B),
    secondaryContainer = Color(0xFF0F7B3F),
    onSecondaryContainer = Color(0xFFD8F3E1),
    tertiary = Color(0xFFFFB95C),
    tertiaryContainer = Color(0xFF633B00),
    onTertiaryContainer = Color(0xFFFFEFD1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFDE8E6),
    background = Color(0xFF101319),
    onBackground = Color(0xFFE3E7EF),
    surface = Color(0xFF181C23),
    onSurface = Color(0xFFE3E7EF),
    surfaceVariant = Color(0xFF2A303A),
    onSurfaceVariant = Color(0xFFC3CAD6),
    outline = Color(0xFF8D96A5),
)

// Textos maiores que o padrão: leitura confortável para todas as idades.
private val Tipografia = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

private val Formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TemaCartaoSus(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        typography = Tipografia,
        shapes = Formas,
        content = conteudo,
    )
}
