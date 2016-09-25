package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;

/**
 * RxJava provides some high level concepts for concurrent execution, like ExecutorService we're not dealing
 * with the low level constructs like creating the Threads ourselves. Instead we're using a {@see rx.Scheduler} which create
 * Workers who are responsible for scheduling and running code. By default RxJava will not introduce concurrency
 * and will run the operations on the subscription thread.
 *
 * There are two methods through which we can introduce Schedulers into our chain of operations:
 * - <b>subscribeOn allows to specify which Scheduler invokes the code contained in the lambda code for Observable.create()
 * - <b>observeOn</b> allows control to which Scheduler executes the code in the downstream operators
 *
 * RxJava provides some general use Schedulers already implemented:
 *  - Schedulers.computation() - to be used for CPU intensive tasks. A threadpool
 *  - Schedulers.io() - to be used for IO bound tasks
 *  - Schedulers.from(Executor) - custom ExecutorService
 *  - Schedulers.newThread() - always creates a new thread when a worker is needed. Since it's not thread pooled
 *  and always creates a new thread instead of reusing one, this scheduler is not very useful
 *
 * Although we said by default RxJava doesn't introduce concurrency, some operators that involve waiting like 'delay',
 * 'interval' need to run on a Scheduler, otherwise they would just block the subscribing thread.
 * By default **Schedulers.computation()** is used, but the Scheduler can be passed as a parameter.
 *
 * @author sbalamaci
 */
public class Part07Schedulers implements BaseTestObservables {

    /**
     * subscribeOn allows to specify which Scheduler invokes the code contained in the lambda code for Observable.create()
     */
    @Test
    public void testSubscribeOn() {
        log.info("Starting");

        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> observable = Observable.create(subscriber -> { //code that will execute inside the IO ThreadPool
            log.info("Starting slow network op");
            Helpers.sleepMillis(2000);

            log.info("Emitting 1st");
            subscriber.onNext(1);

            subscriber.onCompleted();
        });
        observable = observable.subscribeOn(Schedulers.io()) //Specify execution on the IO Scheduler
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                });

        subscribeWithLog(observable, latch);
        Helpers.wait(latch);
    }


    /**
     * observeOn switches the thread that is used for the subscribers downstream.
     * If we initially subscribedOn the IoScheduler we and we
     * further make another .
     */
    @Test
    public void testObserveOn() {
        log.info("Starting");

        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> observable = simpleObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                })
                .observeOn(Schedulers.newThread());

        subscribeWithLog(observable, latch);
        Helpers.wait(latch);
    }

    /**
     * Multiple calls to subscribeOn have no effect, just the first one will take effect, so we'll see the code
     * execute on an IoScheduler thread.
     */
    @Test
    public void multipleCallsToSubscribeOn() {
        log.info("Starting");

        CountDownLatch latch = new CountDownLatch(1);

        Observable<Integer> observable = simpleObservable()
                .subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.computation())
                .map(val -> {
                    int newValue = val * 2;
                    log.info("Mapping new val {}", newValue);
                    return newValue;
                });

        subscribeWithLog(observable, latch);
        Helpers.wait(latch);
    }

}
