package br.com.meetpen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.ui.theme.TextSecondary

@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onSubscribe: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header com badge e botão fechar ──────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                // Badge no canto superior esquerdo
                Surface(
                    modifier = Modifier
                        .padding(start = 24.dp, top = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Meet Pen Prime",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                // Botão fechar no canto superior direito
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Headline Editorial ────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // "Pense. Diga." em cinza
                Text(
                    text = "Pense. Diga.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 44.sp,
                    letterSpacing = (-1).sp
                )
                // "Meet Pen." em branco com destaque laranja
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                            append("Meet Pen")
                        }
                        withStyle(SpanStyle(color = AmberMain, fontWeight = FontWeight.ExtraBold)) {
                            append(".")
                        }
                    },
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Subtítulo
                Text(
                    text = "Construa sua biblioteca pessoal de pensamentos com o Meet Pen Prime.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ── Lista de benefícios ───────────────────────────────────
                val features = listOf(
                    "Tudo do plano gratuito",
                    "Gravações ilimitadas",
                    "Gravações mais longas (60 minutos)",
                    "Geração de resumos e tarefas com IA",
                    "Transcrição automática precisa",
                    "Efeitos de voz e compartilhamento de áudio",
                    "Sem anúncios, sem interrupções"
                )
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓",
                            color = AmberMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // ── Depoimentos ───────────────────────────────────────────────
            TestimonialCard(
                name = "Carlos M.",
                text = "\"Uso o Meet Pen em todas as minhas reuniões. Ele organiza tudo de forma tão clara que virou indispensável para minha rotina.\""
            )
            Spacer(modifier = Modifier.height(12.dp))
            TestimonialCard(
                name = "Ana T.",
                text = "\"O Meet Pen se tornou meu app favorito. Ele prioriza exatamente o que importa e transforma minhas gravações em ações concretas.\""
            )
            Spacer(modifier = Modifier.height(12.dp))
            TestimonialCard(
                name = "Rafael A.",
                text = "\"Uso o Meet Pen para organizar ideias, ter resumos e depois usar como rascunho para relatórios. Um superpoder no bolso.\""
            )

            Spacer(modifier = Modifier.height(80.dp))
        }

        // ── Botão de CTA fixo na parte inferior ───────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0A0A0C), Color(0xFF0A0A0C)),
                        startY = 0f,
                        endY = 200f
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberMain,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Assinar por R$ 129,90/ano",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Equivale a R$ 10,82 por mês  •  Cancele quando quiser",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TestimonialCard(name: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "— $name",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
