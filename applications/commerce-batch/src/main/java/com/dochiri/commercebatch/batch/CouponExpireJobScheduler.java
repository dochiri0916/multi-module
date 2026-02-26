package com.dochiri.commercebatch.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CouponExpireJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job couponExpireJob;

    public CouponExpireJobScheduler(JobLauncher jobLauncher, Job couponExpireJob) {
        this.jobLauncher = jobLauncher;
        this.couponExpireJob = couponExpireJob;
    }

    @Scheduled(cron = "${commerce.batch.coupon-expire.cron:0 */5 * * * *}")
    public void runCouponExpireJob() {
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("requestedAt", System.currentTimeMillis())
            .toJobParameters();

        try {
            jobLauncher.run(couponExpireJob, jobParameters);
        } catch (Exception ex) {
            log.error("Failed to run couponExpireJob", ex);
        }
    }
}
