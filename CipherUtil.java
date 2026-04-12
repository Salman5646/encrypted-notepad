import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CipherUtil - handles the encryption/decryption logic using DES (CBC mode + IVs), and HMAC.
 */
public class CipherUtil {
    
    private static final String DEFAULT_KEY = "S3cr3tK3"; // 8 bytes for DES
    private static final String DEFAULT_HMAC_KEY = "HmacAuthKey99"; // Key for HMAC authenticity
    
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String encrypt(String text) throws Exception {
        DESKeySpec keySpec = new DESKeySpec(DEFAULT_KEY.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);

        // Generate an incredibly secure, unpredictable 8-byte IV
        byte[] ivBytes = new byte[8];
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        // We upgrade from ECB to CBC mode now to utilize the IV
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        
        byte[] encryptedBytes = cipher.doFinal(text.getBytes("UTF-8"));
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
        String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);
        
        // Calculate HMAC on both the IV and the Ciphertext so neither can be tampered with!
        String payloadToMac = ivBase64 + "::" + encryptedBase64;
        String mac = getHmacSHA256(payloadToMac);
        
        return payloadToMac + "::" + mac;
    }

    public static String decrypt(String text) throws Exception {
        // Strip any hidden newlines or spaces added by text editors/OS that break exact string matching
        String cleanText = text.trim();
        String[] parts = cleanText.split("::");
        
        if (parts.length == 3) {
            // NEW MODE: IV + CBC Encryption + HMAC
            String ivBase64 = parts[0];
            String dataToDecrypt = parts[1];
            String expectedMac = parts[2].trim(); // trim the seal itself just to be completely safe
            
            String payloadToMac = ivBase64 + "::" + dataToDecrypt;
            if (!expectedMac.equals(getHmacSHA256(payloadToMac))) {
                throw new Exception("HMAC AUTHENTICITY FAILED: Forgery detected!");
            }
            
            DESKeySpec keySpec = new DESKeySpec(DEFAULT_KEY.getBytes("UTF-8"));
            SecretKey key = SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
            
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Base64.getDecoder().decode(ivBase64)));
            
            byte[] decodedBytes = Base64.getDecoder().decode(dataToDecrypt);
            return new String(cipher.doFinal(decodedBytes), "UTF-8");
            
        } else {
            // LEGACY MODE FALLBACK (ECB without IV) for older saved files
            String dataToDecrypt = parts[0];
            String expectedMac = parts.length > 1 ? parts[1].trim() : null;

            boolean hmacMatched = false;
            if (expectedMac != null) {
                if (expectedMac.equals(getHmacSHA256(dataToDecrypt))) {
                    hmacMatched = true;
                }
            }

            DESKeySpec keySpec = new DESKeySpec(DEFAULT_KEY.getBytes("UTF-8"));
            SecretKey key = SecretKeyFactory.getInstance("DES").generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            byte[] decodedBytes = Base64.getDecoder().decode(dataToDecrypt);
            String decryptedText = new String(cipher.doFinal(decodedBytes), "UTF-8");

            // Check absolute legacy (Plain SHA-256)
            if (expectedMac != null && !hmacMatched) {
                if (!expectedMac.equals(getSHA256Hash(decryptedText))) {
                    throw new Exception("AUTHENTICITY FAILED: Forgery detected!");
                }
            }
            return decryptedText;
        }
    }

    /**
     * Generates an HMAC-SHA256 cryptographic authentication code for the given text.
     */
    public static String getHmacSHA256(String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(DEFAULT_HMAC_KEY.getBytes("UTF-8"), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(text.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder(2 * hmacBytes.length);
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a one-way SHA-256 hash of the given text (retained for backward compatibility/utilities).
     */
    public static String getSHA256Hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(text.getBytes("UTF-8"));
            
            // Convert the binary hash into a readable hexadecimal string
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
