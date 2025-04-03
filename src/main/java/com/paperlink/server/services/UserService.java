package com.paperlink.server.services;

import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.UserNotFoundException;
import com.paperlink.server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Find a user by ID
     *
     * @param id User ID
     * @return The user entity
     * @throws UserNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserEntity findById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    @Transactional(readOnly = true)
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    /**
     * Create a new user
     *
     * @param userEntity The user entity to create
     * @return The created user entity
     */
    @Transactional
    public UserEntity createUser(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    /**
     * Update an existing user
     *
     * @param id          User ID
     * @param userDetails Updated user details
     * @return The updated user entity
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public UserEntity updateUser(String id, UserEntity userDetails) {
        UserEntity user = findById(id);

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());

        return userRepository.save(user);
    }

    /**
     * Delete a user
     *
     * @param id User ID
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public void deleteUser(String id) {
        UserEntity user = findById(id);
        userRepository.delete(user);
    }

    /**
     * Load user by username
     *
     * @param username Username of the user
     * @return UserDetails of the user
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }
}