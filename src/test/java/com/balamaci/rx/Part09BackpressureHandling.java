package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Backpressure is related to preventing overloading the subscriber with too many events.
 * It can be the case of a slow consumer that cannot keep up with the producer.
 * Backpressure relates to a feedback mechanism through which the subscriber can signal
 * to the producer how much data it can consume.
 *
 * However the producer must be 'backpressure-aware' in order to know how to throttle back.
 *
 * If the producer is not 'backpressure-aware', in order to prevent an OutOfMemory due to an unbounded increase of events,
 * we still can define a BackpressureStrategy to specify how we should deal with piling events.
 * If we should buffer(BackpressureStrategy.BUFFER) or drop(BackpressureStrategy.DROP, BackpressureStrategy.LATEST)
 * incoming events.
 *
 * @author sbalamaci
 */
public class Part09BackpressureHandling implements BaseTestObservables {

    @Test
    public void customBackpressureAwareFlux() {
        Flowable<Integer> flux = new CustomRangeFlowable(5, 10);

        flux.subscribe(new Subscriber<Integer>() {

            private Subscription subscription;
            private int backlogItems;

            private final int BATCH = 2;
            private final int INITIAL_REQ = 5;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                backlogItems = INITIAL_REQ;

                log.info("Initial request {}", backlogItems);
                subscription.request(backlogItems);
            }

            @Override
            public void onNext(Integer val) {
                log.info("Subscriber received {}", val);
                backlogItems--;

                if (backlogItems == 0) {
                    backlogItems = BATCH;
                    subscription.request(BATCH);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("Subscriber encountered error");
            }

            @Override
            public void onComplete() {
                log.info("Subscriber completed");
            }
        });
    }



    /**
     * We specify a buffering strategy, however since the buffer is not very large, we
     * can opt to drop overflowing events.
     */
    @Test
    public void bufferingThenDroppingEvents() {
        Flowable<Integer> flowable = observableWithoutBackpressureSupport(200)
                .toFlowable(BackpressureStrategy.ERROR)
                .onBackpressureBuffer(30)
                .onBackpressureDrop(val -> log.info("Dropped {}", val));

        //we need to switch threads to not run the producer in the same thread as the subscriber(which waits some time
        // to simulate a slow subscriber)
        flowable = flowable
                .observeOn(Schedulers.io());

        subscribeWithSlowSubscriberAndWait(flowable);
    }

    /**
     * A more complicated
     */
    @Test
    public void throwingBackpressureNotSupportedSlowOperator() {
        Observable<Integer> observable = observableWithoutBackpressureSupport(200);

        Flowable<String> flowable = observable
                .observeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(30, () -> log.info("***************Overflowing"),
                        BackpressureOverflowStrategy.DROP_OLDEST)
                .onBackpressureDrop(val -> log.info("Dropped {}", val))
                .map(val -> {
//                    Helpers.sleepMillis(1000);
                    return "*" + val + "*";
                });

        subscribeWithLogWaiting(flowable);
    }

    /**
     * Subjects are also not backpressure aware
     */
    @Test
    public void throwingBackpressureNotSupportedSubject() {
        CountDownLatch latch = new CountDownLatch(1);

        PublishSubject<Integer> subject = PublishSubject.create();

        Flowable<Integer> flowable = subject
                .toFlowable(BackpressureStrategy.BUFFER)
                .onBackpressureBuffer(50)
                .onBackpressureDrop(val -> log.info("Dropped {}", val));

        flowable = flowable.observeOn(Schedulers.io());

        subscribeWithSlowSubscriber(flowable, latch);

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
        Observable<Integer> fast = observableWithoutBackpressureSupport(200);
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
        Observable<Integer> observable = observableWithoutBackpressureSupport(200);

        Flowable<Integer> flowable = observable
                .toFlowable(BackpressureStrategy.DROP)
                .observeOn(Schedulers.io());
        subscribeWithSlowSubscriberAndWait(flowable);
    }


    private Observable<Integer> observableWithoutBackpressureSupport(int items) {
        return Observable.create(subscriber -> {
            log.info("Started emitting");

            for (int i = 0; i < items; i++) {
                log.info("Emitting {}", i);
                subscriber.onNext(i);
            }

            subscriber.onComplete();
        });
    }


    private <T> void subscribeWithSlowSubscriberAndWait(Observable<T> observable) {
        CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(logNextAndSlowByMillis(50), logError(), logComplete());

        Helpers.wait(latch);
    }

    private <T> void subscribeWithSlowSubscriberAndWait(Flowable<T> flowable) {
        CountDownLatch latch = new CountDownLatch(1);
        flowable.subscribe(logNextAndSlowByMillis(50), logError(latch), logComplete(latch));

        Helpers.wait(latch);
    }

    private <T> void subscribeWithSlowSubscriber(Flowable<T> flowable, CountDownLatch latch) {
        flowable.subscribe(logNextAndSlowByMillis(50), logError(latch), logComplete(latch));
    }

    private class CustomRangeFlowable extends Flowable<Integer> {

        private int startFrom;
        private int count;

        CustomRangeFlowable(int startFrom, int count) {
            this.startFrom = startFrom;
            this.count = count;
        }

        @Override
        public void subscribeActual(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new CustomRangeSubscription(startFrom, count, subscriber));
        }

        class CustomRangeSubscription implements Subscription {

            volatile boolean cancelled;
            boolean completed = false;
            private int count;
            private int currentCount;
            private int startFrom;

            private Subscriber<? super Integer> actualSubscriber;

            CustomRangeSubscription(int startFrom, int count, Subscriber<? super Integer> actualSubscriber) {
                this.count = count;
                this.startFrom = startFrom;
                this.actualSubscriber = actualSubscriber;
            }

            @Override
            public void request(long items) {
                log.info("Downstream requests {} items", items);
                for(int i=0; i < items; i++) {
                    if(cancelled || completed) {
                        return;
                    }

                    if(currentCount == count) {
                        completed = true;
                        if(cancelled) {
                            return;
                        }

                        actualSubscriber.onComplete();
                        return;
                    }

                    int emitVal = startFrom + currentCount;
                    currentCount++;
                    actualSubscriber.onNext(emitVal);
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }


}
