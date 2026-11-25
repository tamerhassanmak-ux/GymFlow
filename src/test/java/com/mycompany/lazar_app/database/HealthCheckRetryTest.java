package com.mycompany.lazar_app.database;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HealthCheckRetryTest {
    private final DatabaseHealthCheck.Result ready=new DatabaseHealthCheck.Result("fixture","1","1");
    @Test void transientAcquisitionRetriesOnlyOnce() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        assertEquals(ready,DatabaseHealthCheck.retry(() -> {
            if(calls.incrementAndGet()==1)throw new SQLTransientConnectionException("fixture","08001");return ready;
        },millis->assertEquals(250,millis)));
        assertEquals(2,calls.get());
    }
    @Test void retryBudgetIsBounded() {
        AtomicInteger calls=new AtomicInteger();
        assertThrows(SQLTimeoutException.class,()->DatabaseHealthCheck.retry(()->{
            calls.incrementAndGet();throw new SQLTimeoutException("fixture","HYT00");
        },millis->{})); assertEquals(2,calls.get());
    }
    @Test void authTlsAndUnknownFailuresNeverRetry() {
        for(SQLException failure:new SQLException[]{new SQLException("auth","28000"),
                new SQLTransientConnectionException("tls","08001",new javax.net.ssl.SSLHandshakeException("fixture")),
                new SQLException("unknown")}) {
            AtomicInteger calls=new AtomicInteger();
            assertSame(failure,assertThrows(SQLException.class,()->DatabaseHealthCheck.retry(()->{calls.incrementAndGet();throw failure;},millis->fail("No retry"))));
            assertEquals(1,calls.get());
        }
    }
    @Test void interruptionCancelsRetry() {
        AtomicInteger calls=new AtomicInteger();
        try {
            SQLException failure=assertThrows(SQLException.class,()->DatabaseHealthCheck.retry(()->{calls.incrementAndGet();throw new SQLTimeoutException();},millis->{throw new InterruptedException();}));
            assertEquals("HY008",failure.getSQLState());assertEquals(1,calls.get());assertTrue(Thread.currentThread().isInterrupted());
        } finally {Thread.interrupted();}
    }
}
