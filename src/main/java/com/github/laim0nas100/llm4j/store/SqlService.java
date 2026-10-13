/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.github.laim0nas100.llm4j.store;

import com.github.laim0nas100.commonslb.Java;
import com.github.laim0nas100.dbstore.filestore.SqliteFileStore;
import com.github.laim0nas100.dbstore.idgen.MultiSqliteCounter;
import com.github.laim0nas100.dbstore.keystore.SqliteKeyValueStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 *
 * @author Lemmin
 */
public class SqlService {

    public final Jdbi jdbi;
    public final DataSource dataSource;

    public final SqliteFileStore fileStore;
    public final SqliteKeyValueStore keyValueStore;
    public final MultiSqliteCounter multiCounter;

    public SqlService(DataSource dataSource) throws SQLException {
        this.dataSource = Objects.requireNonNull(dataSource);
        jdbi = Jdbi.create(this.dataSource);
        fileStore = new SqliteFileStore(jdbi);

        keyValueStore = new SqliteKeyValueStore(jdbi);

        multiCounter = new MultiSqliteCounter(jdbi);
    }

    public SqlService(Path dbFile) throws IOException, SQLException {
        this(createDefault(dbFile));
    }

    public static HikariDataSource createDefault(Path dbFile) throws IOException {
        Objects.requireNonNull(dbFile);
        Files.createDirectories(dbFile.getParent());
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(org.sqlite.JDBC.class.getName());
        config.setJdbcUrl(String.format("jdbc:sqlite:%s", dbFile.toAbsolutePath().toString()));
        config.setMaximumPoolSize(Java.getAvailableProcessors());
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        return new HikariDataSource(config);
    }
}
