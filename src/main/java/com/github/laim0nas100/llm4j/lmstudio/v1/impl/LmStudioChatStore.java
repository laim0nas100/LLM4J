package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.github.laim0nas100.llm4j.store.ChatStore;
import com.github.laim0nas100.llm4j.store.Entity;
import com.github.laim0nas100.llm4j.store.TableAccess;
import org.jdbi.v3.core.Jdbi;

/**
 *
 * @author Lemmin
 */
public class LmStudioChatStore implements ChatStore {

    @Override
    public <T extends Entity> TableAccess<T> tableFor(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Jdbi getJdbi() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
