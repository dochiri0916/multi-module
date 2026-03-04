package com.dochiri.jpa.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;

    private LocalDateTime deletedAt;

    public boolean markDeleted(LocalDateTime now) {
        requireNonNull(now, "now must not be null");

        if (this.deletedAt != null) {
            return false;
        }

        this.deletedAt = now;
        return true;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}