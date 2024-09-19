package com.hexagone.service_user;

import io.vertx.core.json.JsonObject;

public class User {

    private String id;
    private String name;
    private String email;

    // Constructors, getters, and setters
    public User() {}

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

     public JsonObject toJson() {
        return new JsonObject()
            .put("id", this.id)
            .put("name", this.name)
            .put("email", this.email);
    }
}
