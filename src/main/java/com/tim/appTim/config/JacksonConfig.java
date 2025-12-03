package com.tim.appTim.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    public static class LocalDateTimeUtcSerializer extends JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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

    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getText();
            if (dateString == null || dateString.isEmpty()) {
                return null;
            }
            try {
                // Try ISO_OFFSET_DATE_TIME format first
                return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                // Fallback to ISO_LOCAL_DATE_TIME
                try {
                    return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException e2) {
                    throw new IOException("Cannot deserialize LocalDateTime from: " + dateString, e2);
                }
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // Register JavaTimeModule for JSR310 support
            // This handles LocalDate and LocalDateTime serialization/deserialization
            builder.modules(new JavaTimeModule());
            
            // Override LocalDateTime serializer/deserializer for UTC format
            SimpleModule customModule = new SimpleModule("CustomDateTimeModule");
            customModule.addSerializer(LocalDateTime.class, new LocalDateTimeUtcSerializer());
            customModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
            builder.modules(customModule);
            
            // Disable writing dates as timestamps - this ensures dates are written as strings
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}

