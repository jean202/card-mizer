package com.jean202.cardmizer.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Card Mizer API")
                .version("0.1.0")
                .description("카드 실적과 혜택 우선순위를 계산해 결제 카드를 추천하는 API")
        )
}
