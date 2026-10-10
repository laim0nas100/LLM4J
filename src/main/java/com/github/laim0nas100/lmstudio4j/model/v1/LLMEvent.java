package com.github.laim0nas100.lmstudio4j.model.v1;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.laim0nas100.lmstudio4j.model.v1.LLMEvent.*;
import com.github.laim0nas100.lmstudio4j.model.v1.LLMToolEvent.*;
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
        defaultImpl = OtherLLMEvent.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ChatStart.class, name = "chat.start"),
    @JsonSubTypes.Type(value = ChatEnd.class, name = "chat.end"),
    @JsonSubTypes.Type(value = PromptProcessingStart.class, name = "prompt_processing.start"),
    @JsonSubTypes.Type(value = PromptProcessingProgress.class, name = "prompt_processing.progress"),
    @JsonSubTypes.Type(value = PromptProcessingEnd.class, name = "prompt_processing.end"),
    @JsonSubTypes.Type(value = MessageStart.class, name = "message.start"),
    @JsonSubTypes.Type(value = MessageDelta.class, name = "message.delta"),
    @JsonSubTypes.Type(value = MessageEnd.class, name = "message.end"),
    @JsonSubTypes.Type(value = ReasoningStart.class, name = "reasoning.start"),
    @JsonSubTypes.Type(value = ReasoningDelta.class, name = "reasoning.delta"),
    @JsonSubTypes.Type(value = ReasoningEnd.class, name = "reasoning.end"),
    @JsonSubTypes.Type(value = ToolCallStart.class, name = "tool_call.start"),
    @JsonSubTypes.Type(value = ToolCallName.class, name = "tool_call.name"),
    @JsonSubTypes.Type(value = ToolCallArguments.class, name = "tool_call.arguments"),
    @JsonSubTypes.Type(value = ToolCallSuccess.class, name = "tool_call.success"),
    @JsonSubTypes.Type(value = LLMError.class, name = "error")
})
public interface LLMEvent {

    public static final Dummy DUMMY = new Dummy();

    public String type();

    public default String printable() {
        return "";
    }

    public record Dummy() implements LLMEvent {

        @Override
        public String type() {
            return "dummy";
        }
    }

    public record APIError(Exception exception) implements LLMEvent {

        @Override
        public String type() {
            return "api_error";
        }

    }

    public record LLMError(String type, LLMException error) implements LLMEvent {

    }

    public record ChatStart(String type, String model_instance_id) implements LLMEvent {

    }

    public record ChatEnd(String type, ChatResponse result) implements LLMEvent {

    }

    public record PromptProcessingStart(String type) implements LLMEvent {

    }

    public record PromptProcessingEnd(String type) implements LLMEvent {

    }

    public record PromptProcessingProgress(String type, double progress) implements LLMEvent {

    }

    public record ReasoningStart(String type) implements LLMEvent {

    }

    public record ReasoningEnd(String type) implements LLMEvent {

    }

    public record ReasoningDelta(String type, String content) implements LLMEvent {

    }

    public record MessageStart(String type) implements LLMEvent {

    }

    public record MessageEnd(String type) implements LLMEvent {

    }

    public record MessageDelta(String type, String content) implements LLMEvent {

        @Override
        public String printable() {
            return content;
        }

    }

    public static class OtherLLMEvent implements LLMEvent {

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
