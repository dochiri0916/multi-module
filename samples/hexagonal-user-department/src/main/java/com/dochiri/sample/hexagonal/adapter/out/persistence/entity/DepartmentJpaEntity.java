package com.dochiri.sample.hexagonal.adapter.out.persistence.entity;

import com.dochiri.jpa.entity.BaseEntity;
import com.dochiri.sample.hexagonal.domain.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = @UniqueConstraint(name = "uk_departments_name", columnNames = "name")
)
public class DepartmentJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    protected DepartmentJpaEntity() {
    }

    private DepartmentJpaEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static DepartmentJpaEntity from(Department department) {
        return new DepartmentJpaEntity(department.name(), department.description());
    }

    public Department toDomain() {
        return new Department(id, name, description);
    }
}
