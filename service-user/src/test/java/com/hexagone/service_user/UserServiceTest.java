package com.hexagone.service_user;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class UserServiceTest {

    @Mock
    private JsonObject config;

    private UserService userService;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        // Mock the configuration
        when(config.getString("db.port")).thenReturn("5432");
        when(config.getString("db.host")).thenReturn("localhost");
        when(config.getString("db.database")).thenReturn("bibliocloud-pg");
        when(config.getString("db.user")).thenReturn("postgres");
        when(config.getString("db.password")).thenReturn("mypgdbpass");

        userService = new UserService(vertx, config);
        testContext.completeNow();
    }

    @Test
    void testCreateUser(VertxTestContext testContext) {
        userService.createUser("John Doe", "john@example.com", "password123")
            .onComplete(testContext.succeeding(user -> {
                testContext.verify(() -> {
                    assertNotNull(user);
                    assertEquals("John Doe", user.getName());
                    assertEquals("john@example.com", user.getEmail());
                    assertNotNull(user.getId());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetUser(VertxTestContext testContext) {
        userService.createUser("Jane Doe", "jane@example.com", "password456")
            .compose(createdUser -> userService.getUser(createdUser.getId()))
            .onComplete(testContext.succeeding(user -> {
                testContext.verify(() -> {
                    assertNotNull(user);
                    assertEquals("Jane Doe", user.getName());
                    assertEquals("jane@example.com", user.getEmail());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetAllUsers(VertxTestContext testContext) {
        userService.createUser("User1", "user1@example.com", "pass1")
            .compose(v -> userService.createUser("User2", "user2@example.com", "pass2"))
            .compose(v -> userService.getAllUsers())
            .onComplete(testContext.succeeding(users -> {
                testContext.verify(() -> {
                    assertNotNull(users);
                    assertTrue(users.size() >= 2);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateUser(VertxTestContext testContext) {
        userService.createUser("Old Name", "old@example.com", "oldpass")
            .compose(createdUser -> 
                userService.updateUser(createdUser.getId(), "New Name", "new@example.com", "newpass")
            )
            .onComplete(testContext.succeeding(updatedUser -> {
                testContext.verify(() -> {
                    assertNotNull(updatedUser);
                    assertEquals("New Name", updatedUser.getName());
                    assertEquals("new@example.com", updatedUser.getEmail());
                    testContext.completeNow();
                });
            }));
    }
}