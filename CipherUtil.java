import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * CipherUtil - handles the encryption/decryption logic using Triple DES (DESede) in CBC mode,
 * and HMAC-SHA256 for integrity. Keys are derived from a user password using SHA-256.
 * Implements strict memory erasure (Goal 4) by using char[] and wiping buffers.
 */
public class CipherUtil {
    
    private static final String ALGORITHM = "DESede/CBC/PKCS5Padding";
    private static final String DES_ALGORITHM = "DESede";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Derives a 24-byte Triple DES key and a 32-byte HMAC key from a password character array.
     * Manually wipes all temporary buffers once keys are generated.
     */
    private static DerivedKeys deriveKeys(char[] password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        // Convert char[] to byte[] securely without creating a String
        ByteBuffer bBuf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] passwordBytes = new byte[bBuf.remaining()];
        bBuf.get(passwordBytes);
        
        // Perform the hash
        byte[] hash = digest.digest(passwordBytes);
        
        // Immediately wipe the raw password byte array
        Arrays.fill(passwordBytes, (byte) 0);
        Arrays.fill(bBuf.array(), (byte) 0); // ByteBuffer.encode returns a buffer backed by a temp array
        
        // Extract 24 bytes for Triple DES
        byte[] desKeyBytes = new byte[24];
        System.arraycopy(hash, 0, desKeyBytes, 0, 24);
        
        // Prepare keys
        SecretKeySpec hmacKey = new SecretKeySpec(hash, HMAC_ALGORITHM);
        DESedeKeySpec keySpec = new DESedeKeySpec(desKeyBytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES_ALGORITHM);
        SecretKey desKey = keyFactory.generateSecret(keySpec);
        
        // Wipe local key material arrays now that JCE has the SecretKey objects
        Arrays.fill(desKeyBytes, (byte) 0);
        Arrays.fill(hash, (byte) 0);
        
        return new DerivedKeys(desKey, hmacKey);
    }

    public static String encrypt(String text, char[] password) throws Exception {
        DerivedKeys keys = deriveKeys(password);

        byte[] ivBytes = new byte[8];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keys.encryptionKey, ivSpec);
        
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(textBytes);
        
        // Wipe plain text bytes immediately
        Arrays.fill(textBytes, (byte) 0);

        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
        String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);
        
        String payloadToMac = ivBase64 + "::" + encryptedBase64;
        String mac = calculateHmac(payloadToMac, keys.hmacKey);
        
        return payloadToMac + "::" + mac;
    }

    public static String decrypt(String text, char[] password) throws Exception {
        String cleanText = text.trim();
        String[] parts = cleanText.split("::");
        
        if (parts.length != 3) {
            throw new Exception("INVALID FILE FORMAT: The file appears to be corrupted or incompatible.");
        }

        DerivedKeys keys = deriveKeys(password);
        
        String ivBase64 = parts[0];
        String dataToDecrypt = parts[1];
        String expectedMac = parts[2].trim();
        
        String payloadToMac = ivBase64 + "::" + dataToDecrypt;
        if (!expectedMac.equals(calculateHmac(payloadToMac, keys.hmacKey))) {
            throw new Exception("AUTHENTICATION FAILED: Incorrect password or file tampering detected.");
        }
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keys.encryptionKey, new IvParameterSpec(Base64.getDecoder().decode(ivBase64)));
        
        byte[] decodedBytes = Base64.getDecoder().decode(dataToDecrypt);
        byte[] originalBytes = cipher.doFinal(decodedBytes);
        
        String result = new String(originalBytes, StandardCharsets.UTF_8);
        
        // Wipe sensitive binary plaintext
        Arrays.fill(originalBytes, (byte) 0);
        
        return result;
    }

    private static String calculateHmac(String data, SecretKey hmacKey) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder(2 * hmacBytes.length);
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static class DerivedKeys {
        final SecretKey encryptionKey;
        final SecretKey hmacKey;

        DerivedKeys(SecretKey encryptionKey, SecretKey hmacKey) {
            this.encryptionKey = encryptionKey;
            this.hmacKey = hmacKey;
        }
    }
}
