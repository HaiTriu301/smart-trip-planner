package com.trieu.tripplanner.security;

import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Lets JJWT read/write the JWT header and payload with the application's Jackson 3 mapper (design.md 3.1).
 * Without this JJWT would need jjwt-jackson, which depends on Jackson 2.
 */
@RequiredArgsConstructor
public class JwtJsonCodec implements Serializer<Map<String, ?>>, Deserializer<Map<String, ?>> {

    private static final TypeReference<Map<String, ?>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    @Override
    public byte[] serialize(Map<String, ?> map) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(map);
        }
        catch (RuntimeException ex) {
            throw new SerializationException("Unable to serialize JWT JSON", ex);
        }
    }

    @Override
    public void serialize(Map<String, ?> map, OutputStream out) throws SerializationException {
        try {
            objectMapper.writeValue(out, map);
        }
        catch (RuntimeException ex) {
            throw new SerializationException("Unable to serialize JWT JSON", ex);
        }
    }

    @Override
    public Map<String, ?> deserialize(byte[] bytes) throws DeserializationException {
        try {
            return objectMapper.readValue(bytes, MAP_TYPE);
        }
        catch (RuntimeException ex) {
            throw new DeserializationException("Unable to deserialize JWT JSON", ex);
        }
    }

    @Override
    public Map<String, ?> deserialize(Reader reader) throws DeserializationException {
        try {
            return objectMapper.readValue(reader, MAP_TYPE);
        }
        catch (RuntimeException ex) {
            throw new DeserializationException("Unable to deserialize JWT JSON", ex);
        }
    }

}
