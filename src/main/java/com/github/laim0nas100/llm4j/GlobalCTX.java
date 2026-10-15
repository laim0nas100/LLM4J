package com.github.laim0nas100.llm4j;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.laim0nas100.cfg.TolerantConfig;
import com.github.laim0nas100.commonslb.Java;
import com.github.laim0nas100.commonslb.io.autopath.AutoPath;
import com.github.laim0nas100.dbstore.JdbiMixin;
import com.github.laim0nas100.dbstore.filestore.SqliteFileStore;
import com.github.laim0nas100.dbstore.idgen.MultiSqliteCounter;
import com.github.laim0nas100.dbstore.keystore.SqliteKeyValueStore;
import com.github.laim0nas100.llm4j.store.SqlService;
import com.github.laim0nas100.uncheckedutils.LazySafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.tika.io.CacheMemoryBudget;
import org.jdbi.v3.core.Jdbi;

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

    public interface SqlDependency extends JdbiMixin {

        @Override
        public default Jdbi getJdbi() {
            return sqlService.get().jdbi;
        }
        
        public default SqliteFileStore fileStore(){
            return sqlService.get().fileStore;
        }
        
        public default SqliteKeyValueStore keyValueStore(){
            return sqlService.get().keyValueStore;
        }
        
        public default MultiSqliteCounter counters(){
            return sqlService.get().multiCounter;
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
    public static final AutoPath WORK_DIR = AutoPath.fs(Java.getUserHome(), "lb-soft", "LMStudio4J");
    public static final AutoPath DB_FILE = WORK_DIR.concat("FileDatabase.db");
    private static final LazySafe<SqlService> sqlService = config.map(c -> {
        return new SqlService(DB_FILE.toPath());
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
