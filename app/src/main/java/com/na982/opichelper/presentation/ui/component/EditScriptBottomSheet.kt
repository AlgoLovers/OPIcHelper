package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.usecase.SentencePair
import com.na982.opichelper.domain.usecase.ValidationError
import com.na982.opichelper.domain.usecase.ValidationResult
import com.na982.opichelper.presentation.viewmodel.EditScriptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScriptBottomSheet(
    qaItem: QaItem,
    isQuestion: Boolean,
    level: UserLevel,
    scriptIndex: Int,
    entityId: String,
    onDismiss: () -> Unit,
    viewModel: EditScriptViewModel = hiltViewModel()
) {
    val sentencePairs by viewModel.sentencePairs.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()

    LaunchedEffect(qaItem, isQuestion, level) {
        viewModel.loadSentences(qaItem, isQuestion, level)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "스크립트 편집",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    viewModel.restoreOriginal(entityId)
                    onDismiss()
                }) {
                    Text("원본 복원")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            sentencePairs.forEachIndexed { index, pair ->
                SentencePairEditor(
                    index = index,
                    totalCount = sentencePairs.size,
                    pair = pair,
                    errors = getErrorsForIndex(validationResult, index),
                    onKoreanChange = { viewModel.updatePair(index, korean = it) },
                    onEnglishChange = { viewModel.updatePair(index, english = it) },
                    onDelete = { viewModel.removePair(index) },
                    canDelete = sentencePairs.size > 1
                )

                if (index < sentencePairs.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { viewModel.addPair() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ 문장 쌍 추가")
            }

            if (validationResult.errors.any { it is ValidationError.MissingPunctuation }) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "마침표(.!?)로 끝나야 정상 분할됩니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButtonCompact(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.save(qaItem, isQuestion, level, scriptIndex)
                        onDismiss()
                    },
                    enabled = validationResult.isValid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("저장")
                }
            }
        }
    }
}

@Composable
private fun SentencePairEditor(
    index: Int,
    totalCount: Int,
    pair: SentencePair,
    errors: List<ValidationError>,
    onKoreanChange: (String) -> Unit,
    onEnglishChange: (String) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val koreanError = errors.any { it is ValidationError.EmptySentence && it.isKorean }
    val koreanPunctuationError = errors.any { it is ValidationError.MissingPunctuation && it.isKorean }
    val englishError = errors.any { it is ValidationError.EmptySentence && !it.isKorean }
    val englishPunctuationError = errors.any { it is ValidationError.MissingPunctuation && !it.isKorean }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("문장 쌍 삭제") },
            text = { Text("문장 ${index + 1}을(를) 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "문장 ${index + 1}/$totalCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (canDelete) {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = pair.korean,
            onValueChange = onKoreanChange,
            label = { Text("한국어", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            isError = koreanError || koreanPunctuationError,
            supportingText = if (koreanError) {
                { Text("빈 문장은 저장할 수 없습니다", fontSize = 10.sp) }
            } else if (koreanPunctuationError) {
                { Text("마침표가 필요합니다", fontSize = 10.sp) }
            } else null,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (koreanError || koreanPunctuationError)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = pair.english,
            onValueChange = onEnglishChange,
            label = { Text("English", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            isError = englishError || englishPunctuationError,
            supportingText = if (englishError) {
                { Text("빈 문장은 저장할 수 없습니다", fontSize = 10.sp) }
            } else if (englishPunctuationError) {
                { Text("마침표가 필요합니다", fontSize = 10.sp) }
            } else null,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (englishError || englishPunctuationError)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun OutlinedButtonCompact(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text)
    }
}

private fun getErrorsForIndex(validationResult: ValidationResult, index: Int): List<ValidationError> =
    validationResult.errors.filter { error ->
        when (error) {
            is ValidationError.EmptySentence -> error.index == index
            is ValidationError.MissingPunctuation -> error.index == index
        }
    }
