package com.example.bezma.security;

import lombok.RequiredArgsConstructor;
import com.example.bezma.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Thử tìm theo Username trước (Dành cho Basic Login)
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByPhone(identifier)) // Thêm tìm theo Phone
                .or(() -> userRepository.findByZaloId(identifier)) // Tìm theo ZaloId
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với: " + identifier));

    }
}