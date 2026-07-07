package br.com.meetpen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.meetpen.ui.theme.AmberMain
import br.com.meetpen.ui.theme.MidnightBg
import br.com.meetpen.ui.theme.MidnightSurface
import br.com.meetpen.ui.theme.TextSecondary

@Composable
fun AccountDialog(
    userEmail: String?,
    isSubscribed: Boolean,
    recordingCount: Int,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onUpgrade: () -> Unit
) {
    val freeLimit = br.com.meetpen.logic.SettingsManager.FREE_USAGE_LIMIT
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF13151A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Top bar ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // ── Seção: Título "Conta" ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Conta",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // E-mail
                    if (userEmail != null) {
                        Text(
                            text = userEmail,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "Não conectado",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botões de ação no topo (modelo AudioPen)
                    if (userEmail != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // "Feedback?"
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AmberMain
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, AmberMain.copy(alpha = 0.6f)
                                )
                            ) {
                                Text("Feedback?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }

                            // "Sair"
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White.copy(alpha = 0.8f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Text("Sair", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Login com Google ainda não implementado
                        Text(
                            text = "Login com Google em breve",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // ── Seção: Plano ──────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    SectionHeader(title = "Plano")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card de plano atual (fundo escuro destacado — igual AudioPen)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E2128)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "tipo de conta: ${if (isSubscribed) "Prime" else "Gratuita"}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 15.sp
                            )

                            if (!isSubscribed) {
                                Spacer(modifier = Modifier.height(20.dp))
                                // Card de upgrade (igual ao "Go further" do AudioPen)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF252830)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(
                                            text = "Vá mais longe com Meet Pen Prime",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Gravações ilimitadas, resumos com IA, efeitos de voz e muito mais.\n\nVocê já usou $recordingCount de $freeLimit gravações disponíveis.",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = onUpgrade,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AmberMain,
                                                contentColor = Color.White
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)
                                        ) {
                                            Text(
                                                text = "Assinar Meet Pen Prime",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓  Gravações ilimitadas ativas",
                                    color = AmberMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // ── Seção: Gravações de Áudio ────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    SectionHeader(title = "Gravações de Áudio")

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E2128)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberMain),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$recordingCount ${if (recordingCount == 1) "gravação" else "gravações"}",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val limit = if (isSubscribed) "ilimitado" else "limite: $freeLimit"
                                    Text(
                                        text = limit,
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!isSubscribed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val progress = (recordingCount.toFloat() / freeLimit).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = AmberMain,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$recordingCount de $freeLimit usadas",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // ── Seção: Suporte ───────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    SectionHeader(title = "Suporte")

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E2128)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Algo não está funcionando como deveria?",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                // ── Rodapé ───────────────────────────────────────────────
                Spacer(modifier = Modifier.height(32.dp))
                if (userEmail != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = onLogout) {
                            Text(
                                text = "excluir conta",
                                color = Color.Red.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Meet Pen v${br.com.meetpen.BuildConfig.VERSION_NAME}",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Linha decorativa laranja — igual ao AudioPen
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.5.dp)
                .background(
                    color = AmberMain,
                    shape = CircleShape
                )
        )
    }
}
