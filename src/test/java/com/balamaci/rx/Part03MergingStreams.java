package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part03MergingStreams implements BaseTestObservables {

    @Test
    public void zipUsedToSlowDown() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> colors = Observable.just("red", "green", "blue");
        Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

        Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
        subscribeWithLog(periodicEmitter, latch);

        Helpers.wait(latch);
    }
}
