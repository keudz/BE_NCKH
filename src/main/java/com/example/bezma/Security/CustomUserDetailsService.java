package com.example.bezma.Security;

import lombok.RequiredArgsConstructor;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.aUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Thử tìm theo Username trước (Dành cho Basic Login)
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByZaloId(identifier)) // 2. Nếu không thấy, thử tìm theo ZaloId
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với: " + identifier));
    }
}