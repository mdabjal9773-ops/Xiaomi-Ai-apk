package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.QuickPrompt
import com.example.ui.theme.XiaomiOrange
import com.example.ui.theme.XiaomiOrangeDark
import com.example.ui.theme.XiaomiOrangeLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timeStr = rememberFormattedTime(message.timestamp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            // Assistant Avatar
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
                Text(
                    text = message.persona.emoji,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Sender name & Persona
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = if (message.isUser) "আপনি" else "${message.persona.titleBn} (Xiaomi)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (message.isUser) XiaomiOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Message Card
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser) {
                        XiaomiOrange
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.testTag("chat_card_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontSize = 15.sp
                        ),
                        color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    if (!message.isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // TTS Playback Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSpeaking) XiaomiOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSpeakToggle() }
                                    .testTag("speak_btn_${message.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = if (isSpeaking) "পড়া থামান" else "উত্তর শুনুন",
                                        tint = XiaomiOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "থামান" else "শুনুন",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = XiaomiOrange
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(message.text))
                                    Toast.makeText(context, "টেক্সট কপি হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "কপি করুন",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "আপনি",
                    tint = XiaomiOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuickPromptsRow(
    prompts: List<QuickPrompt>,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { item ->
            val iconVector = when (item.iconName) {
                "favorite" -> Icons.Default.Favorite
                "auto_stories" -> Icons.Default.AutoStories
                "phone_android" -> Icons.Default.PhoneAndroid
                "volunteer_activism" -> Icons.Default.VolunteerActivism
                "mood" -> Icons.Default.Mood
                else -> Icons.Default.SentimentSatisfied
            }

            AssistChip(
                onClick = { onPromptClick(item.prompt) },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = XiaomiOrange,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = XiaomiOrange.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("prompt_chip_${item.title}")
            )
        }
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
