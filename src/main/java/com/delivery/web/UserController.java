package com.delivery.web;

import com.delivery.model.Client;
import com.delivery.model.User;
import com.delivery.repository.ClientRepository;
import com.delivery.service.UserService;
import com.delivery.web.dto.ClientProfileDto;
import com.delivery.web.dto.UpdateClientProfileRequest;
import com.delivery.web.dto.UserDto;
import io.javalin.http.Context;

import java.util.List;

/**
 * User endpoints: the header selector list and the client profile page.
 *
 * <p>Controllers are thin by design (Layered Architecture): they translate HTTP
 * to service calls and domain objects to DTOs — no business rules here.</p>
 */
public class UserController {

    private final UserService userService;
    private final ClientRepository clientRepository;

    public UserController(UserService userService, ClientRepository clientRepository) {
        this.userService = userService;
        this.clientRepository = clientRepository;
    }

    /** GET /api/users — every user with a role badge for the header selector. */
    public void getAllUsers(Context ctx) {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole().getDbValue()))
                .toList();
        ctx.json(users);
    }

    /** GET /api/clients/{userId} — the client profile for the user page. */
    public void getClientProfile(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        User user = userService.getUserById(userId);
        Client client = clientRepository.findByIdUser(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client profile not found for user " + userId));
        ctx.json(toProfileDto(user, client));
    }

    /** PUT /api/clients/{userId}/profile — rename / phone / address. */
    public void updateProfile(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        UpdateClientProfileRequest request = ctx.bodyAsClass(UpdateClientProfileRequest.class);
        Client updated = userService.updateClientProfile(
                userId, request.username(), request.phone(), request.address());
        User user = userService.getUserById(userId);
        ctx.json(toProfileDto(user, updated));
    }

    private ClientProfileDto toProfileDto(User user, Client client) {
        return new ClientProfileDto(user.getId(), client.getId(), user.getUsername(),
                client.getPhone(), client.getDefaultAddress());
    }
}
