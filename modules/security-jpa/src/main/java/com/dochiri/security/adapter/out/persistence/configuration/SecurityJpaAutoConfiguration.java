package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.adapter.out.persistence.RefreshSessionBulkRevocationAdapter;
import com.dochiri.security.adapter.out.persistence.RefreshSessionCleanupAdapter;
import com.dochiri.security.adapter.out.persistence.RefreshSessionPersistenceAdapter;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;

@AutoConfiguration(before = {
        DataJpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@Import({
        SecurityJpaPackageRegistrar.class,
        RefreshSessionPersistenceAdapter.class,
        RefreshSessionBulkRevocationAdapter.class,
        RefreshSessionCleanupAdapter.class
})
public class SecurityJpaAutoConfiguration {
}
