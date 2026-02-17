package com.example.framework.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for transparently decrypting encrypted configuration properties.
 *
 * <p>Encrypted values are expected to use the following format:
 * <pre>
 * my.secret.property=ENC(base64(iv + ciphertext))
 * </pre>
 *
 * <p>The encryption algorithm is AES-256-GCM with a 12-byte IV and a key derived
 * from a shared secret using SHA-256. The shared secret is resolved in this
 * order:
 * <ol>
 *   <li>System property {@code config.secret}</li>
 *   <li>Environment variable {@code CONFIG_SECRET}</li>
 *   <li>Fallback demo default (NOT suitable for real secrets)</li>
 * </ol>
 *
 * <p>The demo fallback allows the test suite to run out-of-the-box but should
 * be overridden in real environments.</p>
 */
final class EncryptedPropertySupport {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    private static final String SECRET_SYS_PROPERTY = "config.secret";
    private static final String SECRET_ENV_VARIABLE = "CONFIG_SECRET";

    /**
     * Fallback secret for local/demo runs. Override via system property or
     * environment variable for any real usage.
     */
    private static final String DEFAULT_DEMO_SECRET = "PLEASE_OVERRIDE_ME_WITH_A_REAL_SECRET";

    // AES/GCM settings
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AES_KEY_LENGTH_BYTES = 32; // 256 bits

    private EncryptedPropertySupport() {
        // utility class
    }

    /**
     * Iterates over all properties and decrypts any value that uses the
     * {@code ENC(...)} format.
     */
    static void decryptAll(Properties properties) {
        Objects.requireNonNull(properties, "properties must not be null");

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            Object rawValue = entry.getValue();
            if (!(rawValue instanceof String)) {
                continue;
            }
            String value = (String) rawValue;
            String decrypted = decryptIfNecessary(value);
            if (!Objects.equals(value, decrypted)) {
                properties.put(key, decrypted);
            }
        }
    }

    /**
     * Returns the decrypted value if the input is in {@code ENC(...)} format;
     * otherwise returns the input as-is.
     */
    static String decryptIfNecessary(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith(ENC_PREFIX) || !trimmed.endsWith(ENC_SUFFIX)) {
            return value;
        }

        String base64 = trimmed.substring(ENC_PREFIX.length(), trimmed.length() - ENC_SUFFIX.length());
        try {
            byte[] ivAndCipher = Base64.getDecoder().decode(base64);
            if (ivAndCipher.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted value is too short to contain IV and ciphertext");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherBytes = new byte[ivAndCipher.length - IV_LENGTH_BYTES];
            System.arraycopy(ivAndCipher, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(ivAndCipher, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), spec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt configuration value", e);
        }
    }

    /**
     * Helper method that can be used from ad-hoc code or tests to produce
     * encrypted values for use in {@code config-*.properties} files.
     */
    static String encrypt(String plainText) {
        Objects.requireNonNull(plainText, "plainText must not be null");
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), spec);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] ivAndCipher = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(cipherBytes, 0, ivAndCipher, iv.length, cipherBytes.length);

            String base64 = Base64.getEncoder().encodeToString(ivAndCipher);
            return ENC_PREFIX + base64 + ENC_SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt configuration value", e);
        }
    }

    private static SecretKeySpec buildKey() {
        String secret = resolveSecret();
        byte[] keyBytes = sha256(secret.getBytes(StandardCharsets.UTF_8));

        if (keyBytes.length != AES_KEY_LENGTH_BYTES) {
            throw new IllegalStateException("Unexpected key length after SHA-256 hashing");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String resolveSecret() {
        String fromSystem = System.getProperty(SECRET_SYS_PROPERTY);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }

        String fromEnv = System.getenv(SECRET_ENV_VARIABLE);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        // Fallback for local/demo usage. For real secrets this should be
        // overridden via system property or environment variable.
        System.err.println("[Config] Using DEFAULT demo encryption secret. " +
                "Please override via -D" + SECRET_SYS_PROPERTY + " or " + SECRET_ENV_VARIABLE + " for real environments.");
        return DEFAULT_DEMO_SECRET;
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

