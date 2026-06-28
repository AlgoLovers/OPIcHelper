package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.CategorySelector
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.MemorizeLevelSelector

@Composable
fun CategoryLevelRow(
    selectedCategory: String,
    categories: List<String>,
    selectedLevel: String,
    onCategorySelected: (String) -> Unit,
    onLevelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CategorySelector(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = onCategorySelected
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📚 학습할 주제를 선택하세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                MemorizeLevelSelector(
                    levels = MemorizeLevel.allDisplayNames,
                    selectedLevel = selectedLevel,
                    onLevelSelected = onLevelSelected
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎯 학습 난이도를 선택하세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
