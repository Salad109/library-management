package librarymanagement.utils;

import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class ControllerTestUtils {
    private ControllerTestUtils() {
    }

    public static int extractIdFromResponse(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asInt();
    }

    public static int extractIdFromResponseArray(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get(0).get("id").asInt();
    }
}
