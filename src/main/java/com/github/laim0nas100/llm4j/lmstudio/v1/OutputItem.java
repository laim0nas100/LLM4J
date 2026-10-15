package com.github.laim0nas100.llm4j.lmstudio.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.laim0nas100.commonslb.reflect.Refl;
import com.github.laim0nas100.llm4j.lmstudio.v1.OutputItem.InvalidToolCallOutput;
import com.github.laim0nas100.llm4j.lmstudio.v1.OutputItem.MessageOutput;
import com.github.laim0nas100.llm4j.lmstudio.v1.OutputItem.ReasoningOutput;
import com.github.laim0nas100.llm4j.lmstudio.v1.OutputItem.ToolCallOutput;
import java.util.Map;

/**
 *
 * @author Lemmin
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = MessageOutput.class,
            name = "message"
    ),
    @JsonSubTypes.Type(
            value = ToolCallOutput.class,
            name = "tool_call"
    ),
    @JsonSubTypes.Type(
            value = ReasoningOutput.class,
            name = "reasoning"
    ),
    @JsonSubTypes.Type(
            value = InvalidToolCallOutput.class,
            name = "invalid_tool_call"
    )
})
public interface OutputItem {

    public static class MessageOutput extends Refl.SelfIDBean implements OutputItem {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class ToolCallOutput extends Refl.SelfIDBean implements OutputItem {

        private String tool;
        private Map<String, Object> arguments;
        private String output;
        @JsonProperty("provider_info")
        private ProviderInfo providerInfo;

        public String getTool() {
            return tool;
        }

        public void setTool(String tool) {
            this.tool = tool;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public ProviderInfo getProviderInfo() {
            return providerInfo;
        }

        public void setProviderInfo(ProviderInfo providerInfo) {
            this.providerInfo = providerInfo;
        }
    }

    public static class ReasoningOutput extends Refl.SelfIDBean implements OutputItem {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class InvalidToolCallOutput implements OutputItem {

        public static enum InvalidToolCallType {
            INVALID_NAME("invalid_name"), INVALID_ARGUMENTS("invalid_arguments");
            private final String value;

            InvalidToolCallType(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        public static class InvalidToolCallMetadata {

            private InvalidToolCallType type;
            @JsonProperty("tool_name")
            private String toolName;
            private Map<String, Object> arguments;
            @JsonProperty("provider_info")
            private ProviderInfo providerInfo;

            public InvalidToolCallType getType() {
                return type;
            }

            public void setType(InvalidToolCallType type) {
                this.type = type;
            }

            public String getToolName() {
                return toolName;
            }

            public void setToolName(String toolName) {
                this.toolName = toolName;
            }

            public Map<String, Object> getArguments() {
                return arguments;
            }

            public void setArguments(Map<String, Object> arguments) {
                this.arguments = arguments;
            }

            public ProviderInfo getProviderInfo() {
                return providerInfo;
            }

            public void setProviderInfo(ProviderInfo providerInfo) {
                this.providerInfo = providerInfo;
            }
        }

        private String reason;
        private InvalidToolCallMetadata metadata;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public InvalidToolCallMetadata getMetadata() {
            return metadata;
        }

        public void setMetadata(InvalidToolCallMetadata metadata) {
            this.metadata = metadata;
        }
    }
}
