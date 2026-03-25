package com.example.bezma.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants",indexes = {
        @Index(name = "idx_teant_slug",columnList = "slug"),
        @Index(name = "idx_teant_code",columnList = "code")

})

public class Tenant {
}
