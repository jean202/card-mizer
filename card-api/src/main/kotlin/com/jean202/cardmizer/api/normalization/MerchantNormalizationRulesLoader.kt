package com.jean202.cardmizer.api.normalization

import org.yaml.snakeyaml.Yaml

class MerchantNormalizationRulesLoader {

    fun loadDefault(): MerchantNormalizationRules = load(DEFAULT_RESOURCE_PATH)

    fun load(resourcePath: String): MerchantNormalizationRules {
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Normalization rules resource not found: $resourcePath")

        inputStream.use { stream ->
            val loaded = Yaml().load<Any>(stream)
            val root = loaded as? Map<*, *>
                ?: throw IllegalStateException("Normalization rules must be a YAML object: $resourcePath")

            return MerchantNormalizationRules(
                categoryAliases = stringMap(root["categoryAliases"]),
                tagAliases = stringMap(root["tagAliases"]),
                simplePayTags = stringSet(root["simplePayTags"]),
                onlineCategories = stringSet(root["onlineCategories"]),
                offlineCategories = stringSet(root["offlineCategories"]),
                subscriptionCategories = stringSet(root["subscriptionCategories"]),
                digitalContentCategories = stringSet(root["digitalContentCategories"]),
                merchantRules = merchantRules(root["merchantRules"]),
            )
        }
    }

    companion object {
        const val DEFAULT_RESOURCE_PATH = "normalization/merchant-rules.yml"

        private fun stringMap(value: Any?): Map<String, String> {
            value ?: return emptyMap()
            val rawMap = value as? Map<*, *>
                ?: throw IllegalStateException("Expected YAML map but got: ${value::class.simpleName}")
            return rawMap.entries.associate { asString(it.key) to asString(it.value) }
        }

        private fun stringSet(value: Any?): Set<String> {
            value ?: return emptySet()
            val rawList = value as? List<*>
                ?: throw IllegalStateException("Expected YAML list but got: ${value::class.simpleName}")
            return rawList.map { asString(it) }.toSet()
        }

        private fun merchantRules(value: Any?): List<MerchantNormalizationRule> {
            value ?: return emptyList()
            val rawList = value as? List<*>
                ?: throw IllegalStateException("Expected YAML list but got: ${value::class.simpleName}")
            return rawList.map { item ->
                val rawRule = item as? Map<*, *>
                    ?: throw IllegalStateException("Expected merchant rule object but got: $item")
                MerchantNormalizationRule(
                    category = asString(rawRule["category"]),
                    keywords = stringSet(rawRule["keywords"]),
                    inferredTags = stringSet(rawRule["inferredTags"]),
                )
            }
        }

        private fun asString(value: Any?): String {
            val s = value as? String
            require(!s.isNullOrBlank()) { "Expected non-blank string but got: $value" }
            return s
        }
    }
}
