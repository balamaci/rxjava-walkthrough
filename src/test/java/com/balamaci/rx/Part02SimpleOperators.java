package com.balamaci.rx;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part02SimpleOperators {

    private static final Logger log = LoggerFactory.getLogger(Part02SimpleOperators.class);

    /**
     * Timer operator emits an event and then completes
     */
    @Test
    public void timerOperator() {
        Observable.timer(5, TimeUnit.SECONDS)
                .toBlocking()
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }

    @Test
    public void intervalOperator() {
        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)
                .toBlocking()
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }



}
