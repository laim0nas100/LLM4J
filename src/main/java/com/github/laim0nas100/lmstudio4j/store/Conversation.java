package com.github.laim0nas100.lmstudio4j.store;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public record Conversation(String id, String default_chat_template_id) {

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM,
        DEVELOPER,
        TOOL
    }

    public enum MessageStatus {
        PENDING,
        STREAMING,
        COMPLETE,
        FAILED,
        CANCELLED
    }

    public record ChatTemplate(
            String id,
            String name,
            String config_json,
            Instant created_at
            ) {}

    public record ChatBranch(
            String id,
            String conversation_id,
            String title,
            String head_message_id,
            String forked_from_message_id,
            Instant created_at,
            Instant updated_at
            ) {}

    public record ChatMessage(
            String id,
            String conversation_id,
            String branch_id,
            String parent_message_id,
            MessageRole role,
            MessageStatus status,
            String content_json,
            String source_json,
            List<String> attachments,
            String provider_response_id,
            String provider,
            String model,
            Instant created_at,
            Instant updated_at
            ) {}

}
