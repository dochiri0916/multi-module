package com.example.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuditableEntityRepository extends JpaRepository<AuditableEntity, Long> {
}
