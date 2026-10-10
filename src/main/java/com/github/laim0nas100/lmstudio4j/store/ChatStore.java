package com.github.laim0nas100.lmstudio4j.store;

import com.github.laim0nas100.dbstore.JdbiMixin;
import org.jdbi.v3.core.Jdbi;

/**
 *
 * @author Lemmin
 */
public class ChatStore implements JdbiMixin {
    
    protected SqlService service;

    public ChatStore(SqlService service) {
        this.service = service;
    }
    
    

    @Override
    public Jdbi getJdbi() {
        return service.jdbi;
    }

    
    
}
