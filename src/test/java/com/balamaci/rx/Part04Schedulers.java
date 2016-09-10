package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
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

        Observable<Integer> observable = simpleObservable()
                .subscribeOn(Schedulers.io())
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                });

        subscribeWithLog(observable, latch);
        Helpers.wait(latch);
    }

    @Test
    public void testObserveOn() {
        log.info("Starting");

        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> observable = simpleObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                })
                .observeOn(Schedulers.newThread());

        subscribeWithLog(observable, latch);
        Helpers.wait(latch);
    }

}
