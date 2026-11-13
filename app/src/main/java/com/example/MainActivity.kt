package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.ui.screens.AgeVerificationScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantPersona
import com.example.service.VoiceService
import com.example.service.WakeWordService
import com.example.ui.XiaomiAssistantViewModel
import com.example.ui.components.ChatBubble
import com.example.ui.components.QuickPromptsRow
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.VoiceWaveVisualizer
import com.example.ui.theme.XiaomiOrange
import com.example.ui.theme.XiaomiOrangeDark
import com.example.ui.theme.XiaomiOrangeLight
import com.example.ui.theme.XiaomiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: XiaomiAssistantViewModel by viewModels()
    private var voiceService: VoiceService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.loadAgeVerification(this)

        voiceService = VoiceService(
            context = this,
            onSpeechResult = { text ->
                runOnUiThread {
                    viewModel.onSpeechRecognized(text)
                }
            },
            onListeningStateChanged = { listening ->
                runOnUiThread {
                    viewModel.updateListeningState(listening)
                }
            },
            onAudioRmsChanged = { rms ->
                runOnUiThread {
                    viewModel.updateAudioRms(rms)
                }
            },
            onSpeakingStateChanged = { speaking, msgId ->
                runOnUiThread {
                    viewModel.updateSpeakingState(speaking, msgId)
                }
            }
        )

        voiceService?.let {
            viewModel.setVoiceServiceInstance(it)
        }

        setContent {
            XiaomiTheme {
                val isAgeVerified by viewModel.isAgeVerified.collectAsState()

                if (!isAgeVerified) {
                    AgeVerificationScreen(
                        onConfirmAge = {
                            viewModel.confirmAgeVerification(this@MainActivity)
                        }
                    )
                } else {
                    XiaomiAppContent(
                        viewModel = viewModel,
                        onStartWakeWordService = { startWakeWordService() },
                        onStopWakeWordService = { stopWakeWordService() },
                        onResetAgeVerification = {
                            viewModel.resetAgeVerification(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    private fun startWakeWordService() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 101)
            return
        }

        val intent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        viewModel.setWakeWordServiceRunning(true)
        Toast.makeText(this, "Xiaomi AI ব্যাকগ্রাউন্ড সার্ভিস চালু হয়েছে", Toast.LENGTH_SHORT).show()
    }

    private fun stopWakeWordService() {
        stopService(Intent(this, WakeWordService::class.java))
        viewModel.setWakeWordServiceRunning(false)
        Toast.makeText(this, "Xiaomi AI সার্ভিস বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        voiceService?.destroy()
        voiceService = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaomiAppContent(
    viewModel: XiaomiAssistantViewModel,
    onStartWakeWordService: () -> Unit,
    onStopWakeWordService: () -> Unit,
    onResetAgeVerification: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val messages by viewModel.messages.collectAsState()
    val currentPersona by viewModel.currentPersona.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val isFemaleVoice by viewModel.isFemaleVoice.collectAsState()
    val isWakeWordRunning by viewModel.isWakeWordServiceRunning.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to latest message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val recordGranted = result[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "ভয়েস ইনপুটের জন্য মাইক্রোফোন অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    // Speech Recognizer Intent Launcher as secondary fallback
    val speechRecognizerIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            viewModel.sendMessage(spokenText)
        }
    }

    fun handleMicClick() {
        if (isListening) {
            viewModel.stopListening()
            return
        }

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            try {
                viewModel.startListening()
            } catch (e: Exception) {
                // Launch standard RecognizerIntent
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "বাংলায় বলুন...")
                }
                speechRecognizerIntentLauncher.launch(intent)
            }
        } else {
            val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSettingsSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(XiaomiOrangeLight, XiaomiOrange, XiaomiOrangeDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentPersona.emoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Xiaomi AI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = XiaomiOrange.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${currentPersona.titleBn} (${currentPersona.age}+)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = XiaomiOrange,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isFemaleVoice) "বাংলা ও English • মেয়েদের মিষ্টি কণ্ঠ 👧" else "বাংলা ও English ভয়েস সহকারী",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "নতুন কথোপকথন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "সেটিংস",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Sound Wave & State Visualizer
            VoiceWaveVisualizer(
                isListening = isListening,
                isSpeaking = speakingMessageId != null,
                isLoading = isLoading,
                audioRms = audioRms,
                onMicClick = { handleMicClick() }
            )

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isSpeaking = speakingMessageId == message.id,
                        onSpeakToggle = { viewModel.toggleSpeakMessage(message) }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = XiaomiOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${currentPersona.titleBn} উত্তর ভাবছে…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Prompt Chips
            QuickPromptsRow(
                prompts = viewModel.quickPrompts,
                onPromptClick = { promptText ->
                    viewModel.sendMessage(promptText)
                },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Mic Button
                    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                    val micScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isListening) 1.25f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "mic_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(micScale)
                            .clip(CircleShape)
                            .background(
                                if (isListening) XiaomiOrange else XiaomiOrange.copy(alpha = 0.12f)
                            )
                            .clickable { handleMicClick() }
                            .testTag("voice_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = if (isListening) "শুনছি" else "ভয়েসে বলুন",
                            tint = if (isListening) Color.White else XiaomiOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_input_field"),
                        placeholder = {
                            Text(
                                text = if (isListening) "শুনছি… বলুন (বাংলা / English)" else "প্রশ্ন বা মনের কথা লিখুন (বাংলা / English)…",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XiaomiOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val textToSend = inputText
                                    inputText = ""
                                    keyboardController?.hide()
                                    viewModel.sendMessage(textToSend)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val textToSend = inputText
                                inputText = ""
                                keyboardController?.hide()
                                viewModel.sendMessage(textToSend)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) XiaomiOrange else XiaomiOrange.copy(alpha = 0.3f)
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "পাঠান",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            currentPersona = currentPersona,
            isFemaleVoice = isFemaleVoice,
            speechRate = speechRate,
            speechPitch = speechPitch,
            isWakeWordRunning = isWakeWordRunning,
            onPersonaChange = { newPersona ->
                viewModel.switchPersona(newPersona)
            },
            onFemaleVoiceToggle = { enable ->
                viewModel.setFemaleVoice(enable)
            },
            onPreviewVoice = {
                viewModel.previewVoice()
            },
            onSpeechRateChange = { rate ->
                viewModel.setSpeechRate(rate)
            },
            onSpeechPitchChange = { pitch ->
                viewModel.setSpeechPitch(pitch)
            },
            onWakeWordToggle = { enable ->
                if (enable) {
                    onStartWakeWordService()
                } else {
                    onStopWakeWordService()
                }
            },
            onResetAgeVerification = onResetAgeVerification,
            onDismiss = { showSettingsSheet = false }
        )
    }
}
