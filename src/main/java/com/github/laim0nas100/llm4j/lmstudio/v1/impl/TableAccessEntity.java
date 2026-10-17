package com.github.laim0nas100.llm4j.lmstudio.v1.impl;

import com.github.laim0nas100.llm4j.GlobalCTX;
import com.github.laim0nas100.llm4j.store.Entity;
import com.github.laim0nas100.llm4j.store.QuickFields;
import com.github.laim0nas100.llm4j.store.QuickFields.Mapper2Way;
import com.github.laim0nas100.llm4j.store.TableAccess;
import java.time.Instant;

/**
 *
 * @author Lemmin
 */
public class TableAccessEntity<T extends Entity> extends TableAccess<T> {
    
    public static final Mapper2Way<Long, Instant> INSTANT_MAPPER = new QuickFields.Mapper2Way<>(){
        @Override
        public Long toExternal(Instant type) {
            return type.toEpochMilli();
        }

        @Override
        public Instant toInternal(Long type) {
            return Instant.ofEpochMilli(type);
        }
    };
    
    
    public TableAccessEntity(GlobalCTX.SqlDependency dep, Class<T> type, String tableName) {
        super(dep.getJdbi(), type, tableName,dep.counters().getFor(tableName));
        this.refInfo.getFields().forEach(field ->{
            if(field.getFieldType().equals(Instant.class)){
                field.setMapper(INSTANT_MAPPER);
            }
        });
    }
    
}
