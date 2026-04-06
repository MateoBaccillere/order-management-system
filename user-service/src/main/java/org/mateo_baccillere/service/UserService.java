package org.mateo_baccillere.service;

import lombok.RequiredArgsConstructor;
import org.mateo_baccillere.entity.User;
import org.mateo_baccillere.entity.UserRole;
import org.mateo_baccillere.exception.DuplicateEmailException;
import org.mateo_baccillere.exception.UserNotFoundException;
import org.mateo_baccillere.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .role(UserRole.BUYER)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public UserRole getRoleById(Long id) {
        return getById(id).getRole();
    }

    public User promoteToSeller(Long id) {
        User user = getById(id);
        user.setRole(UserRole.SELLER);
        return userRepository.save(user);
    }
}
