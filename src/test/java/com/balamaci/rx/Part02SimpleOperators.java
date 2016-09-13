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
     * Delay operator - the Thread.sleep of the reactive world, it's pausing for a particular increment of time
     * before emitting the whole range events which are thus shifted by the specified time amount.
     *
     * The delay operator uses a Scheduler {@see Part04Schedulers} by default, which actually means it's
     * running the operators and the subscribe operations on a different thread, which means the test method
     * will terminate before we see the text from the log.
     *
     * To prevent this we use the .toBlocking() operator which returns a BlockingObservable. Operators on
     * BlockingObservable block(wait) until upstream Observable is completed
     */
    @Test
    public void delayOperator() {
        Observable.range(0, 5)
                .delay(5, TimeUnit.SECONDS)
                .toBlocking() //waits on the main thread for the Scheduler thread to finish.
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));


//        Helpers.sleepMillis(10000);
    }

    /**
     * Periodically emits a number starting from 0 and then increasing the value on each emission
     */
    @Test
    public void intervalOperator() {
        log.info("Starting");
        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)
                .toBlocking()
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }

    /**
     * Timer operator waits for a specific amount of time before it emits an event and then completes
     */
    @Test
    public void timerOperator() {
        log.info("Starting");
        Observable.timer(5, TimeUnit.SECONDS)
                .toBlocking()
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }



}
