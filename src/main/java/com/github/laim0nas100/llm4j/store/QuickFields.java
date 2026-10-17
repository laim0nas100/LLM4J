package com.github.laim0nas100.llm4j.store;

import com.github.laim0nas100.commonslb.iteration.streams.MakeStream;
import com.github.laim0nas100.commonslb.iteration.streams.SimpleStream;
import com.github.laim0nas100.commonslb.reflect.unified.IObjectField;
import com.github.laim0nas100.commonslb.reflect.unified.ReflFields;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lemmin
 */
public class QuickFields {

    public static interface Mapper2Way<EXT, INT> {

        public EXT toExternal(INT type);

        public INT toInternal(EXT type);

        public static final Mapper2Way IDENTITY = new Mapper2Way() {
            @Override
            public Object toExternal(Object type) {
                return type;
            }

            @Override
            public Object toInternal(Object type) {
                return type;
            }
        };
    }

    private static ConcurrentHashMap<Class, RefInfo> map = new ConcurrentHashMap<>();

    public static <T> RefInfo<T> getOf(Class<T> type) {
        Objects.requireNonNull(type);
        return map.computeIfAbsent(type, k -> {
            if (k.isRecord()) {
                return new RefInfoRecord(k);
            } else {
                return new RefInfoClass(k);
            }
        });
    }

    public interface RefInfoField<T> {

        public Class getFieldType();

        public Class<T> getObjectType();

        public String getName();

        public <A> SafeOpt<A> getFieldValue(T object);

        public <A> SafeOpt<A> getFieldExternalValue(T object);

        public <S, J> Mapper2Way<S, J> getMapper();
        
        public <S,J> void setMapper(Mapper2Way<S,J> mapper);

    }

    public interface RefInfo<T> {

        public Class<T> getType();

        public List<String> getFieldNames();

        public List<RefInfoField> getFields();

        public SafeOpt<T> initObject(boolean mapping, Map<String, Object> valueMap);

        public default SafeOpt<T> initObject(Map<String, Object> valueMap) {
            return initObject(true, valueMap);
        }

        public default SafeOpt<Map<String, Object>> objectToMap(boolean mapping, T object) {
            return SafeOpt.ofNullable(object).map(ob -> {
                Map<String, Object> valueMap = new LinkedHashMap<>();
                for (RefInfoField field : getFields()) {
                    SafeOpt value = mapping ? field.getFieldExternalValue(object) : field.getFieldValue(object);
                    valueMap.put(field.getName(), value.throwAnyOrNull());
                }
                return valueMap;
            });
        }

        public default SafeOpt<T> copy(T object) {
            return objectToMap(false, object).flatMap(valueMap -> initObject(false, valueMap));
        }

        public default SafeOpt<Map<String, Object>> objectToMap(T object) {
            return objectToMap(true, object);
        }

        public default String getFieldNamesFormatted() {
            return getFieldNamesFormatted("", "");
        }

        public default String getFieldNamesFormatted(String pre, String post) {
            StringBuilder sb = new StringBuilder();
            pre = Objects.requireNonNullElse(pre, "");
            post = Objects.requireNonNullElse(post, "");

            boolean first = true;
            for (String name : getFieldNames()) {
                String insert = pre + name + post;
                if (first) {
                    first = false;
                    sb.append(insert);
                } else {
                    sb.append(", ").append(insert);
                }
            }

            return sb.toString();
        }
    }

    public static class RefInfoFieldRecordComponent<T> implements RefInfoField<T> {

        public final RecordComponent recordComponent;
        public Mapper2Way mapper = Mapper2Way.IDENTITY;

        public RefInfoFieldRecordComponent(RecordComponent recordComponent) {
            this.recordComponent = recordComponent;
        }

        @Override
        public Class getFieldType() {
            return recordComponent.getType();
        }

        @Override
        public Class<T> getObjectType() {
            return (Class<T>) recordComponent.getDeclaringRecord();
        }

        @Override
        public String getName() {
            return recordComponent.getName();
        }

        @Override
        public <A> SafeOpt<A> getFieldValue(T object) {
            return SafeOpt.ofNullable(object).map(ob -> (A) recordComponent.getAccessor().invoke(ob));
        }

        @Override
        public <A> SafeOpt<A> getFieldExternalValue(T object) {
            return getFieldValue(object).map(val -> (A) getMapper().toExternal(val));
        }

        @Override
        public <S, J> Mapper2Way<S, J> getMapper() {
            return mapper;
        }

        @Override
        public <S, J> void setMapper(Mapper2Way<S, J> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
        }

    }

