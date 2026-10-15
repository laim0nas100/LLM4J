package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.github.laim0nas100.llm4j.GlobalCTX;
import com.github.laim0nas100.llm4j.lmstudio.v1.ChatSession;
import com.github.laim0nas100.llm4j.store.ChatStore;
import com.github.laim0nas100.llm4j.store.Conversation;
import com.github.laim0nas100.llm4j.store.Entity;
import com.github.laim0nas100.llm4j.store.TableAccess;
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

    protected TableCreation creation;
    protected Map<Class, TableAccess> tables = new HashMap<>();
    
    protected Map<String, ChatSession> templates = new ConcurrentHashMap<>();
    

    public LMStudioChatStore(TableCreation tableCreation) {
        this.creation = Objects.requireNonNull(tableCreation);

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
        tables.put(type, new TableAccess(getJdbi(), type, name, counters().getFor(name)));

    }

    @Override
    public <T extends Entity> TableAccess<T> tableFor(Class<T> type) {
        return tables.get(type);
    }
    
    public SafeOpt<ChatSession> getConfig(String name){
        ChatSession found = templates.getOrDefault(name, null);
        if(found != null){
            return SafeOpt.of(found);
        }
        return safeHandle(handle ->{
            Conversation.ChatTemplate template = searchChatTemplate(name).throwAnyGet().get(0);
            return jsonMapper().readValue(template.config_json(), ChatSession.class);
            
        }, name).peek(config ->{
            templates.putIfAbsent(name, config);
        });
    }

}
