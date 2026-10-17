package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.github.laim0nas100.llm4j.GlobalCTX;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatSession;
import com.github.laim0nas100.llm4j.lmstudio.v1.Stats;
import com.github.laim0nas100.llm4j.store.ChatStore;
import com.github.laim0nas100.llm4j.store.Conversation;
import com.github.laim0nas100.llm4j.store.Conversation.ChatBranch;
import com.github.laim0nas100.llm4j.store.Conversation.ChatMessage;
import com.github.laim0nas100.llm4j.store.Conversation.ChatTemplate;
import com.github.laim0nas100.llm4j.store.Entity;
import com.github.laim0nas100.llm4j.store.Helpers;
import com.github.laim0nas100.llm4j.store.TableCreation;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lemmin
 */
public class LMStudioChatStore implements ChatStore, GlobalCTX.SqlDependency {

    public static final String PROVIDER = "LM-Studio";

    public record LMStudioResponseMeta(Stats stats, String responseId) {}

    public record LMStudioResponse(ChatSession session, LLMEventAccumulator accumulator, String conversationId, String branchId, String usermessageId, String assitantMessageId) {}

    protected TableCreation creation;
    protected Map<Class, TableAccessEntity> tables = new HashMap<>();

    protected Map<String, ChatSession> templates = new ConcurrentHashMap<>();
    protected Map<String, String> branchLastResponseId = new ConcurrentHashMap<>();

    public LMStudioChatStore(TableCreation tableCreation) throws SQLException {
        this.creation = Objects.requireNonNull(tableCreation);
        initialize();
    }

    public void initialize() throws SQLException {
        safeHandle(handle -> {
            if (!tableExists(handle.getConnection(), creation.conversationsName)) {
                creation.executeAll(getJdbi());
            }
            return 0;
        });
        putTableAccess(Conversation.class, creation.conversationsName);
        putTableAccess(Conversation.ChatTemplate.class, creation.chatTemplatesName);
        putTableAccess(Conversation.ChatBranch.class, creation.chatBranchesName);
        putTableAccess(Conversation.ChatMessage.class, creation.chatMessagesName);

    }

    protected void putTableAccess(Class type, String name) {
        tables.put(type, new TableAccessEntity(this, type, name));
    }

    @Override
    public <T extends Entity> TableAccessEntity<T> tableFor(Class<T> type) {
        return tables.get(type);
    }

    public SafeOpt<ChatTemplate> initializeTemplate(String name, ChatSession template) {
        return safeHandle(hanle -> {
            ChatSession justConfig = template.justConfig();
            String config = jsonMapper().writeValueAsString(justConfig);
            Helpers.ChatTemplateHold hold = new Helpers.ChatTemplateHold();
            hold.setConfig_json(config);
            hold.setTemplateName(name);
            return createChatTemplate(hold).throwAnyGet();
        }, template);
    }

    public SafeOpt<ChatSession> getConfigByName(String name) {
        ChatSession found = templates.getOrDefault(name, null);
        if (found != null) {
            return SafeOpt.of(found);
        }
        return safeHandle(handle -> {
            Conversation.ChatTemplate template = searchChatTemplate(name).throwAnyGet().get(0);
            return jsonMapper().readValue(template.config_json(), ChatSession.class);

        }, name).peek(config -> {
            templates.putIfAbsent(name, config);
        });
    }

    public SafeOpt<ChatSession> getConfigById(String id) {
        ChatSession found = templates.getOrDefault(id, null);
        if (found != null) {
            return SafeOpt.of(found);
        }
        return getTemplate(id).map(template -> {
            return jsonMapper().readValue(template.config_json(), ChatSession.class);
        }).peek(config -> {
            templates.putIfAbsent(id, config);
        });
    }

    public SafeOpt<String> getLastResponseIdForBranch(String branchId) {
        String found = branchLastResponseId.getOrDefault(branchId, null);
        if (found != null) {
            return SafeOpt.of(found);
        }
        return safeHandle(handle -> {
            String branchTableName = tableFor(ChatBranch.class).getTableName();
            String messageTableName = tableFor(ChatMessage.class).getTableName();
            String provider_json = handle.createQuery("SELECT m.provider_json FROM %s AS m, %s AS b WHERE m.branch_id = b.id AND m.id = b.head_message_id AND b.id = :branch_id".formatted(messageTableName, branchTableName))
                    .bind("branch_id", branchId)
                    .mapTo(String.class).one();
            return jsonMapper().readValue(provider_json, LMStudioResponseMeta.class).responseId;
        }, branchId).peek(respId -> {
            branchLastResponseId.putIfAbsent(branchId, respId);
        });
    }

}
