package com.github.laim0nas100.lmstudio4j.store;

import com.github.laim0nas100.dbstore.JdbiMixin;
import com.github.laim0nas100.dbstore.idgen.IdGenerator;
import com.github.laim0nas100.lmstudio4j.store.QuickFields.RefInfo;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.util.List;
import java.util.Objects;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

/**
 *
 * @author Lemmin
 */
public class TableAccess<T> implements JdbiMixin {

    protected final RefInfo<T> refInfo;
    protected final Jdbi jdbi;
    protected final Class<T> type;
    protected final QuickFields.RefInfoField idField;
    protected final String tableName;
    protected final IdGenerator idGenerator;

    public TableAccess(Jdbi jdbi, Class<T> type, String tableName, IdGenerator generator) {
        this.jdbi = Objects.requireNonNull(jdbi);
        this.type = Objects.requireNonNull(type);
        this.idGenerator = generator;
        this.refInfo = QuickFields.getOf(type);
        this.tableName = Objects.requireNonNull(tableName);
        List<QuickFields.RefInfoField> idFields = refInfo.getFields().stream().filter(f -> f.getName().equalsIgnoreCase("ID")).toList();
        if (idFields.size() != 1) {
            throw new IllegalArgumentException("Expected to find exactly 1 field with a name ID");
        }
        idField = idFields.get(0);

    }

    @Override
    public Jdbi getJdbi() {
        return jdbi;
    }

    public Object nextId() {
        return idGenerator.getAndIncrement().get();
    }

    public SafeOpt<List<T>> getAll() {
        return safeHandle(handle -> {
            return handle.createQuery("SELECT " + refInfo.getFieldNamesFormatted() + " FROM " + tableName)
                    .mapToMap()
                    .map(res -> {
                        return refInfo.initObject(res).throwAnyGet();
                    }).list();
        });
    }

    public SafeOpt<T> get(Object id) {
        return safeHandle(handle -> {
            return handle.createQuery("SELECT " + refInfo.getFieldNamesFormatted() + " FROM " + tableName + " WHERE " + idField.getName() + " = :param")
                    .bind("param", id)
                    .mapToMap()
                    .map(res -> {
                        return refInfo.initObject(res).throwAnyGet();
                    }).one();
        }, id);
    }

    public SafeOpt<Integer> delete(Object id) {
        return safeHandle(handle -> {
            return handle.createUpdate("DELETE FROM " + tableName + " WHERE " + idField.getName() + " = :param")
                    .bind("param", id)
                    .execute();
        }, id);
    }

    public SafeOpt<Integer> upsert(T object) {
        return safeHandle(handle -> {
            StringBuilder sb = new StringBuilder();
            String pre = ":";
            String post = "";
            sb.append("INSERT INTO ").append(tableName).append(" (").append(refInfo.getFieldNamesFormatted()).append(") ")
                    .append("VALUES (").append(refInfo.getFieldNamesFormatted(pre, post)).append(") ")
                    .append("ON CONFLICT(").append(idField.getName()).append(") ")
                    .append("DO UPDATE SET ");

            boolean first = true;
            for (String name : refInfo.getFieldNames()) {
                if (name.equals(idField.getName())) {
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(name).append(" = ").append("excluded.").append(name);

            }

            Update update = handle.createUpdate(sb.toString());

            for (QuickFields.RefInfoField field : refInfo.getFields()) {
                update.bind(field.getName(), field.getFieldValue(object).throwAnyGet());
            }

            return update.execute();
        }, object);
    }

}
