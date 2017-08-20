package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public interface BaseTestObservables {

    Logger log = LoggerFactory.getLogger(BaseTestObservables.class);

    default Flowable<Integer> simpleFlowable() {
        return Flowable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            log.info("Emitting 2nd");
            subscriber.onNext(2);

            subscriber.onComplete();
        }, BackpressureStrategy.BUFFER);
    }

    default <T> void subscribeWithLog(Flowable<T> flowable) {
        flowable.subscribe(
                logNext(),
                logError(),
                logComplete()
        );
    }

    default <T> void subscribeWithLog(Observable<T> observable) {
        observable.subscribe(
                logNext(),
                logError(),
                logComplete()
        );
    }

    default <T> void subscribeWithLogOutputWaitingForComplete(Observable<T> observable) {
        CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(
                logNext(),
                logError(latch),
                logComplete(latch)
        );

        Helpers.wait(latch);
    }

    default <T> void subscribeWithLog(Single<T> single) {
        single.subscribe(
                val -> log.info("Subscriber received: {}", val),
                logError()
        );
    }

    default <T> void subscribeWithLogOutputWaitingForComplete(Flowable<T> flowable) {
        CountDownLatch latch = new CountDownLatch(1);

        flowable.subscribe(
                logNext(),
                logError(latch),
                logComplete(latch)
        );

        Helpers.wait(latch);
    }

    default <T> void subscribeWithLogOutputWaitingForComplete(Single<T> single) {
        CountDownLatch latch = new CountDownLatch(1);

        single.subscribe(
                val -> {
                    log.info("Subscriber received: {} and completed", val);
                    latch.countDown();
                },
                logError(latch)
        );

        Helpers.wait(latch);
    }

    default  <T> Flowable<T> periodicEmitter(T t1, T t2, T t3, int interval, TimeUnit unit) {
        return periodicEmitter(t1, t2, t3, interval, unit, interval);
    }

    default  <T> Flowable<T> periodicEmitter(T t1, T t2, T t3, int interval,
                                               TimeUnit unit, int initialDelay) {
        Flowable<T> itemsStream = Flowable.just(t1, t2, t3);
        Flowable<Long> timer = Flowable.interval(initialDelay, interval, unit);

        return Flowable.zip(itemsStream, timer, (key, val) -> key);
    }

    default  <T> Observable<T> periodicEmitter(T[] items, int interval,
                                               TimeUnit unit, int initialDelay) {
        Observable<T> itemsStream = Observable.fromArray(items);
        Observable<Long> timer = Observable.interval(initialDelay, interval, unit);

        return Observable.zip(itemsStream, timer, (key, val) -> key);
    }

    default  <T> Observable<T> periodicEmitter(T[] items, int interval,
                                               TimeUnit unit) {
        return periodicEmitter(items, interval, unit);
    }

    default  Flowable<String> delayedByLengthEmitter(TimeUnit unit, String...items) {
        Flowable<String> itemsStream = Flowable.fromArray(items);

        return itemsStream.concatMap(item -> Flowable.just(item)
                        .doOnNext(val -> log.info("Received {} delaying for {} ", val, val.length()))
                        .delay(item.length(), unit)
                );
    }

    default <T> Consumer<? super T> logNext() {
        return (Consumer<T>) val -> log.info("Subscriber received: {}", val);
    }

    default <T> Consumer<? super T> logNextAndSlowByMillis(int millis) {
        return (Consumer<T>) val -> {
            log.info("Subscriber received: {}", val);
            Helpers.sleepMillis(millis);
        };
    }

    default Consumer<? super Throwable> logError() {
        return err -> log.error("Subscriber received error '{}'", err.getMessage());
    }

    default Consumer<? super Throwable> logError(CountDownLatch latch) {
        return err -> {
            log.error("Subscriber received error '{}'", err.getMessage());
            latch.countDown();
        };
    }

    default Action logComplete() {
        return () -> log.info("Subscriber got Completed event");
    }

    default Action logComplete(CountDownLatch latch) {
        return () -> {
            log.info("Subscriber got Completed event");
            latch.countDown();
        };
    }

}


