package com.banking.account.service;

import com.banking.account.model.User;
import com.banking.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * CustomUserDetailsService — bridges Spring Security with our User database.
 *
 * TEACHING POINT — UserDetailsService:
 * Spring Security calls loadUserByUsername() whenever it needs to authenticate
 * or authorize a user. We load the User from our DB and wrap it in Spring's
 * UserDetails interface (which provides username, password, authorities/roles).
 *
 * SimpleGrantedAuthority wraps role strings like "ROLE_USER", "ROLE_ADMIN".
 * @PreAuthorize("hasRole('ADMIN')") checks for "ROLE_ADMIN" automatically.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())  // already Bcrypt-hashed in DB
                .authorities(
                        user.getRoles().stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                )
                .disabled(!user.isEnabled())
                .build();
    }
}
