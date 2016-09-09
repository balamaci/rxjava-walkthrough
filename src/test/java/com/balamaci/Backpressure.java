package com.balamaci;

import com.balamaci.util.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;

/**
 * @author sbalamaci
 */
public class Backpressure {

    private static final Logger log = LoggerFactory.getLogger(Backpressure.class);

    @Test
    public void testThrowingBackpressureNotSupported() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> observable = observableWithoutBackpressureSupport();

        observable.observeOn(Schedulers.io())
                .subscribe(val -> {
                            log.info("Got {}", val);
                            Utils.sleepMillis(50);
                        },
                        err -> {
                            log.error("Subscriber got error", err);
                            latch.countDown();
                        });
        Utils.wait(latch);
    }

    private Observable<Integer> observableWithoutBackpressureSupport() {
        return Observable.create(subscriber -> {
            log.info("Started emitting");

            for(int i=0; i < 200; i++) {
                log.info("Emitting {}", i);
                subscriber.onNext(i);
            }

            subscriber.onCompleted();
        });
    }

}
