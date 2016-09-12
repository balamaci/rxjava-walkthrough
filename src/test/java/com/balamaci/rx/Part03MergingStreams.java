package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part03MergingStreams implements BaseTestObservables {

    /**
     * Zip operator operates sort of like a zipper in the sense that it takes
     * an event from one stream and waits for an event from the other stream. Once an event for the other stream
     * arrives, it uses the zip function to merge the two events
     *
     */
    @Test
    public void zipUsedToSlowDown() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> colors = Observable.just("red", "green", "blue");
        Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

        Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
        subscribeWithLog(periodicEmitter, latch);

        Helpers.wait(latch);
    }

    @Test
    public void mergingStreams() {
        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(2);

        BlockingObservable observable = Observable.merge(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }


    @Test
    public void concatStreams() {
        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(4);

        BlockingObservable observable = Observable.concat(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }


}
