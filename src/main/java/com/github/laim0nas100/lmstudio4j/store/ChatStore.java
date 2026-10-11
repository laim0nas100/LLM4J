package com.github.laim0nas100.lmstudio4j.store;

import com.github.laim0nas100.commonslb.Nulls;
import com.github.laim0nas100.dbstore.JdbiMixin;
import com.github.laim0nas100.lmstudio4j.Global;
import com.github.laim0nas100.lmstudio4j.model.v1.ChatRequest;
import com.github.laim0nas100.lmstudio4j.model.v1.ChatSession;
import com.github.laim0nas100.lmstudio4j.store.Conversation.*;
import com.github.laim0nas100.lmstudio4j.store.Helpers.MessageIds;
import com.github.laim0nas100.lmstudio4j.store.Helpers.MessageSource;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public interface ChatStore extends JdbiMixin {

//    public static class Context{
//        public final Instant now = Instant.now();
//        public final List<Entity> createdEntities = new ArrayList<>();
//        public <T extends Entity> T save(T entity){
//            this.createdEntities.add(entity);
//            return entity;
//        }
//    }
//    
//    public static final ThreadLocal<Context> context = ThreadLocalValue.withInitial(Context::new);
    public static final String DEFAULT_CHAT_TEMPLATE_NAME = "DEFAULT_TEMPLATE";

    public static final String DEFAULT_CHAT_BRANCH_TITLE = "New Chat";

    public <T extends Entity> TableAccess<T> tableFor(Class<T> type);

    public static Instant now() {
        return Instant.now();
    }

    public default SafeOpt<List<ChatTemplate>> searchChatTemplate(String templateName) {
        return safeHandle(handle -> {
            return tableFor(ChatTemplate.class)
                    .search(str -> str + " WHERE name = :name", q -> q.bind("name", templateName))
                    .throwAnyGet();
        }, templateName);
    }

    public default SafeOpt<ChatTemplate> createChatTemplate(String name, ChatSession session) {

        return safeHandle(handle -> {
            TableAccess<ChatTemplate> table = tableFor(ChatTemplate.class);
            String id = String.valueOf(table.nextId());
            String templateName = Nulls.requireNonNullElse(name, "Unnamed" + "-" + id);
            String config_json = Global.jsonMapper.writeValueAsString(session);
            ChatTemplate chatTemplate = new ChatTemplate(id, templateName, config_json, now());

            return table.insert(chatTemplate).throwAnyGet();
        }, session);

    }

    public default SafeOpt<Conversation> createConversation(String templateName) {
        return safeHandle(handle -> {
            TableAccess<Conversation> table = tableFor(Conversation.class);
            List<ChatTemplate> templates = searchChatTemplate(templateName).throwAnyGet();
            if (templates.isEmpty()) {
                throw new IllegalStateException("No templates by the name:" + templateName + " were found");
            }
            ChatTemplate template = templates.get(0);
            String id = String.valueOf(table.nextId());

            return table.insert(new Conversation(id, template.id())).throwAnyGet();

        }, templateName);
    }

    public default SafeOpt<ChatBranch> createBranch(String conversationId) {
        return safeHandle(handle -> {
            TableAccess<ChatBranch> table = tableFor(ChatBranch.class);
            String id = String.valueOf(table.nextId());

            Instant now = Instant.now();
            String headMesasgeId = null;
            String forkedFromId = null;
            ChatBranch chatBranch = new ChatBranch(id, conversationId, DEFAULT_CHAT_BRANCH_TITLE,
                    headMesasgeId, forkedFromId, now, now);
            return table.insert(chatBranch).throwAnyGet();
        }, conversationId);
    }

    public default SafeOpt<ChatMessage> createUserMessage(MessageIds ids, ChatRequest request, MessageSource msgSource) {
        return safeHandle(handle -> {
            TableAccess<ChatMessage> table = tableFor(ChatMessage.class);

            String content_json = Helpers.userInputToJson(request.getInput());

            String provider = "LM Studio";
            List<String> attachments = List.of();

            Instant now = Instant.now();
            String id = String.valueOf(table.nextId());
            ChatMessage chatMessage = new ChatMessage(
                    id,
                    ids.getConversationId(),
                    ids.getBranchId(),
                    ids.getParentMessageId(),
                    ids.getTemplateId(),
                    MessageRole.USER,
                    MessageStatus.COMPLETE,
                    content_json,
                    msgSource.getSourceId(),
                    msgSource.getSource(),
                    msgSource.getSource_json(),
                    attachments,
                    request.getPreviousResponseId(),
                    provider,
                    now,
                    now
            );

            return table.insert(chatMessage).throwAnyGet();
        }, ids, request, msgSource);
    }
}
