package com.github.laim0nas100.llm4j.store;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public record Conversation(String id, String chat_default_template_id) implements Entity {

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
            ) implements Entity {}

    public record ChatBranch(
            String id,
            String conversation_id,
            String title,
            String head_message_id,
            String forked_from_message_id,
            Instant created_at,
            Instant updated_at
            ) implements Entity {

        public ChatBranch updateHeadMessageId(String head_message_id) {
            return new ChatBranch(id,
                    conversation_id,
                    title,
                    head_message_id,
                    forked_from_message_id,
                    created_at,
                    Instant.now()
            );
        }

        public ChatBranch updateTitle(String title) {
            return new ChatBranch(id,
                    conversation_id,
                    title,
                    head_message_id,
                    forked_from_message_id,
                    created_at,
                    Instant.now()
            );
        }

    }

    public record ChatMessage(
            String id,
            String conversation_id,
            String branch_id,
            String parent_message_id,
            String chat_template_id,
            MessageRole role,
            MessageStatus status,
            String source,
            String source_json,
            List<String> attachments,
            String content_json,
            String provider,
            String provider_json,
            Instant created_at,
            Instant updated_at
            ) implements Entity {}

}
