package de.subnix.jgrep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.thisptr.jackson.jq.JsonQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JsonMatcherTest
{
    @Inject
    JsonMatcher matcher;

    @Inject
    ObjectMapper mapper;

    private JsonNode json(String raw) throws Exception
    {
        return mapper.readTree(raw);
    }

    @Test
    void fieldAccess() throws Exception
    {
        JsonQuery q = matcher.compile(".name");
        List<JsonNode> result = matcher.apply(q, json("{\"name\": \"Alice\"}"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).asText()).isEqualTo("Alice");
    }

    @Test
    void selectMatchingDocument() throws Exception
    {
        JsonQuery q = matcher.compile("select(.age > 18)");
        assertThat(matcher.apply(q, json("{\"age\": 25}"))).hasSize(1);
        assertThat(matcher.apply(q, json("{\"age\": 15}"))).isEmpty();
    }

    @Test
    void missingFieldIsNull() throws Exception
    {
        JsonQuery q = matcher.compile(".nonexistent");
        assertThat(matcher.apply(q, json("{\"name\": \"Alice\"}"))).isEmpty();
    }

    @Test
    void arrayIteration() throws Exception
    {
        JsonQuery q = matcher.compile(".items[]");
        List<JsonNode> result = matcher.apply(q, json("{\"items\": [1, 2, 3]}"));
        assertThat(result).hasSize(3);
    }

    @Test
    void nestedAccess() throws Exception
    {
        JsonQuery q = matcher.compile(".user.email");
        List<JsonNode> result = matcher.apply(q, json("{\"user\": {\"email\": \"a@b.com\"}}"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).asText()).isEqualTo("a@b.com");
    }

    @Test
    void identityFilter() throws Exception
    {
        JsonQuery q = matcher.compile(".");
        JsonNode input = json("{\"x\": 1}");
        List<JsonNode> result = matcher.apply(q, input);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(input);
    }
}
