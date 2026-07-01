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
class PerformanceTest
{
    @Inject
    JsonMatcher matcher;

    @Inject
    ObjectMapper mapper;

    @Test
    void filterTenThousandDocuments() throws Exception
    {
        JsonQuery query = matcher.compile("select(.active == true)");
        int matchCount = 0;
        int total = 10_000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
        {
            String json = "{\"id\":" + i + ",\"active\":" + (i % 2 == 0) + ",\"value\":\"item-" + i + "\"}";
            JsonNode node = mapper.readTree(json);
            List<JsonNode> results = matcher.apply(query, node);
            matchCount += results.size();
        }
        long elapsed = System.currentTimeMillis() - start;

        assertThat(matchCount).isEqualTo(total / 2);
        assertThat(elapsed).isLessThan(5_000);
        System.out.println("Processed " + total + " documents in " + elapsed + "ms (" +
                (total * 1000L / elapsed) + " docs/sec)");
    }
}
