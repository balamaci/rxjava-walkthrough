package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import com.balamaci.rx.util.Pair;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
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
        Single<Boolean> isUserBlockedStream = Single.fromFuture(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(200);
            return Boolean.FALSE;
        }));
        Single<Integer> userCreditScoreStream = Single.fromFuture(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(2300);
            return 200;
        }));

        Single<Pair<Boolean, Integer>> userCheckStream = Single.zip(isUserBlockedStream, userCreditScoreStream,
                Pair::new);
        subscribeWithLogOutputWaitingForComplete(userCheckStream);
    }

    /**
     * Implementing a periodic emitter, by waiting for a slower stream to emit periodically.
     * Since the zip operator need a pair of events, the slow stream will work like a timer by periodically emitting
     * with zip setting the pace of emissions downstream.
     */
    @Test
    public void zipUsedToSlowDownAnotherStream() {
        Flowable<String> colors = Flowable.just("red", "green", "blue");
        Flowable<Long> timer = Flowable.interval(2, TimeUnit.SECONDS);

        Flowable<String> periodicEmitter = Flowable.zip(colors, timer, (key, val) -> key);

        subscribeWithLogOutputWaitingForComplete(periodicEmitter);
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

        Flowable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Flowable<Long> numbers = Flowable.interval(1, TimeUnit.SECONDS)
                .take(5);

        Flowable flowable = Flowable.merge(colors, numbers);
        subscribeWithLogOutputWaitingForComplete(flowable);
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
        Flowable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Flowable<Long> numbers = Flowable.interval(1, TimeUnit.SECONDS)
                .take(4);

        Flowable observable = Flowable.concat(colors, numbers);
        subscribeWithLogOutputWaitingForComplete(observable);
    }

    /**
     * combineLatest pairs events from multiple streams, but instead of waiting for an event
     * from other streams, it uses the last emitted event from that stream
     */
    @Test
    public void combineLatest() {
        log.info("Starting");

        Flowable<String> colors = periodicEmitter("red", "green", "blue", 3, TimeUnit.SECONDS);
        Flowable<Long> numbers = Flowable.interval(1, TimeUnit.SECONDS)
                .take(4);
        Flowable combinedFlowables = Flowable.combineLatest(colors, numbers, Pair::new);

        subscribeWithLogOutputWaitingForComplete(combinedFlowables);
    }
}
