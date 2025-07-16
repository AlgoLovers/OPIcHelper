package com.na982.opichelper.presentation.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MainUiState
import androidx.compose.ui.platform.LocalContext
import com.na982.opichelper.presentation.ui.component.FlipCard
import com.na982.opichelper.domain.entity.QaItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    ttsPlayer: com.na982.opichelper.presentation.ui.component.TtsPlayer,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OPic Helper",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.4f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Category Selection
        Text(
            text = "카테고리 선택",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        var expanded by remember { mutableStateOf(false) }
        val selectedCategory = uiState.currentCategory ?: "카테고리 선택"

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("카테고리") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            viewModel.selectCategory(category)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Question & Answer Display
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
            uiState.error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            uiState.currentQaItem != null -> {
                val currentQuestionSentenceIndex by ttsPlayer.currentQuestionSentenceIndex.collectAsState()
                val currentAnswerSentenceIndex by ttsPlayer.currentAnswerSentenceIndex.collectAsState()
                // 질문 카드에 하이라이트 적용
                QuestionCard(
                    qaItem = uiState.currentQaItem!!,
                    currentSentenceIndex = currentQuestionSentenceIndex
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ttsPlayer.speakQuestion(uiState.currentQaItem!!.questionEn, rate = 0.8f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("재생")
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 답변 카드 (플립, TTS)
                AnswerCard(
                    answerEn = uiState.currentQaItem!!.answerEn,
                    answerKo = uiState.currentQaItem!!.answerKo,
                    currentSentenceIndex = currentAnswerSentenceIndex
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ttsPlayer.speakAnswer(uiState.currentQaItem!!.answerEn, rate = 0.8f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("전체 재생")
                    }
                    Button(
                        onClick = {
                            ttsPlayer.speakBySentence(uiState.currentQaItem!!.answerEn, repeatCount = 5, pauseRatio = 1.5f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("문장별 따라하기")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                ttsPlayer.stop()
                viewModel.nextQaItem()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.currentCategory != null
        ) {
            Text("다음 질문")
        }
    }
}

@Composable
fun QuestionCard(
    qaItem: QaItem,
    currentSentenceIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    FlipCard(
        modifier = modifier,
        frontContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "질문",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // 질문도 문장별 하이라이트
                    val sentences = qaItem.questionEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                    sentences.forEachIndexed { idx, sentence ->
                        Text(
                            text = sentence,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentSentenceIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        backContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "질문",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = qaItem.questionKo,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    )
}

@Composable
fun AnswerCard(
    answerEn: String,
    answerKo: String,
    currentSentenceIndex: Int?,
    modifier: Modifier = Modifier
) {
    if (answerEn.isNotEmpty() || answerKo.isNotEmpty()) {
        FlipCard(
            modifier = modifier,
            frontContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "샘플 답변",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val sentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                        sentences.forEachIndexed { idx, sentence ->
                            Text(
                                text = sentence,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                                color = if (currentSentenceIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            },
            backContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "샘플 답변",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = answerKo,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        )
    }
} 