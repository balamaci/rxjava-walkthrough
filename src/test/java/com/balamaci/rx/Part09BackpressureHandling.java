package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Backpressure is related to preventing overloading the subscriber with too many events.
 * It can be the case of a slow consumer that cannot keep up.
 * Backpressure relates to a feedback mechanism through which the subscriber can signal
 * to the producer how much data it can consume.
 * However the producer must be 'backpressure-aware' in order to know how to throttle back.
 *
 * @author sbalamaci
 */
public class Part09BackpressureHandling implements BaseTestObservables {

    private static final Logger log = LoggerFactory.getLogger(Part09BackpressureHandling.class);

    /**
     * Not being backpressure aware
     */
    @Test
    public void throwingBackpressureNotSupported() {
        Observable<Integer> observable = observableWithoutBackpressureSupport();

        observable = observable
                .observeOn(Schedulers.io());
        subscribeWithSlowSubscriberAndWait(observable);
    }

    /**
     * Not only a slow subscriber triggers backpressure, but also a slow operator
     */
    @Test
    public void throwingBackpressureNotSupportedSlowOperator() {
        Observable<Integer> observable = observableWithoutBackpressureSupport();

        observable = observable
                .observeOn(Schedulers.io())
                .map(val -> {
                    Helpers.sleepMillis(50);
                    return val * 2;
                });

        subscribeWithLog(observable);
    }

    /**
     * Subjects are also not backpressure aware
     */
    @Test
    public void throwingBackpressureNotSupportedSubject() {
        CountDownLatch latch = new CountDownLatch(1);

        PublishSubject<Integer> subject = PublishSubject.create();

        Observable<Integer> observable = subject
                .observeOn(Schedulers.io());
        subscribeWithSlowSubscriber(observable, latch);

        for (int i = 0; i < 200; i++) {
            log.info("Emitting {}", i);
            subject.onNext(i);
        }

        Helpers.wait(latch);
    }

    /**
     * Zipping a slow stream with a faster one also can cause a backpressure problem
     */
    @Test
    public void zipOperatorHasALimit() {
        Observable<Integer> fast = observableWithoutBackpressureSupport();
        Observable<Long> slowStream = Observable.interval(100, TimeUnit.MILLISECONDS);

        Observable<String> observable = Observable.zip(fast, slowStream,
                (val1, val2) -> val1 + " " + val2);

        subscribeWithSlowSubscriberAndWait(observable);
    }

    @Test
    public void backpressureAwareObservable() {
        Observable<Integer> observable = Observable.range(0, 200);

        observable = observable
                .observeOn(Schedulers.io());

        subscribeWithSlowSubscriberAndWait(observable);
    }

    // Handling
    //========================================================

    @Test
    public void dropOverflowingEvents() {
        Observable<Integer> observable = observableWithoutBackpressureSupport();

        observable = observable
                .onBackpressureDrop(val -> log.info("Dropped {}", val))
                .observeOn(Schedulers.io());
        subscribeWithSlowSubscriberAndWait(observable);
    }


    private Observable<Integer> observableWithoutBackpressureSupport() {
        return Observable.create(subscriber -> {
            log.info("Started emitting");

            for (int i = 0; i < 200; i++) {
                log.info("Emitting {}", i);
                subscriber.onNext(i);
            }

            subscriber.onCompleted();
        });
    }


    private void subscribeWithSlowSubscriberAndWait(Observable observable) {
        CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(val -> {
                    log.info("Got {}", val);
                    Helpers.sleepMillis(50);
                },
                err -> {
                    log.error("Subscriber got error", err);
                    latch.countDown();
                },
                () -> {
                    log.info("Completed");
                    latch.countDown();
                });

        Helpers.wait(latch);
    }

    private void subscribeWithSlowSubscriber(Observable observable, CountDownLatch latch) {
        observable.subscribe(val -> {
                    log.info("Got {}", val);
                    Helpers.sleepMillis(50);
                },
                err -> {
                    log.error("Subscriber got error", (Throwable) err);
                    latch.countDown();
                },
                () -> {
                    log.info("Completed");
                    latch.countDown();
                });
    }

}
