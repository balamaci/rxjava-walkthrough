package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part04HotPublishers implements BaseTestObservables {

    /**
     * We've seen that with 'cold publishers', whenever a subscriber subscribes, each subscriber will get
     * it's version of emitted values independently
     *
     * Subjects keep reference to their subscribers and allow 'multicasting' to them.
     *
     *  for (Disposable<T> s : subscribers.get()) {
     *     s.onNext(t);
     *  }

     *
     * Subjects besides being traditional Observables you can use the same operators and subscribe to them,
     * are also an Observer, meaning you can invoke subject.onNext(value) from different parts in the code,
     * which means that you publish events which the Subject will pass on to their subscribers.
     *
     * Observable.create(subscriber -> {
     *      subscriber.onNext(val);
     * })
     * .map(...)
     * .subscribe(...);
     *
     * With Subjects you can call onNext from different parts of the code:
     * Subject<Integer> subject = ReplaySubject.create()
     *                              .map(...);
     *                              .subscribe(); //
     *
     * ...
     * subject.onNext(val);
     * ...
     * subject.onNext(val2);
     *
     * ReplaySubject keeps a buffer of events that it 'replays' to each new subscriber, first he receives a batch of missed
     * and only later events in real-time.
     *
     * PublishSubject - doesn't keep a buffer but instead, meaning if another subscriber subscribes later
     */

    private int counter = 0;

    @Test
    public void replaySubject() {
        Subject<Integer> subject = ReplaySubject.createWithSize(50);

        Runnable action = () -> {
            counter ++;

            if(counter == 10) {
                subject.onComplete();
                return;
            }

            log.info("Emitted {}", counter);
            subject.onNext(counter);

        };
        startPushingEventsToSubject(action);

        Helpers.sleepMillis(1000);
        log.info("Subscribing 1st");
        subject.subscribe(val -> log.info("Subscriber1 received {}", val));

        Helpers.sleepMillis(1000);
        log.info("Subscribing 2nd");
        CountDownLatch latch = new CountDownLatch(1);
        subject.subscribe(val -> log.info("Subscriber2 received {}", val), logError(), logComplete(latch));
        Helpers.wait(latch);
    }


    @Test
    public void publishSubject() {
        Subject<Integer> subject = PublishSubject.create();

        Runnable action = () -> {
            counter ++;

            if(counter == 10) {
                subject.onComplete();
                return;
            }

            log.info("Emitted {}", counter);
            subject.onNext(counter);
        };
        startPushingEventsToSubject(action);

        Helpers.sleepMillis(1000);
        log.info("Subscribing 1st");
        subject.subscribe(val -> log.info("Subscriber1 received {}", val));

        Helpers.sleepMillis(1000);
        log.info("Subscribing 2nd");
        CountDownLatch latch = new CountDownLatch(1);
        subject.subscribe(val -> log.info("Subscriber2 received {}", val), logError(), logComplete(latch));
        Helpers.wait(latch);
    }

    private void startPushingEventsToSubject(Runnable action) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(action, 0, 500, TimeUnit.MILLISECONDS);
    }
}
