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
    void testHashNormalizesTypedCodes() {
        String hash = ActivationCodes.hash("AB2C-DE3F");

        assertThat(ActivationCodes.hash(" ab2c de3f ")).isEqualTo(hash);
        assertThat(ActivationCodes.hash("AB2CDE3F")).isEqualTo(hash);
        // Lookalike letters read as digits
        assertThat(ActivationCodes.hash("oO1i-lL00")).isEqualTo(ActivationCodes.hash("0011-1100"));
    }
}
