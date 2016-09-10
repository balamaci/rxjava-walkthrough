package com.balamaci.rx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.BlockingObservable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public interface BaseTestObservables {

    Logger log = LoggerFactory.getLogger(BaseTestObservables.class);

    default Observable<Integer> simpleObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            log.info("Emitting 2nd");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        return observable;
    }

    default void subscribeWithLog(Observable observable) {
        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                logError(),
                logComplete()
        );
    }

    default void subscribeWithLog(Observable observable, CountDownLatch latch) {
        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                logError(latch),
                logComplete(latch)
        );
    }

    default void subscribeWithLog(BlockingObservable observable) {
        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                logError(),
                logComplete()
        );
    }

    default  <T> Observable<T> periodicEmitter(T t1, T t2, T t3, int interval, TimeUnit unit) {
        return periodicEmitter(t1, t2, t3, interval, unit, interval);
    }

    default  <T> Observable<T> periodicEmitter(T t1, T t2, T t3, int interval,
                                               TimeUnit unit, int initialDelay) {
        Observable<T> colors = Observable.just(t1, t2, t3);
        Observable<Long> timer = Observable.interval(initialDelay, interval, unit);

        return Observable.zip(colors, timer, (key, val) -> key);
    }

    default Action1<Throwable> logError() {
        return err -> log.error("Subscriber received error", err);
    }

    default Action1<Throwable> logError(CountDownLatch latch) {
        return err -> {
            log.error("Subscriber received error", err);
            latch.countDown();
        };
    }

    default Action0 logComplete() {
        return () -> log.info("Subscriber got Completed event");
    }

    default Action0 logComplete(CountDownLatch latch) {
        return () -> {
            log.info("Subscriber got Completed event");
            latch.countDown();
        };
    }

}


