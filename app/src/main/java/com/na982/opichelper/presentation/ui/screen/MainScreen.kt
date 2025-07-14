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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf(
        QuestionCategory.PERSONAL,
        QuestionCategory.TRAVEL,
        QuestionCategory.WORK
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                    label = { Text(category.name) },
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
            uiState.currentQuestion != null -> {
                val context = LocalContext.current
                val ttsPlayer = rememberTtsPlayer(context)
                QuestionCard(
                    question = uiState.currentQuestion!!
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ttsPlayer.speak(uiState.currentQuestion!!.question, rate = 0.8f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("재생")
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnswerCard(
                    answer = uiState.currentQuestion!!.sampleAnswer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.nextQuestion() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.currentCategory != null
        ) {
            Text("다음 질문")
        }
    }
}

@Composable
fun QuestionCard(
    question: com.na982.opichelper.domain.entity.Question,
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
                        text = question.question,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 카테고리 텍스트 제거
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
                        text = question.questionKo,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    )
}

@Composable
fun AnswerCard(
    answer: String,
    modifier: Modifier = Modifier
) {
    if (answer.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
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
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 