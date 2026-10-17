package com.github.laim0nas100.llm4j;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.laim0nas100.cfg.TolerantConfig;
import com.github.laim0nas100.commonslb.Java;
import com.github.laim0nas100.commonslb.io.autopath.AutoPath;
import com.github.laim0nas100.dbstore.JdbiMixin;
import com.github.laim0nas100.dbstore.filestore.SqliteFileStore;
import com.github.laim0nas100.dbstore.idgen.MultiSqliteCounter;
import com.github.laim0nas100.llm4j.store.SqlService;
import com.github.laim0nas100.uncheckedutils.LazySafe;
import com.github.laim0nas100.uncheckedutils.SafeOpt;
import com.github.laim0nas100.uncheckedutils.func.UncheckedRunnable;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.tika.io.CacheMemoryBudget;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Simple explicit singleton dependency injection via default interface methods.
 *
 * @author Lemmin
 */
public class GlobalCTX {

    private static final Logger logger = LoggerFactory.getLogger(GlobalCTX.class);

    public static final String PROPERTIES_NAME = "llm4j";

    private static final LazySafe<TolerantConfig> config = new LazySafe<>(() -> {
        try {
            return new PropertyConfig(PROPERTIES_NAME).loadConfig();
        } catch (Exception error) {
            logger.error("Failed to load properties file", error);
            return TolerantConfig.empty();// uses all the default values or throws where no defaults are provided.
        }
    });
    public static final AutoPath WORK_DIR = AutoPath.fs(Java.getUserHome(), "lb-soft", "LLM4J");
    public static final AutoPath MAIN_FILE = WORK_DIR.concat("FileDatabase.db");
    public static final AutoPath BLOB_FILE = WORK_DIR.concat("BlobDatabase.db");

    public static LazySafe<HikariDataSource> dsMap(LazySafe<?> parent, AutoPath path) {
        return parent.map(c -> SqlService.createDefault(path.toPath())).withCleanup(c -> c.ifPresent(ds -> ds.close()));
    }

    public static final LazySafe<HikariDataSource> MAIN_DS = dsMap(config, MAIN_FILE);
    public static final LazySafe<HikariDataSource> BLOB_DS = dsMap(config, BLOB_FILE);

    public static final LazySafe<Jdbi> BLOB_JDBI = BLOB_DS.map(Jdbi::create);
    public static final LazySafe<Jdbi> JDBI = MAIN_DS.map(Jdbi::create);
    public static final LazySafe<MultiSqliteCounter> COUNTERS = JDBI.map(c -> new MultiSqliteCounter(c) {
        @Override
        protected void initialize() throws SQLException {
            super.initialize();
            this.increment = 10;
        }

    });
    public static final String BLOB_TABLE_NAME_BLOB = "FileBlobStore";
    public static final String BLOB_TABLE_NAME_META = "FileStore";
    public static final LazySafe<SqliteFileStore> FILE_STORE = BLOB_JDBI.map(c -> new SqliteFileStore(c, BLOB_TABLE_NAME_META, BLOB_TABLE_NAME_BLOB));

    public static final LazySafe<CacheMemoryBudget> MEMORY_BUDGET = config.map(c -> {
        long bytes = 500 * 1024 * 1024; // 500MB
        if (c != null) {
            bytes = c.getLong("cache.memory.budged", bytes);
        }
        return new CacheMemoryBudget(bytes);
    });
    private static final AtomicBoolean shutdown = new AtomicBoolean(false);
    private static final List<UncheckedRunnable> shutdownList = new ArrayList<>();
    private static final JsonMapper jsonMapper = new JsonMapper();
    private static final ExecutorService executor = SafeOpt.of(Executors.newVirtualThreadPerTaskExecutor()).peek(exe -> {
        shutdownList.add(exe::shutdown);
    }).get();

    public static void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            //reverse order
            for (int i = shutdownList.size() - 1; i >= 0; i--) {
                try {
                    shutdownList.get(i).runUnchecked();
                } catch (Throwable th) {
                    logger.error("Failed at shutdown", th);
                }
            }
        }
    }

    public interface LifeCycle {

        public default void register(AutoCloseable auto) {
            registerShutdown(() -> auto.close());
        }

        public default void registerShutdown(UncheckedRunnable run) {
            if (!shutdown.get()) {
                synchronized (shutdownList) {
                    shutdownList.add(run);
                }
            }
        }
    }

    public interface Globals extends LifeCycle {

        public default JsonMapper jsonMapper() {
            return jsonMapper;
        }

        public default ExecutorService executorService() {
            return executor;
        }

        public default TolerantConfig config() {
            return config.get();
        }

    }

    public interface SqlDependency extends JdbiMixin, LifeCycle {

        @Override
        public default Jdbi getJdbi() {
            return JDBI.get();
        }

        public default Jdbi jdbi() {
            return JDBI.get();
        }

        public default SqliteFileStore fileStore() {
            return FILE_STORE.get();
        }

        public default MultiSqliteCounter counters() {
            return COUNTERS.get();
        }

    }
}
