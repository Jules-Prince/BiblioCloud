package com.hexagone.service_user;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class User {

    private String id;
    private String name;
    private String email;
    private String password;

     public JsonObject toJson() {
        return new JsonObject()
            .put("id", this.id)
            .put("name", this.name)
            .put("email", this.email);
    }
}
