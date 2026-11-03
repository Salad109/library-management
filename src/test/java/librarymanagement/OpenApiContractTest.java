package librarymanagement;

import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest extends IntegrationTest {

    private static final Path SPEC = Path.of("frontend/openapi.json");

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void committedSpecMatchesTheApi() throws IOException {
        MvcTestResult result = mockMvcTester.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();

        String actual = result.getResponse().getContentAsString();
        String committed = Files.exists(SPEC) ? Files.readString(SPEC) : "";
        if (!actual.equals(committed)) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, actual);
        }
        assertThat(committed).as("API changed: %s is outdated", SPEC).isEqualTo(actual);
    }
}
