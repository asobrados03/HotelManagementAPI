package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> getAllUsers();
    Optional<User> getUserByEmail(String email);
    long createUser(User newUser);
    void updateUser(User user);
    int deleteUser(long id);
    void deleteAll();
    Optional<User> getUserById(long id);
}