package com.delivery.web;

import com.delivery.model.User;
import com.delivery.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Optional;

/**
 * Controller for handling HTTP requests related to Users.
 */
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void getAllUsers(Context ctx) {
        ctx.json(userService.getAllUsers());
    }

    public void getUser(Context ctx) {
        String id = ctx.pathParam("id");
        Optional<User> user = userService.getUser(id);
        if (user.isPresent()) {
            ctx.json(user.get());
        } else {
            ctx.status(HttpStatus.NOT_FOUND).result("User not found");
        }
    }

    public void createUser(Context ctx) {
        // For simplicity, we'll assume the user is passed in JSON
        // In a real app, we'd use a DTO or map the JSON to a User object
        // Since User is abstract, this is tricky without a concrete class.
        // Let's just return a success message for now.
        ctx.status(HttpStatus.CREATED).result("User creation endpoint called (implementation depends on concrete User type)");
    }
}
