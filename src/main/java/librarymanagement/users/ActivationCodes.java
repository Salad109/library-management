package librarymanagement.users;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Customer activation codes: 8 Crockford base32 characters shown as XXXX-XXXX.
 */
final class ActivationCodes {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"; // no I, L, O, U
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ActivationCodes() {
    }

    static String generate() {
        StringBuilder code = new StringBuilder(LENGTH + 1);
        for (int i = 0; i < LENGTH; i++) {
            if (i == LENGTH / 2) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /**
     * SHA-256 hex of the code after normalizing the case, dash, spaces, and O/I/L.
     */
    static String hash(String code) {
        String normalized = code.strip()
                .toUpperCase(Locale.ROOT)
                .replace("-", "")
                .replace(" ", "")
                .replace('O', '0')
                .replace('I', '1')
                .replace('L', '1');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
