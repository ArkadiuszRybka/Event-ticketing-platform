package com.rybka.ticketing.service;

import com.rybka.ticketing.model.enums.Role;
import com.rybka.ticketing.model.entity.User;
import com.rybka.ticketing.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder encoder;

    public User register(String email, String rawPassword){
        if(userRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User();
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    public User authenticate(String email, String rawPassword){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if(!encoder.matches(rawPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return user;
    }

    public Optional<User> findById(Long id){
        return userRepository.findById(id);
    }
}
