package com.github.laim0nas100.llm4j.store;

import java.sql.SQLException;
import org.jdbi.v3.core.Jdbi;

/**
 *
 * @author Lemmin
 */
public class TableCreation {

    public String chatTemplatesName = "ChatTemplates";
    public String conversationsName = "Conversations";
    public String chatBranchesName = "ChatBranches";
    public String chatMessagesName = "ChatMessages";

    public String indexChatBranchesConversationUpdatedName = "idx_chat_branches_conversation_updated";
    public String indexChatMessagesBranchCreatedName = "idx_chat_messages_branch_created";

    public void executeAll(Jdbi jdbi) throws SQLException {
        jdbi.inTransaction(handle -> {
            handle.execute(getChatTemplateSql());
            handle.execute(getConversationsSql());
            handle.execute(getChatBranchesSql());
            handle.execute(getChatMessagesSql());
            handle.execute(getIndexChatBranchesConversationUpdatedSql());
            handle.execute(getIndexChatMessagesBranchCreatedSql());
            return 0;
        });
    }

    public String getChatTemplateSql() {
        return String.format(CREATE_CHAT_TEMPLATES, chatTemplatesName);
    }

    public String getConversationsSql() {
        return String.format(CREATE_CONVERSATIONS, conversationsName, chatTemplatesName);
    }

    public String getChatBranchesSql() {
        return String.format(CREATE_CHAT_BRANCHES, chatBranchesName, conversationsName, chatMessagesName, chatMessagesName);
    }

    public String getChatMessagesSql() {
        return String.format(CREATE_CHAT_MESSAGES, chatMessagesName, chatBranchesName, chatMessagesName,chatTemplatesName);
    }

    public String getIndexChatBranchesConversationUpdatedSql() {
        return String.format(CREATE_CHAT_BRANCHES_INDEX, indexChatBranchesConversationUpdatedName, chatBranchesName);
    }

    public String getIndexChatMessagesBranchCreatedSql() {
        return String.format(CREATE_CHAT_MESSAGES_INDEX, indexChatMessagesBranchCreatedName, chatMessagesName);
    }

    public static final String CREATE_CHAT_TEMPLATES = """
    CREATE TABLE %s (
        id TEXT PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        config_json TEXT NOT NULL
            CHECK (json_valid(config_json)),
        created_at INTEGER NOT NULL
    )
    """;

    public static final String CREATE_CONVERSATIONS = """
    CREATE TABLE %s (
        id TEXT PRIMARY KEY NOT NULL,
        chat_default_template_id TEXT,
        FOREIGN KEY (chat_default_template_id)
            REFERENCES %s(id) ON DELETE SET NULL
    )
    """;

    public static final String CREATE_CHAT_BRANCHES = """
    CREATE TABLE %s (
        id TEXT PRIMARY KEY NOT NULL,
        conversation_id TEXT NOT NULL,
        title TEXT NOT NULL,
        head_message_id TEXT,
        forked_from_message_id TEXT,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,

        UNIQUE (id, conversation_id),

        FOREIGN KEY (conversation_id)
            REFERENCES %s(id) ON DELETE CASCADE,

        FOREIGN KEY (head_message_id, conversation_id)
            REFERENCES %s(id, conversation_id)
            DEFERRABLE INITIALLY DEFERRED,

        FOREIGN KEY (forked_from_message_id, conversation_id)
            REFERENCES %s(id, conversation_id)
            DEFERRABLE INITIALLY DEFERRED
    )
    """;

    public static final String CREATE_CHAT_MESSAGES = """
    CREATE TABLE %s (
        id TEXT PRIMARY KEY NOT NULL,
        conversation_id TEXT NOT NULL,
        branch_id TEXT NOT NULL,
        parent_message_id TEXT,
        chat_template_id TEXT NOT NULL,                                                      
        role TEXT NOT NULL
            CHECK (role IN (
                'USER', 'ASSISTANT', 'SYSTEM', 'DEVELOPER', 'TOOL'
            )),
        status TEXT NOT NULL
            CHECK (status IN (
                'PENDING', 'STREAMING', 'COMPLETE', 'FAILED', 'CANCELLED'
            )),
       
        proxy TEXT,
        proxy_json TEXT NOT NULL
                    CHECK (json_valid(proxy_json)),
        attachments_json TEXT NOT NULL
            CHECK (
                json_valid(attachments_json)
                AND json_type(attachments_json) = 'array'
            ),
        content_json TEXT NOT NULL
                   CHECK (json_valid(content_json)),
        provider TEXT,
        provider_json TEXT NOT NULL
            CHECK (json_valid(provider_json)),
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL,

        UNIQUE (id, conversation_id),

        FOREIGN KEY (branch_id, conversation_id)
            REFERENCES %s(id, conversation_id)
            ON DELETE CASCADE,

        FOREIGN KEY (parent_message_id, conversation_id)
            REFERENCES %s(id, conversation_id)
            DEFERRABLE INITIALLY DEFERRED,
                                                      
        FOREIGN KEY (chat_template_id) 
            REFERENCES %s(id) ON DELETE CASCADE
    )
    """;

    public static final String CREATE_CHAT_BRANCHES_INDEX = """
    CREATE INDEX %s ON %s (conversation_id, updated_at)
    """;

    public static final String CREATE_CHAT_MESSAGES_INDEX = """
    CREATE INDEX %s ON %s (branch_id, created_at)
    """;

}
