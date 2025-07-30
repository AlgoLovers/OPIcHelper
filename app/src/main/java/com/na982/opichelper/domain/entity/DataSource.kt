package com.na982.opichelper.domain.entity

/**
 * 사용자가 선택할 수 있는 데이터 소스
 */
enum class DataSource(val folderName: String, val displayName: String) {
    AL("al", "AL"),
    IH("ih", "IH"),
    IH_RAW("ih_raw", "IH Raw"),
    IM("im", "IM");
    
    companion object {
        fun fromFolderName(folderName: String): DataSource? {
            return values().find { it.folderName == folderName }
        }
    }
} 