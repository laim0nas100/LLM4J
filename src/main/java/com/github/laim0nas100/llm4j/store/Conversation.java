package com.github.laim0nas100.llm4j.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.laim0nas100.llm4j.store.Helpers.ChatContentHold;
import java.time.Instant;

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
            String proxy,
            String proxy_json,
            String attachments_json,
            String content_json,
            String provider,
            String provider_json,
            Instant created_at,
            Instant updated_at
            ) implements Entity {

        public ChatMessage updatedContent(ChatContentHold hold) throws JsonProcessingException {
            return new ChatMessage(id, conversation_id, branch_id, parent_message_id, chat_template_id, hold.getRole(), hold.getStatus(), proxy, proxy_json, hold.getAttachmentsJson(), hold.getContent_json(), hold.getProvider(), hold.getProvider_json(), created_at, Instant.now());
        }
        
        public ChatMessage updatedProviderJson(String provider_json) throws JsonProcessingException {
            return new ChatMessage(id, conversation_id, branch_id, parent_message_id, chat_template_id, role, status, proxy, proxy_json, attachments_json, content_json, provider, provider_json, created_at, Instant.now());
        }
    }

}
