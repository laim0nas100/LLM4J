package com.github.laim0nas100.llm4j;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.laim0nas100.cfg.TolerantConfig;
import com.github.laim0nas100.uncheckedutils.LazySafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.tika.io.CacheMemoryBudget;

/**
 *
 * Simple explicit singleton dependency injection via default interface methods.
 *
 * @author Lemmin
 */
public class GlobalCTX {

    public interface GlobalDependency {

        public JsonMapper jsonMapper();

        public ExecutorService executorService();

        public TolerantConfig config();

    }

    public interface Globals extends GlobalDependency {

        @Override
        public default JsonMapper jsonMapper() {
            return ctx().jsonMapper();
        }

        @Override
        public default ExecutorService executorService() {
            return ctx().executorService();
        }

        @Override
        public default TolerantConfig config() {
            return ctx().config();
        }

    }

    public static final String PROPERTIES_NAME = "llm4j";

    private static final LazySafe<TolerantConfig> config = new LazySafe<>(() -> {
        try {
            return new PropertyConfig(PROPERTIES_NAME).loadConfig();
        } catch (Exception error) {
            System.getLogger("GlobalCTGX").log(System.Logger.Level.ERROR, "Failed to load properties file", error);
            return TolerantConfig.empty();
        }
    });

    public static final LazySafe<CacheMemoryBudget> MEMORY_BUDGET = config.map(c -> {
        long bytes = 500 * 1024 * 1024; // 500MB
        if (c != null) {
            bytes = c.getLong("cache.memory.budged", bytes);
        }
        return new CacheMemoryBudget(bytes);
    });

    private static final JsonMapper jsonMapper = new JsonMapper();
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public static GlobalDependency ctx = new GlobalDependency() {
        @Override
        public JsonMapper jsonMapper() {
            return jsonMapper;
        }

        @Override
        public ExecutorService executorService() {
            return executor;
        }

        @Override
        public TolerantConfig config() {
            return config.get();
        }
    };

    public static GlobalDependency ctx() {
        return ctx;
    }

}
