package com.balamaci.rx;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sbalamaci
 */
public class Part04HotPublishers implements BaseTestObservables {

    /**
     * We've seen that with 'cold publishers', whenever a subscriber, when you have two, each subscriber will get
     * it's version of emitted values independently
     *
     * Subjects keep reference to their subscribers and allow 'multicasting' to them.
     *
     *  for (Disposable<T> s : subscribers.get()) {
     *     s.onNext(t);
     *  }

     *
     * Subjects besides being traditional Observables, are also an Observer, meaning you can invoke onNext(value)
     * on them which means that you can which the Subject will pass on to their subscribers.
     *
     * ReplaySubject keeps a buffer of events that it 'replays' to each subscriber
     * PublishSubject - doesn't keep a buffer but instead, meaning if another subscriber subscribes later
     */

    private AtomicInteger counter;

    @Test
    public void replaySubject() {
//        Subject subject = ReplaySubject.create();
        Subject<Integer> subject = ReplaySubject.create();

        Runnable action = () -> {
            subject.onNext(counter.incrementAndGet());

            if(counter.get() == 10) {
                subject.onComplete();
            }
        };

        startPushingEventsToSubject(action);

        subscribeWithLogWaiting(subject);
    }


    @Test
    public void publishSubject() {
        Subject subject = PublishSubject.create();

        subscribeWithLog(subject);
    }

    private void startPushingEventsToSubject(Runnable action) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(action, 0, 500, TimeUnit.MILLISECONDS);
    }
}
