package com.github.laim0nas100.llm4j.store;

import com.github.laim0nas100.dbstore.JdbiMixin;
import com.github.laim0nas100.llm4j.GlobalCTX.Globals;
import com.github.laim0nas100.llm4j.store.Conversation.*;
import com.github.laim0nas100.llm4j.store.Helpers.ChatBranchHold;
import com.github.laim0nas100.llm4j.store.Helpers.ChatContentHold;
import com.github.laim0nas100.llm4j.store.Helpers.ChatTemplateHold;
import com.github.laim0nas100.llm4j.store.Helpers.MessageIdHold;
import com.github.laim0nas100.llm4j.store.Helpers.MessageProxyHold;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author Lemmin
 */
public interface ChatStore extends JdbiMixin, Globals {

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

    public default SafeOpt<ChatTemplate> createChatTemplate(ChatTemplateHold hold) {
        return safeHandle(h -> {
            TableAccess<ChatTemplate> table = tableFor(ChatTemplate.class);
            return createChatTemplate(String.valueOf(table.nextId()), hold).throwAnyGet();
        }, hold);
    }

    public default SafeOpt<ChatTemplate> createChatTemplate(String id, ChatTemplateHold hold) {

        return safeHandle(handle -> {
            TableAccess<ChatTemplate> table = tableFor(ChatTemplate.class);

            String templateName = hold.getTemplateName();
            if (templateName == null) {
                String base = "Unnamed-" + id;
                String possibleName = base;
                int i = 1;
                while (true) {

                    if (searchChatTemplate(possibleName).get().isEmpty()) {
                        templateName = possibleName;
                        break; // ok
                    } else {
                        // name is present, try another one
                        possibleName = base + "-" + i;
                        i++;
                    }
                }
            }

            return table.insert(new ChatTemplate(id, templateName, hold.getConfig_json(), now())).throwAnyGet();
        }, hold);

    }

    public default SafeOpt<Conversation> createConversation(String templateName) {
        return safeHandle(h -> {
            TableAccess<Conversation> table = tableFor(Conversation.class);
            return createConversation(String.valueOf(table.nextId()), templateName).throwAnyGet();
        }, templateName);
    }

    public default SafeOpt<Conversation> createConversation(String id, String templateName) {
        return safeHandle(handle -> {
            TableAccess<Conversation> table = tableFor(Conversation.class);
            List<ChatTemplate> templates = searchChatTemplate(templateName).throwAnyGet();
            if (templates.isEmpty()) {
                throw new IllegalStateException("No templates by the name:" + templateName + " were found");
            }
            ChatTemplate template = templates.get(0);
            return table.insert(new Conversation(id, template.id())).throwAnyGet();

        }, templateName);
    }

    public default SafeOpt<ChatBranch> createBranch(ChatBranchHold hold) {
        return safeHandle(h -> {
            TableAccess<ChatBranch> table = tableFor(ChatBranch.class);
            String id = String.valueOf(table.nextId());
            return createBranch(id, hold).throwAnyGet();
        }, hold);
    }

    public default SafeOpt<ChatBranch> createBranch(String id, ChatBranchHold hold) {
        return safeHandle(handle -> {
            TableAccess<ChatBranch> table = tableFor(ChatBranch.class);

            Instant now = Instant.now();
            ChatBranch chatBranch = new ChatBranch(id, hold.getConversationId(), hold.getTitle(),
                    hold.getHeadMessageId(), hold.getForkedFromId(), now, now);
            return table.insert(chatBranch).throwAnyGet();
        }, hold);
    }

    public default SafeOpt<ChatMessage> createMessage(MessageIdHold ids, ChatContentHold content, MessageProxyHold msgProxy) {
        return safeHandle(h -> {
            TableAccess<ChatMessage> table = tableFor(ChatMessage.class);
            String id = String.valueOf(table.nextId());
            return createMessage(id, ids, content, msgProxy).throwAnyGet();
        }, ids, content, msgProxy);
    }

    public default SafeOpt<ChatMessage> createMessage(String id, MessageIdHold ids, ChatContentHold content, MessageProxyHold msgProxy) {
        return safeHandle(handle -> {
            TableAccess<ChatMessage> table = tableFor(ChatMessage.class);

            Instant now = Instant.now();

            ChatMessage chatMessage = new ChatMessage(
                    id,
                    ids.getConversationId(),
                    ids.getBranchId(),
                    ids.getParentMessageId(),
                    ids.getTemplateId(),
                    content.getRole(),
                    content.getStatus(),
                    msgProxy.getProxy(),
                    msgProxy.getProxy_json(),
                    content.getAttachments(),
                    content.getContent_json(),
                    content.getProvider(),
                    content.getProvider_json(),
                    now,
                    now
            );

            ChatMessage inserted = table.insert(chatMessage).throwAnyGet();
            //update chat branch to the new message
            TableAccess<ChatBranch> tableFor = tableFor(ChatBranch.class);
            tableFor.get(ids.getBranchId()).map(branch -> branch.updateHeadMessageId(id)).map(branch -> tableFor.upsert(branch)).throwAnyGet();
            return inserted;
        }, ids, content, msgProxy);
    }
}
