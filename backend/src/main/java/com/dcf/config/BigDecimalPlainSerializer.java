package com.dcf.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Custom Jackson serializer for BigDecimal that ensures plain decimal string output
 * without scientific notation, regardless of the value size.
 */
public class BigDecimalPlainSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            // Use toPlainString() to ensure no scientific notation
            gen.writeString(value.toPlainString());
        }
    }
}