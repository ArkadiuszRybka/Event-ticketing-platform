package com.rybka.ticketing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdvisoryLockService {
    private final JdbcTemplate jdbc;

    public boolean tryAcquire(long key){
        Boolean ok = jdbc.queryForObject("select pg_try_advisory_lock(?)", Boolean.class, key);
        return ok != null && ok;
    }

    public void release(long key){
        jdbc.execute("select pg_advisory_unlock(" + key + ")");
    }
}
