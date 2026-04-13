package com.example.bezma.listener;

import lombok.extern.slf4j.Slf4j;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class RedisExpirationListener extends KeyExpirationEventMessageListener {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public RedisExpirationListener(RedisMessageListenerContainer listenerContainer,
                                   TenantRepository tenantRepository,
                                   UserRepository userRepository) {
        super(listenerContainer);
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith("registration_checkpoint:")) {
            try {
                Long tenantId = Long.parseLong(expiredKey.split(":")[1]);
                log.warn("Registration expired! Cleaning up Tenant ID: {}", tenantId);

                // 1. Delete Users associated with Tenant
                userRepository.deleteByTenantId(tenantId);

                // 2. Delete Tenant
                tenantRepository.deleteById(tenantId);

                log.info("Successfully cleaned up expired registration data for Tenant ID: {}", tenantId);
            } catch (Exception e) {
                log.error("Error during auto-cleanup: {}", e.getMessage());
            }
        }
    }
}
