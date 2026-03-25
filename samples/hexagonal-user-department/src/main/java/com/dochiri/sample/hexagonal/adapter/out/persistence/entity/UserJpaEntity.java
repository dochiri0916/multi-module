package com.dochiri.sample.hexagonal.adapter.out.persistence.entity;

import com.dochiri.jpa.entity.BaseEntity;
import com.dochiri.sample.hexagonal.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = @Index(name = "idx_users_department_id", columnList = "department_id")
)
public class UserJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    protected UserJpaEntity() {
    }

    private UserJpaEntity(String name, String email, Long departmentId) {
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
    }

    public static UserJpaEntity from(User user) {
        return new UserJpaEntity(user.name(), user.email(), user.departmentId());
    }

    public User toDomain() {
        return new User(id, name, email, departmentId);
    }
}
