package com.hexagone.service_user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);       
    private final SqlClient client;
    
    public UserService(Vertx vertx, JsonObject config) {
        PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(Integer.parseInt(config.getString("db.port")))
            .setHost(config.getString("db.host"))
            .setDatabase(config.getString("db.database"))
            .setUser(config.getString("db.user"))
            .setPassword(config.getString("db.password"));

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

        client = Pool.pool(vertx, connectOptions, poolOptions);

        // Create the users table if it doesn't exist
        client.query("CREATE TABLE IF NOT EXISTS users (id UUID PRIMARY KEY, name VARCHAR(255), email VARCHAR(255))")
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public Future<User> createUser(String name, String email) {
        String id = UUID.randomUUID().toString();
        return client.preparedQuery("INSERT INTO users (id, name, email) VALUES ($1, $2, $3) RETURNING *")
            .execute(Tuple.of(id, name, email))
            .compose(rowSet -> {
                if (rowSet.size() > 0) {
                    return Future.succeededFuture(toUser(rowSet.iterator().next()));
                } else {
                    return Future.failedFuture("Failed to create user");
                }
            });
    }

    public Future<User> getUser(String id) {
        return client.preparedQuery("SELECT * FROM users WHERE id = $1")
            .execute(Tuple.of(id))
            .compose(rowSet -> {
                if (rowSet.size() > 0) {
                    return Future.succeededFuture(toUser(rowSet.iterator().next()));
                } else {
                    return Future.failedFuture("User not found");
                }
            });
    }

    public Future<List<User>> getAllUsers() {
        return client.query("SELECT * FROM users")
            .execute()
            .compose(rowSet -> {
                List<User> users = new ArrayList<>();
                for (Row row : rowSet) {
                    users.add(toUser(row));
                }
                return Future.succeededFuture(users);
            });
    }

    public Future<User> updateUser(String id, String name, String email) {
        return client.preparedQuery("UPDATE users SET name = $1, email = $2 WHERE id = $3 RETURNING *")
            .execute(Tuple.of(name, email, id))
            .compose(rowSet -> {
                if (rowSet.size() > 0) {
                    return Future.succeededFuture(toUser(rowSet.iterator().next()));
                } else {
                    return Future.failedFuture("User not found");
                }
            });
    }

    private User toUser(Row row) {
        return new User(row.getUUID("id").toString(), row.getString("name"), row.getString("email"));
    }
}