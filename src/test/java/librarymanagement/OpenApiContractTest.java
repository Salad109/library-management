package librarymanagement;

import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest extends IntegrationTest {

    private static final Path SPEC = Path.of("frontend/openapi.json");

    @Autowired
    private MockMvcTester mockMvcTester;

    // Compares parsed JSON, so reformatting the committed file doesn't fail the test
    @Test
    void committedSpecMatchesTheApi() throws IOException {
        MvcTestResult result = mockMvcTester.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();

        String actualText = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actual = mapper.readTree(actualText);
        JsonNode committed = null;
        if (Files.exists(SPEC)) {
            committed = mapper.readTree(Files.readString(SPEC));
        }

        if (!actual.equals(committed)) {
            Files.createDirectories(SPEC.getParent());
            Files.writeString(SPEC, actualText);
        }
        assertThat(committed).as("API changed: %s is outdated", SPEC).isEqualTo(actual);
    }
}
