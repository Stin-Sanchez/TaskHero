package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Converter
@Component
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    
    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    public EncryptionConverter(@Value("${app.db.encryption-key:TaskHeroSecretKey2026!}") String secretKey) {
        try {
            byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            
            // Usamos los primeros 16 bytes para la clave AES-128 (más compatible) o los 32 para AES-256
            // Para simplicidad y evitar problemas de políticas de exportación JCE, usamos 128 bits (16 bytes)
            byte[] keyBytes = Arrays.copyOf(key, 16);
            this.keySpec = new SecretKeySpec(keyBytes, AES);
            
            // IV fijo de 16 bytes derivado de la misma clave para determinismo en este converter simple
            // En un entorno de alta seguridad, el IV debería ser aleatorio y guardarse con el dato
            byte[] ivBytes = Arrays.copyOf(Arrays.copyOfRange(key, 16, 32), 16);
            this.ivSpec = new IvParameterSpec(ivBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar el motor de cifrado", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) return attribute;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar datos para la DB", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return dbData;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(dbData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Si el dato no está cifrado (ej. datos previos), retornamos el original o manejamos el error
            return dbData;
        }
    }
}
