package com.tim.appTim.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /**
     * Custom serializer để convert LocalDateTime thành ISO-8601 string với timezone UTC
     * Format: "2025-03-12T03:22:00Z" (với Z để chỉ UTC)
     */
    public static class LocalDateTimeUtcSerializer extends StdSerializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        public LocalDateTimeUtcSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                // Convert LocalDateTime sang OffsetDateTime với UTC timezone
                String formatted = value.atOffset(ZoneOffset.UTC).format(FORMATTER);
                gen.writeString(formatted);
            }
        }
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // Sử dụng custom serializer để LocalDateTime được serialize thành ISO-8601 với UTC timezone
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeUtcSerializer());
        
        return builder
                .modules(javaTimeModule)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}