    public static class RefInfoRecord<T> implements RefInfo<T> {

        public final Class type;
        public final SafeOpt<List<RefInfoFieldRecordComponent>> components;
        public final SafeOpt<List<String>> fieldNames;

        public final SafeOpt<Constructor> constructor;

        public RefInfoRecord(Class type) {
            this.type = type;
            components = SafeOpt.ofLazy(type)
                    .map(t -> MakeStream.from(t.getRecordComponents()))
                    .map(stream -> stream.map(rc -> new RefInfoFieldRecordComponent(rc)))
                    .map(stream -> stream.toList());

            fieldNames = components.map(list -> list.stream().map(m -> m.getName())).map(m -> m.toList());
            constructor = components.map(list -> list.stream().map(m -> m.getFieldType()).toArray(s -> new Class[s]))
                    .map(array -> type.getConstructor(array));

        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public List<String> getFieldNames() {
            return fieldNames.get();
        }

        @Override
        public List<RefInfoField> getFields() {
            return (List) components.get();
        }

        @Override
        public SafeOpt<T> initObject(boolean mapping, Map<String, Object> valueMap) {
            return SafeOpt.ofNullable(valueMap).map(rs -> {
                List<RefInfoFieldRecordComponent> fields = components.get();
                Object[] args = new Object[fields.size()];
                for (int i = 0; i < args.length; i++) {
                    RefInfoFieldRecordComponent field = fields.get(i);
                    Object value = valueMap.getOrDefault(field.getName(), null);
                    args[i] = mapping ? field.getMapper().toInternal(value) : value;
                }
                return (T) constructor.get().newInstance(args);
            });
        }

    }

    public static class RefInfoFieldIObjectField<T> implements RefInfoField<T> {

        public final IObjectField field;
        public Mapper2Way mapper = Mapper2Way.IDENTITY;

        public RefInfoFieldIObjectField(IObjectField field) {
            this.field = field;
        }

        @Override
        public Class getFieldType() {
            return field.getType();
        }

        @Override
        public Class getObjectType() {
            return field.getDeclaringClass();
        }

        @Override
        public String getName() {
            return field.getName();
        }

        @Override
        public <A> SafeOpt<A> getFieldValue(T object) {
            return field.safeGet(object);
        }

        public SafeOpt<Void> setFieldValue(Object object, Object value) {
            return field.safeSet(value, value);
        }

        @Override
        public <A> SafeOpt<A> getFieldExternalValue(T object) {
            return getFieldValue(object).map(val -> (A) getMapper().toExternal(val));
        }

        @Override
        public <S, J> Mapper2Way<S, J> getMapper() {
            return mapper;
        }
        
        @Override
        public <S, J> void setMapper(Mapper2Way<S, J> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
        }

    }

    public static class RefInfoClass<T> implements RefInfo<T> {

        public final Class type;
        public final SafeOpt<List<RefInfoFieldIObjectField>> fields;
        public final SafeOpt<List<String>> fieldNames;

        public final SafeOpt<Constructor> constructor;

        public RefInfoClass(Class type) {
            this.type = Objects.requireNonNull(type);
            constructor = SafeOpt.ofLazy(type).map(m -> m.getDeclaredConstructor());
            fields = SafeOpt.ofLazy(() -> (SimpleStream<IObjectField>) ReflFields.getLocalFields(type))
                    .map(stream -> stream
                            .filter(io -> includeFilter(io))
                            .map(io -> new RefInfoFieldIObjectField(io)))
                    .map(stream -> stream.toList());
            fieldNames = fields.map(m -> m.stream().map(field -> field.getName()).toList());

        }

        public boolean includeFilter(IObjectField field) {
            if (field.isTransient()) {
                return false;
            }
            return true;
        }

        @Override
        public Class getType() {
            return type;
        }

        @Override
        public List<String> getFieldNames() {
            return fieldNames.get();
        }

        @Override
        public List<RefInfoField> getFields() {
            return (List) fields.get();
        }

        @Override
        public SafeOpt<T> initObject(boolean mapping, Map<String, Object> map) {
            return SafeOpt.ofNullable(map).map(rs -> {
                T pojo = (T) constructor.get().newInstance();
                for (RefInfoFieldIObjectField field : fields.get()) {
                    String name = field.getName();
                    if (map.containsKey(name)) {
                        Object value = map.getOrDefault(field.getName(), null);
                        Object internal = mapping ? field.getMapper().toInternal(value) : value;
                        field.setFieldValue(pojo, internal).throwAny(); // don't overwrite defaults
                    }

                }
                return pojo;
            });
        }

    }
}
