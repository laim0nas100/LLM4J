package com.github.laim0nas100.lmstudio4j.model.v1;

import com.github.laim0nas100.uncheckedutils.SafeOpt;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Lemmin
 */
public interface LLMToolEvent extends LLMEvent {

    public record ToolCallContent(String type, String text) {}

    public record ToolCallStart(String type) implements LLMToolEvent {}

    public record ToolCallName(String type, String tool_name, ProviderInfo provider_info) implements LLMToolEvent {}

    public record ToolCallArguments(String type, String tool, Map<String, Object> arguments, ProviderInfo provider_info) implements LLMToolEvent {}

    public record ToolCallSuccess(String type, String tool, Map<String, Object> arguments, String output, ProviderInfo provider_info) implements LLMToolEvent {

        public SafeOpt<List<ToolCallContent>> tryParse(McpJsonMapper mapper) {
            Objects.requireNonNull(mapper);
            return SafeOpt.ofNullable(output()).map(con -> mapper.readValue(con, new TypeRef<List<ToolCallContent>>() {
            }));
        }
    }

}
