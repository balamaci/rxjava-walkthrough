package com.balamaci.rx;

import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part05AdvancedOperators implements BaseTestObservables {

    @Test
    public void window() {
        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS);

        BlockingObservable<Long> delayedNumbersWindow = numbers
                .window(10, 5, TimeUnit.SECONDS)
                .flatMap(window -> window.doOnCompleted(() -> log.info("Window completed")))
                .toBlocking();

        subscribeWithLog(delayedNumbersWindow);
    }

}
