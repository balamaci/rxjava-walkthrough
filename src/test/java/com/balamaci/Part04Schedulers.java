package com.balamaci;

import com.balamaci.util.Utils;
import org.junit.Test;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;

/**
 * @author sbalamaci
 */
public class Part04Schedulers implements BaseTestObservables {

    @Test
    public void testSubscribeOn() {
        log.info("Starting");

        CountDownLatch latch = new CountDownLatch(1);
        Observable<Integer> observable = simpleObservable();

        observable
                .subscribeOn(Schedulers.computation())
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                })
//                .toBlocking()
                .subscribe(val -> log.info("Subscribe received {}", val),
                        logError(latch),
                        logComplete(latch));
        Utils.wait(latch);
    }
}
