package de.subnix.ygrep;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JsonMatcher
{
    private final Scope rootScope;

    public JsonMatcher()
    {
        rootScope = Scope.newEmptyScope();
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);
    }

    public JsonQuery compile(String filter) throws JsonQueryException
    {
        return JsonQuery.compile(filter, Versions.JQ_1_6);
    }

    public List<JsonNode> apply(JsonQuery query, JsonNode input) throws JsonQueryException
    {
        List<JsonNode> results = new ArrayList<>();
        query.apply(rootScope, input, results::add);
        return results.stream()
                .filter(r -> !r.isNull() && !(r.isBoolean() && !r.asBoolean()))
                .toList();
    }
}
