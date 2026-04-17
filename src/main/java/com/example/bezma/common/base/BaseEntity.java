package com.example.bezma.common.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Filter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = Long.class)})
@FilterDef(name = "deletedFilter", parameters = {@ParamDef(name = "isDeleted", type = Boolean.class)})
@Filter(name = "deletedFilter", condition = "is_deleted = :isDeleted")
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    @CreatedBy
    private String createdBy;
    @LastModifiedBy
    private String updatedBy;
}
