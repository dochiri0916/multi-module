package com.example.securityjpa;

import com.dochiri.jpa.adapter.in.bootstrap.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "audited_security_entities")
@Getter
class AuditedSecurityEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected AuditedSecurityEntity() {
        super();
    }

    AuditedSecurityEntity(String name) {
        super();
        this.name = name;
    }

    void rename(String name) {
        this.name = name;
    }
}
