package it.matato.dietreminder.backend.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Stable wire contracts for native and TypeScript clients, including empty arrays/defaults. */
@Configuration
class ApiJsonConfig : WebMvcConfigurer {
    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.replaceAll { converter ->
            if (converter is KotlinSerializationJsonHttpMessageConverter)
                KotlinSerializationJsonHttpMessageConverter(Json { encodeDefaults = true; ignoreUnknownKeys = false })
            else converter
        }
    }
}
