package com.delivery.service;

import com.delivery.model.Client;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.ClientRepository;
import com.delivery.repository.UserRepository;

import java.util.List;

/**
 * User service: lookups and client profile editing.
 *
 * <p><b>Role checks live here, not in the web layer</b> (Layered Architecture):
 * a controller can be replaced by a CLI or a test without losing authorization.
 * The actor is always passed explicitly as a user id — there is no hidden
 * "current user" thread-local state.</p>
 */
public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public UserService(UserRepository userRepository, ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    /** @throws IllegalArgumentException if the user does not exist. */
    public User getUserById(long userId) {
        return ServiceGuards.requireUser(userRepository, userId);
    }

    /** All users — used by the header selector in the demo UI. */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Updates a client's profile: optional username change plus phone and address.
     *
     * @param actorUserId   who performs the edit (must be a CLIENT)
     * @param newUsername   new login, or {@code null}/blank to keep the current one
     * @param phone         new contact phone (required)
     * @param address       new default delivery address (required)
     * @return the updated client profile
     */
    public Client updateClientProfile(long actorUserId, String newUsername,
                                      String phone, String address) {
        User user = getUserById(actorUserId);
        if (user.getRole() != Role.CLIENT) {
            throw new IllegalStateException("Only clients can edit their profile");
        }
        Client client = ServiceGuards.requireClientProfile(clientRepository, user.getId());

        if (newUsername != null && !newUsername.isBlank()) {
            String trimmed = newUsername.trim();
            userRepository.findByUsername(trimmed).ifPresent(existing -> {
                if (existing.getId() != user.getId()) {
                    throw new IllegalArgumentException("Username is already taken: " + trimmed);
                }
            });
            user.updateUsername(trimmed);
            userRepository.save(user);
        }

        // Validation happens inside the model methods (encapsulation).
        client.updatePhone(phone);
        client.updateDefaultAddress(address);
        clientRepository.save(client);
        return client;
    }
}
