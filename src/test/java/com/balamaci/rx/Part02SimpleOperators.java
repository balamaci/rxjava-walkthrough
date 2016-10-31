package com.balamaci.rx;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part02SimpleOperators implements BaseTestObservables {

    private static final Logger log = LoggerFactory.getLogger(Part02SimpleOperators.class);

    /**
     * Delay operator - the Thread.sleep of the reactive world, it's pausing for a particular increment of time
     * before emitting the whole range events which are thus shifted by the specified time amount.
     *
     * The delay operator uses a Scheduler {@see Part07Schedulers} by default, which actually means it's
     * running the operators and the subscribe operations on a different thread, which means the test method
     * will terminate before we see the text from the log.
     *
     * To prevent this we use the .toBlocking() operator which returns a BlockingObservable. Operators on
     * BlockingObservable block(wait) until upstream Observable is completed
     */
    @Test
    public void delayOperator() {
        Flowable.range(0, 5)
                .delay(5, TimeUnit.SECONDS)
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));

//        Helpers.sleepMillis(10000);
    }

    /**
     * Timer operator waits for a specific amount of time before it emits an event and then completes
     */
    @Test
    public void timerOperator() {
        log.info("Starting");
        Flowable observable = Flowable.timer(5, TimeUnit.SECONDS);
        subscribeWithLogWaiting(observable);
    }


    @Test
    public void delayOperatorWithVariableDelay() {
        Flowable.range(0, 5)
                .delay(val -> Flowable.timer(val * 2, TimeUnit.SECONDS))
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }

    /**
     * Periodically emits a number starting from 0 and then increasing the value on each emission
     */
    @Test
    public void intervalOperator() {
        log.info("Starting");
        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> log.info("Completed"));
    }

    /**
     * scan operator - takes an initial value and a function(accumulator, currentValue). It goes through the events
     * sequence and combines the current event value with the previous result(accumulator) emitting downstream the
     * The initial value is used for the first event
     *
     */
    @Test
    public void scanOperator() {
        Flowable<Integer> numbers = Flowable.just(3, 5, -2, 9)
                .scan(0, (totalSoFar, currentValue) -> {
                    log.info("totalSoFar={}, emitted={}", totalSoFar, currentValue);
                    return totalSoFar + currentValue;
                });

        subscribeWithLog(numbers);
    }

    /**
     * reduce operator acts like the scan operator but it only passes downstream the final result
     * (doesn't pass the intermediate results downstream) so the subscriber receives just one event
     */
    @Test
    public void reduceOperator() {
        Single<Integer> numbers = Flowable.just(3, 5, -2, 9)
                .reduce(0, (totalSoFar, val) -> {
                    log.info("totalSoFar={}, emitted={}", totalSoFar, val);
                    return totalSoFar + val;
                });
        subscribeWithLog(numbers);
    }

    /**
     * collect operator acts similar to the reduce() operator, but while the reduce() operator uses a reduce function
     * which returns a value, the collect() operator takes a container supplie and a function which doesn't return
     * anything(a consumer). The mutable container is passed for every event and thus you get a chance to modify it
     * in this collect consumer function
     */
    @Test
    public void collectOperator() {
        Single<List<Integer>> numbers = Flowable.just(3, 5, -2, 9)
                .collect(ArrayList::new, (container, value) -> {
                    log.info("Adding {} to container", value);
                    container.add(value);
                    //notice we don't need to return anything
                });
        subscribeWithLog(numbers);
    }

    /**
     * repeat resubscribes to the observable after it receives onComplete
     */
    @Test
    public void repeat() {
        Flowable random = Flowable.defer(() -> {
                                Random rand = new Random();
                                return Flowable.just(rand.nextInt(20));
                            })
                            .repeat(5);

        subscribeWithLogWaiting(random);
    }

}
