package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.entity.MainScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: String,
    categories: List<String>, // 외부에서 주입받음
    onCategorySelected: (String) -> Unit,
    screenState: MainScreenState,
    onHighlightReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "카테고리 선택",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory.ifEmpty { "카테고리를 선택하세요" },
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            Log.d("CategorySelector", "Category selected: $category")
                            // 모든 상태 초기화
                            screenState.resetAllPlayStates()
                            onHighlightReset()
                            // 카테고리 변경
                            onCategorySelected(category)
                            expanded = false
                            Log.d("CategorySelector", "Category changed, all states reset")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} 