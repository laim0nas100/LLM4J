package com.github.laim0nas100.llm4j.store;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMException;
import com.github.laim0nas100.llm4j.lmstudio.v1.LLMToolEvent;
import com.github.laim0nas100.llm4j.store.MessageStep.*;

/**
 *
 * @author Lemmin
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextStep.class, name = "text"),
    @JsonSubTypes.Type(value = ImageStep.class, name = "image"),
    @JsonSubTypes.Type(value = ReasoningStep.class, name = "reasoning"),
    @JsonSubTypes.Type(value = ToolCallStep.class, name = "tool_call"),
    @JsonSubTypes.Type(value = ErrorStep.class, name = "error")
})
public interface MessageStep {
    
    public record TextStep(String type, String text) implements MessageStep {

        public TextStep(String text) {
            this("text", text);
        }

    }
    
    public record ImageStep(String type, String dataUrl) implements MessageStep {

        public ImageStep(String url) {
            this("image", url);
        }

    }

    public record ReasoningStep(String type, String text) implements MessageStep {

        public ReasoningStep(String text) {
            this("reasoning", text);
        }
    }

    public record ToolCallStep(String type, LLMToolEvent tool_event, LLMException error) implements MessageStep {

        public ToolCallStep(LLMToolEvent tool_event, LLMException error) {
            this("tool_call", tool_event, error);
        }

        public ToolCallStep(LLMToolEvent tool_event) {
            this("tool_call", tool_event, null);
        }

    }

    public record ErrorStep(String type, LLMException error) implements MessageStep {

        public ErrorStep(LLMException error) {
            this("error", error);
        }

    }

}
