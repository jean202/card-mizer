package com.jean202.cardmizer.api.normalization

class MerchantNormalizationRule(
    val category: String,
    keywords: Set<String>,
    inferredTags: Set<String>,
) {
    val keywords: Set<String>
    val inferredTags: Set<String>

    init {
        require(category.isNotBlank()) { "category must not be blank" }
        require(keywords.isNotEmpty()) { "keywords must not be empty" }
        this.keywords = keywords.toSet()
        this.inferredTags = inferredTags.toSet()
    }
}
