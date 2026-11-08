package com.delivery.service;

import com.delivery.model.User;
import com.delivery.repository.UserRepository;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> getUser(String id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
