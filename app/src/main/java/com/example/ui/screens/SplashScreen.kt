package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Smooth entry animations
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }

    var progress by remember { mutableFloatStateOf(0f) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    // Auto-advance progress timer (2.6 seconds total)
    LaunchedEffect(Unit) {
        val totalSteps = 26
        for (i in 1..totalSteps) {
            delay(100)
            progress = i / totalSteps.toFloat()
        }
        delay(150)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF132A46),
                        Color(0xFF0F1E34),
                        Color(0xFF09111E)
                    )
                )
            )
            .testTag("splash_screen_container")
    ) {
        // Tech Circuit / HUD Grid Backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Subdued tech grid lines
            val gridSpacing = 40.dp.toPx()
            for (x in 0..(canvasWidth / gridSpacing).toInt()) {
                val xPos = x * gridSpacing
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.04f),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, canvasHeight),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(canvasHeight / gridSpacing).toInt()) {
                val yPos = y * gridSpacing
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.04f),
                    start = Offset(0f, yPos),
                    end = Offset(canvasWidth, yPos),
                    strokeWidth = 1f
                )
            }

            // Decorative Glowing Tech Rings
            drawCircle(
                color = Color(0xFF0284C7).copy(alpha = 0.08f * glowAlpha),
                radius = 160.dp.toPx(),
                center = Offset(canvasWidth / 2, canvasHeight * 0.28f)
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.25f * glowAlpha),
                radius = 120.dp.toPx(),
                center = Offset(canvasWidth / 2, canvasHeight * 0.28f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Skip / Continue action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSplashFinished() }
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skip to App",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF7DD3FC)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Skip",
                            tint = Color(0xFF7DD3FC),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Developer Glowing Crest & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Futuristic Crest
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0369A1).copy(alpha = 0.6f),
                                    Color(0xFF0F172A).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .border(2.dp, Color(0xFF38BDF8).copy(alpha = glowAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top AI & Circuit Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFFE0F2FE)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Brain",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Urdu Calligraphy: Sadaqat Ali
                        Text(
                            text = "صداقت علی",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Code Symbol: </>
                        Text(
                            text = "< / >",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitles
                Text(
                    text = "Full Stack AI Developer",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 23.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sadaqat Ali",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF7DD3FC),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // About Developer Header
                Text(
                    text = "About Developer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Urdu Tagline
                Text(
                    text = "یہ ایپ صداقت علی نے بنائی ہے۔",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFBAE6FD),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contact & Info Cards
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Phone Number
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString("0318-6036054"))
                                Toast.makeText(context, "Phone copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "0318-6036054",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }

                    // Email Address
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString("TechSadaqat@gmail.com"))
                                Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TechSadaqat@gmail.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    // Full Physical Address
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Address",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Amjad Bhatti Kiryana Store, Basti Muslim Town, Opt Catholic Church, Nasar Town, Chak No 53/5-L Road, Adda Gamber, Okara Cantt 57460, Punjab, Pakistan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Loading Indicator & Continue Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSplashFinished,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_tracker_button")
                ) {
                    Text(
                        text = "Continue to Electric Meter Tracker",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}
