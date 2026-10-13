package com.github.laim0nas100.llm4j.lmstudio.v1;

/**
 *
 * @author Lemmin
 */
public class InvalidToolCallOutput implements OutputItem {

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
