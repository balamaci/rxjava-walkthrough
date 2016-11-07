package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;

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
        subject.onComplete();

        Helpers.wait(latch);
    }

    /**
     * Zipping a slow stream with a faster one also can cause a backpressure problem
     */
    @Test
    public void zipOperatorHasALimit() {
        Observable<Integer> fast = observableWithoutBackpressureSupport();
        Flowable<Long> slowStream = Flowable.interval(100, TimeUnit.MILLISECONDS);

        Observable<String> observable = Observable.zip(fast, slowStream.toObservable(),
                (val1, val2) -> val1 + " " + val2);

        subscribeWithSlowSubscriberAndWait(observable);
    }

    @Test
    public void backpressureAwareObservable() {
        Flowable<Integer> flowable = Flowable.range(0, 200);

        flowable = flowable
                .observeOn(Schedulers.io());

        subscribeWithSlowSubscriberAndWait(flowable);
    }

    // Handling
    //========================================================

    @Test
    public void dropOverflowingEvents() {
        Observable<Integer> observable = observableWithoutBackpressureSupport();

        Flowable<Integer> flowable = observable
                .toFlowable(BackpressureStrategy.DROP)
                .observeOn(Schedulers.io());
        subscribeWithSlowSubscriberAndWait(flowable);
    }


    private Observable<Integer> observableWithoutBackpressureSupport() {
        return Observable.create(subscriber -> {
            log.info("Started emitting");

            for (int i = 0; i < 200; i++) {
                log.info("Emitting {}", i);
                subscriber.onNext(i);
            }

            subscriber.onComplete();
        });
    }


    private <T> void subscribeWithSlowSubscriberAndWait(Observable<T> observable) {
        CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(slowObserver(latch));

        Helpers.wait(latch);
    }

    private <T> void subscribeWithSlowSubscriberAndWait(Flowable<T> flowable) {
        CountDownLatch latch = new CountDownLatch(1);

        flowable.subscribe(val -> {
                    log.info("Got {}", val);
                    Helpers.sleepMillis(100);
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

    private <T> void subscribeWithSlowSubscriber(Observable<T> observable, CountDownLatch latch) {
        observable.subscribe(slowObserver(latch));
    }

    private <T> Observer<T> slowObserver(CountDownLatch latch) {
        return new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Object value) {
                log.info("Got {}", value);
                Helpers.sleepMillis(100);
            }

            @Override
            public void onError(Throwable err) {
                log.error("Subscriber got error", err);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                log.info("Completed");
                latch.countDown();
            }
        };
    }


}
