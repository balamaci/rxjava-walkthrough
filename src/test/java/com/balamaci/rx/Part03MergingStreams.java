package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import javafx.util.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Operators for working with multiple streams
 *
 *
 */
public class Part03MergingStreams implements BaseTestObservables {

    /**
     * Zip operator operates sort of like a zipper in the sense that it takes an event from one stream and waits
     * for an event from another other stream. Once an event for the other stream arrives, it uses the zip function
     * to merge the two events.
     * <p>
     * This is an useful scenario when for example you want to make requests to remote services in parallel and
     * wait for both responses before continuing.
     * <p>
     * Zip operator besides the streams to zip, also takes as parameter a function which will produce the
     * combined result of the zipped streams once each stream emitted it's value
     */
    @Test
    public void zipUsedForTakingTheResultOfCombinedAsyncOperations() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<Boolean> isUserBlockedStream = Observable.from(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(200);
            return Boolean.FALSE;
        }));
        Observable<Integer> userCreditScoreStream = Observable.from(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(2300);
            return 200;
        }));

        Observable<Pair<Boolean, Integer>> userCheckStream = Observable.zip(isUserBlockedStream, userCreditScoreStream,
                (blocked, creditScore) -> new Pair<Boolean, Integer>(blocked, creditScore));
        subscribeWithLog(userCheckStream, latch);

        Helpers.wait(latch);
    }

    /**
     * Implementing a periodic emitter, by waiting for a slower stream to emit periodically.
     * Since the zip operator need a pair of events, the slow stream will work like a timer by periodically emitting
     * with zip setting the pace of emissions downstream.
     */
    @Test
    public void zipUsedToSlowDownAnotherStream() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> colors = Observable.just("red", "green", "blue");
        Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

        Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
        subscribeWithLog(periodicEmitter, latch);

        Helpers.wait(latch);
    }


    /**
     * Merge operator combines one or more stream and passes events downstream as soon
     * as they appear
     * <p>
     * The subscriber will receive both color strings and numbers from the Observable.interval
     * as soon as they are emitted
     */
    @Test
    public void mergeOperator() {
        log.info("Starting");

        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(5);

        BlockingObservable observable = Observable.merge(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }

    /**
     * Concat operator appends another streams at the end of another
     * The ex. shows that even the 'numbers' streams should start early, the 'colors' stream emits fully its events
     * before we see any 'numbers'.
     * This is because 'numbers' stream is actually subscribed only after the 'colors' complete.
     * Should the second stream be a 'hot' emitter, its events would be lost until the first one finishes
     * and the seconds stream is subscribed.
     */
    @Test
    public void concatStreams() {
        log.info("Starting");
        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(4);

        BlockingObservable observable = Observable.concat(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }

    /**
     * combineLatest pairs events from multiple streams, but instead of waiting for an event
     * from all other streams, it uses the last emitted event from that stream
     */
    @Test
    public void combineLatest() {
        CountDownLatch latch = new CountDownLatch(1);

        log.info("Starting");

        Observable<String> colors = periodicEmitter("red", "green", "blue", 3, TimeUnit.SECONDS);
        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(4);
        Observable observable = Observable.combineLatest(colors, numbers, Pair::new);
        subscribeWithLog(observable, latch);

        Helpers.wait(latch);
    }
}
