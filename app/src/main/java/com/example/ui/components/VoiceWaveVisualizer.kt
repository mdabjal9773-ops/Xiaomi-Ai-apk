package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.XiaomiOrange
import com.example.ui.theme.XiaomiOrangeDark
import com.example.ui.theme.XiaomiOrangeLight

@Composable
fun VoiceWaveVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    isLoading: Boolean,
    audioRms: Float,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isSpeaking || isLoading) 1.22f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 400 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Glowing Orb & Live Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp)
                ) {
                    // Outer glow halo
                    Canvas(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                rotationZ = rotationAngle
                            }
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2
                        val gradient = Brush.radialGradient(
                            colors = listOf(
                                XiaomiOrange.copy(alpha = if (isListening) 0.6f else 0.35f),
                                XiaomiOrangeLight.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        )
                        drawCircle(brush = gradient, radius = radius, center = center)
                    }

                    // Inner glowing circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(XiaomiOrangeLight, XiaomiOrange, XiaomiOrangeDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isListening -> Icons.Default.Mic
                                isSpeaking -> Icons.Default.VolumeUp
                                else -> Icons.Default.GraphicEq
                            },
                            contentDescription = "Assistant State",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when {
                            isListening -> "শুনছি… বাংলায় বলুন"
                            isSpeaking -> "উত্তর পড়ে শোনাচ্ছি…"
                            isLoading -> "চিন্তা করছি…"
                            else -> "Xiaomi AI সক্রিয় ও প্রস্তুত"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                        color = if (isListening) XiaomiOrange else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            isListening -> "মাইকে আপনার কণ্ঠস্বর রিসিভ হচ্ছে"
                            isSpeaking -> "বাংলা স্পিচ আউটপুট চলছে"
                            isLoading -> "Gemini AI উত্তর তৈরি করছে"
                            else -> "ভয়েস বা টেক্সটে যেকোনো প্রশ্ন করুন"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Animated Audio Waveform Bars
            AnimatedWaveformBars(
                isActive = isListening || isSpeaking,
                isListening = isListening,
                audioRms = audioRms
            )
        }
    }
}

@Composable
private fun AnimatedWaveformBars(
    isActive: Boolean,
    isListening: Boolean,
    audioRms: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(28.dp)
    ) {
        val barCounts = 5
        val infiniteTransition = rememberInfiniteTransition(label = "wave_bars")

        for (i in 0 until barCounts) {
            val animHeight by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = if (isActive) (14f + (i * 3.5f)) else 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 250 + (i * 70), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val displayHeight = if (isListening && audioRms > 0) {
                (animHeight + (audioRms * 1.5f)).coerceIn(6f, 26f)
            } else if (isActive) {
                animHeight
            } else {
                6f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(displayHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) XiaomiOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
            )
        }
    }
}
