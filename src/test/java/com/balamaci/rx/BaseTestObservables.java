package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
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

    default void subscribeWithLogWaiting(Observable observable) {
        CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                logError(latch),
                logComplete(latch)
        );

        Helpers.wait(latch);
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
        Observable<T> itemsStream = Observable.just(t1, t2, t3);
        Observable<Long> timer = Observable.interval(initialDelay, interval, unit);

        return Observable.zip(itemsStream, timer, (key, val) -> key);
    }

    default  <T> Observable<T> periodicEmitter(T[] items, int interval,
                                               TimeUnit unit, int initialDelay) {
        Observable<T> itemsStream = Observable.from(items);
        Observable<Long> timer = Observable.interval(initialDelay, interval, unit);

        return Observable.zip(itemsStream, timer, (key, val) -> key);
    }

    default  <T> Observable<T> periodicEmitter(T[] items, int interval,
                                               TimeUnit unit) {
        return periodicEmitter(items, interval, unit);
    }

    default  Observable<String> delayedByLengthEmitter(TimeUnit unit, String...items) {
        Observable<String> itemsStream = Observable.from(items);

        return itemsStream.concatMap(item -> Observable.just(item)
                        .doOnNext(val -> log.info("Received {} delaying for {} ", val, val.length()))
                        .delay(item.length(), unit)
                );
    }

    default Action1<Throwable> logError() {
        return err -> log.error("Subscriber received error '{}'", err.getMessage());
    }

    default Action1<Throwable> logError(CountDownLatch latch) {
        return err -> {
            log.error("Subscriber received error '{}'", err.getMessage());
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


