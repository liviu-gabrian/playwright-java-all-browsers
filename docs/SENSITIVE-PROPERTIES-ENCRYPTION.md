# Encryption and Decryption of Sensitive Properties

This document describes how the framework handles **encryption** and **decryption** of sensitive configuration properties (e.g. API keys, passwords, tokens) so they can be stored in property files with reduced risk of exposure.

---

## Why encrypt sensitive properties?

- **Config files** such as `config-test.properties` and `config-uat.properties` may be committed to version control or copied across environments.
- **Sensitive values** (TestRail API key, service passwords, tokens) should not be stored in plain text in those files.
- The framework supports **encrypting** such values and **decrypting** them at runtime so that:
  - Property files can safely contain placeholders like `ENC(base64...)`.
  - Only processes that have the **shared secret** can decrypt and use the real values.
  - Callers (e.g. `ConfigManager.get("testrail.apiKey")`) always receive the **plain-text** value; encryption is transparent.

---

## How it works

### 1. Format of encrypted values

In any `config-<env>.properties` file, a sensitive value is stored as:

```properties
# Plain (not encrypted):
some.key=plainValue

# Encrypted (decrypted automatically at load time):
testrail.apiKey=ENC(<base64-encoded-IV-and-ciphertext>)
```

- **Prefix/suffix**: The value must be wrapped with `ENC(` and `)`.
- **Payload**: Inside the parentheses, a **Base64**-encoded string that represents:
  - **12 bytes**: IV (initialization vector) for AES-GCM.
  - **Remaining bytes**: Ciphertext (including the GCM authentication tag).

Only values in this form are decrypted; all other values are left unchanged.

### 2. Algorithm and key derivation

- **Cipher**: AES-256-GCM (authenticated encryption).
- **IV**: 12 bytes, randomly generated at encryption time, stored with the ciphertext.
- **Key**: 256-bit AES key derived from a **shared secret**:
  - The secret is a string (e.g. a passphrase or key material).
  - The key is computed as **SHA-256(secret)** and used as the raw AES key.

So encryption and decryption both depend on the same **shared secret**.

### 3. Where the shared secret comes from (decryption)

At **runtime**, when the framework loads config (e.g. in `ConfigManager`), it needs the shared secret to decrypt any `ENC(...)` values. The secret is resolved in this order:

1. **JVM system property** `config.secret`  
   - Example: `-Dconfig.secret=your-secret-passphrase`
2. **Environment variable** `CONFIG_SECRET`  
   - Example: `export CONFIG_SECRET=your-secret-passphrase`
3. **Demo fallback**  
   - A default value is used so the project can run out-of-the-box.  
   - A warning is printed to stderr. **You must override** this in real or CI environments (e.g. Jenkins, Docker) via system property or environment variable.

**Important:** Use a strong, unique secret in test/uat/production and never commit it. Prefer environment variables or a secrets manager in CI.

### 4. When decryption happens

- When `ConfigManager` loads the active config file (e.g. `config-test.properties`), it calls the internal **EncryptedPropertySupport**, which:
  - Scans all property values.
  - For any value that starts with `ENC(` and ends with `)`, it Base64-decodes the payload, splits IV and ciphertext, and decrypts using the resolved secret.
  - Replaces the encrypted value in the `Properties` object with the decrypted string.
- After that, every access via `ConfigManager.get(...)` or typed getters (e.g. `getApiBaseUrl()`) returns **plain text**. No extra steps are required in steps, page objects, or API clients.

---

## How to encrypt a value (for use in config files)

To put an encrypted value into `config-test.properties` or `config-uat.properties`, you need to **encrypt** the plain text using the **same algorithm and secret** that the framework uses for decryption.

### Option A: Small Java utility in the same package

The framework’s encryption helper lives in `com.example.framework.config.EncryptedPropertySupport` and is used internally. You can add a small **utility class** in the same package that:

1. Reads the shared secret from `config.secret` or `CONFIG_SECRET` (same as at runtime).
2. Calls the internal `encrypt(String)` method (or a small wrapper that uses the same logic).
3. Prints the result so you can paste it into your properties file.

Example usage idea:

```text
CONFIG_SECRET=my-secret mvn exec:java -Dexec.mainClass="com.example.framework.config.EncryptPropertyUtil" -Dexec.args="my-api-key"
```

The output would be something like `ENC(abc123...)`, which you then set as `testrail.apiKey=ENC(abc123...)` in the config file.

### Option B: External script/tool

You can use any tool that:

- Derives a 256-bit key with **SHA-256(your_secret)**.
- Encrypts with **AES-256-GCM**, 12-byte random IV, 128-bit tag.
- Outputs **Base64(IV || ciphertext)** and wraps it as **ENC(...)**.

As long as the format and algorithm match, the framework will decrypt it at runtime.

---

## End-to-end flow (summary)

| Step | What happens |
|------|-------------------------------|
| 1. **Encrypt (one-time)** | You encrypt a secret (e.g. TestRail API key) with the shared secret and put `ENC(...)` in `config-<env>.properties`. |
| 2. **Provide secret at runtime** | In CI/local, you set `CONFIG_SECRET` or `-Dconfig.secret=...` so the framework can decrypt. |
| 3. **Load config** | `ConfigManager` loads the right `config-<env>.properties` and runs `EncryptedPropertySupport.decryptAll(properties)`. |
| 4. **Use config** | Steps, page objects, and API clients call `ConfigManager.get("testrail.apiKey")` (or typed getters) and receive the **decrypted** string. |

---

## Security recommendations

- **Do not commit** the real shared secret (`config.secret` / `CONFIG_SECRET`) or plain-text secrets to version control.
- **Override** the demo secret in every non-demo environment (Jenkins, Docker, local runs that use real credentials).
- Prefer **environment variables** or a **secrets manager** in CI (e.g. Jenkins credentials) and pass the secret into the JVM via `-Dconfig.secret=...` or `CONFIG_SECRET`.
- Keep **encrypted** values in config files if you need them in SCM; ensure only authorized environments have the decryption secret.
- Optionally, keep **no secrets in config files** at all and pass them only via system properties or environment variables (e.g. `testrail.apiKey` read from `TESTRAIL_API_KEY`); encryption is then an extra option for values you do store in files.

---

## Related code

- **ConfigManager** (`com.example.framework.config.ConfigManager`) – loads env-specific properties and triggers decryption.
- **EncryptedPropertySupport** (`com.example.framework.config.EncryptedPropertySupport`) – performs decrypt-all and optional encrypt; used by ConfigManager and by any small encryption utility you add in the same package.

For a list of config keys (including optional TestRail and other sensitive keys), see the main [README](../README.md) and the comments in `config-test.properties` / `config-uat.properties`.
