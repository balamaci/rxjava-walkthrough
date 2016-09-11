package com.balamaci.rx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * @author sbalamaci
 */
public class Helpers {

    private static final Logger log = LoggerFactory.getLogger(Helpers.class);

    public static void sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Interrupted Thread");
            throw new RuntimeException("Interrupted thread");
        }
    }

    public static void wait(CountDownLatch waitOn) {
        try {
            waitOn.await();
        } catch (InterruptedException e) {
            log.error("Interrupted waiting on CountDownLatch");
            throw new RuntimeException("Interrupted thread");
        }
    }

}
