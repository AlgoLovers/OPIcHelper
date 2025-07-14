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
import com.na982.opichelper.presentation.ui.component.rememberTtsPlayer
import com.na982.opichelper.presentation.ui.component.FlipCard
import com.na982.opichelper.domain.entity.QaItem

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OPic Helper",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Category Selection
        Text(
            text = "카테고리 선택",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) },
                    selected = uiState.currentCategory == category
                )
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
                val context = LocalContext.current
                val ttsPlayer = rememberTtsPlayer(context)
                QuestionCard(
                    qaItem = uiState.currentQaItem!!
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ttsPlayer.speak(uiState.currentQaItem!!.questionEn, rate = 0.8f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("재생")
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 답변 카드 (플립, TTS)
                AnswerCard(
                    answerEn = uiState.currentQaItem!!.answerEn,
                    answerKo = uiState.currentQaItem!!.answerKo
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ttsPlayer.speak(uiState.currentQaItem!!.answerEn, rate = 0.8f)
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
            onClick = { viewModel.nextQaItem() },
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
                    Text(
                        text = qaItem.questionEn,
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                        Text(
                            text = answerEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
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