package de.subnix.shared;

import io.quarkus.runtime.annotations.RegisterForReflection;

// Classes added in jackson-jq 1.6.x that are referenced only via ServiceLoader and
// therefore not reachable by GraalVM static analysis without explicit registration.
@RegisterForReflection(classNames = {
        "net.thisptr.jackson.jq.internal.functions.FromDateIso8601Function",
        "net.thisptr.jackson.jq.internal.functions.ToDateIso8601Function",
        "net.thisptr.jackson.jq.internal.functions.MathFunction$CeilFunction"
})
class JacksonJqReflectionConfig
{
}
