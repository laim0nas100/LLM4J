package com.github.laim0nas100.llm4j.lmstudio.v1;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lemmin
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY, // Uses the 'type' field already in the JSON
        property = "type",
        visible = true,
        defaultImpl = LLMException.GenericException.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LLMException.InvalidRequestError.class, name = "invalid_request"),
    @JsonSubTypes.Type(value = LLMException.McpConnectionError.class, name = "mcp_connection_error"),
    @JsonSubTypes.Type(value = LLMException.PluginConnectionError.class, name = "plugin_connection_error"),
    @JsonSubTypes.Type(value = LLMException.NotImplemented.class, name = "not_implemented"),
    @JsonSubTypes.Type(value = LLMException.ModelNotFound.class, name = "model_not_found"),
    @JsonSubTypes.Type(value = LLMException.JobNotFound.class, name = "job_not_found"),
    @JsonSubTypes.Type(value = LLMException.InternalError.class, name = "internal_error"),
    @JsonSubTypes.Type(value = LLMException.UnknownError.class, name = "unknown")
})
public interface LLMException {

    public String type();

    public record InvalidRequestError(String type, String message, String code, String param) implements LLMException {}

    public record McpConnectionError(String type, String message, String code, String param) implements LLMException {}

    public record PluginConnectionError(String type, String message, String code, String param) implements LLMException {}

    public record NotImplemented(String type, String message, String code, String param) implements LLMException {}

    public record ModelNotFound(String type, String message, String code, String param) implements LLMException {}

    public record JobNotFound(String type, String message, String code, String param) implements LLMException {}

    public record InternalError(String type, String message, String code, String param) implements LLMException {}

    public record UnknownError(String type, String message, String code, String param) implements LLMException {}

    public static class GenericException implements LLMException {

        private String type;

        private Map<String, Object> fields = new HashMap<String, Object>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @JsonAnySetter
        public void setField(String name, Object value) {
            fields.put(name, value);
        }

        public Map<String, Object> getFields() {
            return fields;
        }

        @Override
        public String type() {
            return type;
        }
    }
}
