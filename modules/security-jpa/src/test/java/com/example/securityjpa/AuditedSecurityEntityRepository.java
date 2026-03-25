package com.example.securityjpa;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuditedSecurityEntityRepository extends JpaRepository<AuditedSecurityEntity, Long> {
}
