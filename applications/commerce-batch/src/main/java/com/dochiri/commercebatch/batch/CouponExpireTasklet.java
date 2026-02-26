package com.dochiri.commercebatch.batch;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CouponExpireTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireTasklet.class);

    private final JdbcTemplate jdbcTemplate;

    public CouponExpireTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ZonedDateTime now = ZonedDateTime.now();
        int updatedCount = jdbcTemplate.update(
            """
                update coupons
                   set status = ?, updated_at = ?
                 where status = ?
                   and expires_at < ?
                """,
            "EXPIRED",
            Timestamp.from(now.toInstant()),
            "ISSUED",
            Timestamp.from(now.toInstant())
        );
        log.info("Expired coupons updated. now={}, updatedCount={}", now, updatedCount);
        return RepeatStatus.FINISHED;
    }
}
