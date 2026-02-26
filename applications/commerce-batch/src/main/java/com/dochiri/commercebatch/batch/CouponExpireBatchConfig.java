package com.dochiri.commercebatch.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CouponExpireBatchConfig {

    @Bean
    public Step couponExpireStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CouponExpireTasklet couponExpireTasklet
    ) {
        return new StepBuilder("couponExpireStep", jobRepository)
            .tasklet(couponExpireTasklet, transactionManager)
            .build();
    }

    @Bean
    public Job couponExpireJob(JobRepository jobRepository, Step couponExpireStep) {
        return new JobBuilder("couponExpireJob", jobRepository)
            .start(couponExpireStep)
            .build();
    }
}
