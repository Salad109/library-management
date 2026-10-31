package librarymanagement.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivationCodesTest {

    @Test
    void testGeneratedCodeFormat() {
        // Crockford base32: no I, L, O, U
        assertThat(ActivationCodes.generate()).matches("[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}");
    }

    @Test
    void testGeneratedCodesDiffer() {
        assertThat(ActivationCodes.generate()).isNotEqualTo(ActivationCodes.generate());
    }

    @Test
    void testHashIgnoresCaseDashAndSpaces() {
        String hash = ActivationCodes.hash("AB2C-DE3F");

        assertThat(ActivationCodes.hash("ab2c-de3f")).isEqualTo(hash);
        assertThat(ActivationCodes.hash("AB2CDE3F")).isEqualTo(hash);
        assertThat(ActivationCodes.hash(" ab2c de3f ")).isEqualTo(hash);
    }

    @Test
    void testHashReadsLookalikeLettersAsDigits() {
        assertThat(ActivationCodes.hash("oO1i-lL00")).isEqualTo(ActivationCodes.hash("0011-1100"));
    }

    @Test
    void testDifferentCodesHashDifferently() {
        assertThat(ActivationCodes.hash("AB2C-DE3F")).isNotEqualTo(ActivationCodes.hash("AB2C-DE3G"));
    }
}
