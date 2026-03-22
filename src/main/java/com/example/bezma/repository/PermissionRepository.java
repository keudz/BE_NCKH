package com.example.bezma.repository;

import com.example.bezma.entity.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long>{
}
