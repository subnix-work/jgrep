package de.subnix.jgrep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.subnix.shared.JsonMatcher;
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

    @Test
    void ecsLogTextProjection() throws Exception
    {
        JsonQuery q = matcher.compile("""
                "\\(.["@timestamp"]) [\\(.["log.level"])] \\(.["service.name"])/\\(.kubernetes.pod): \\(.message)"
                """);

        List<JsonNode> result = matcher.apply(q, json("""
                {
                  "@timestamp": "2026-06-28T08:16:12Z",
                  "log.level": "ERROR",
                  "message": "payment declined",
                  "service.name": "checkout",
                  "kubernetes": {"pod": "checkout-7b9"}
                }
                """));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).asText())
                .isEqualTo("2026-06-28T08:16:12Z [ERROR] checkout/checkout-7b9: payment declined");
    }

    @Test
    void ecsLogFilterAndNestedDetailsProjection() throws Exception
    {
        JsonQuery q = matcher.compile("""
                select(.["log.level"] == "ERROR" and .kubernetes.namespace == "shop") |
                "\\(.["@timestamp"]) ERROR \\(.["service.name"]): \\(.message) trace=\\(.custom_tracker.trace_id)"
                """);

        List<JsonNode> result = matcher.apply(q, json("""
                {
                  "@timestamp": "2026-06-28T08:16:12Z",
                  "log.level": "ERROR",
                  "message": "payment declined",
                  "service.name": "checkout",
                  "kubernetes": {"namespace": "shop"},
                  "details": {"order_id": "A-1002", "reason": "card_expired"},
                  "custom_tracker": {"trace_id": "tr-93"}
                }
                """));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).asText())
                .isEqualTo("2026-06-28T08:16:12Z ERROR checkout: payment declined trace=tr-93");
    }

    @Test
    void ecsLogProjectionWithFallbackValues() throws Exception
    {
        JsonQuery q = matcher.compile("""
                "\\(.["@timestamp"]) \\(.kubernetes.pod // "-") \\(.message) order=\\(.details.order_id // "-")"
                """);

        List<JsonNode> result = matcher.apply(q, json("""
                {
                  "@timestamp": "2026-06-28T08:16:12Z",
                  "message": "payment declined",
                  "kubernetes": {},
                  "details": {"reason": "card_expired"}
                }
                """));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).asText())
                .isEqualTo("2026-06-28T08:16:12Z - payment declined order=-");
    }
}
