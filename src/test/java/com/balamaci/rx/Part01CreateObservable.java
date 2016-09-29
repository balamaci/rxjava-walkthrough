package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import java.util.concurrent.CompletableFuture;

/**
 * @author sbalamaci
 */
public class Part01CreateObservable implements BaseTestObservables {

    private static final Logger log = LoggerFactory.getLogger(Part01CreateObservable.class);


    @Test
    public void just() {
        Observable<Integer> observable = Observable.just(1, 5, 10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void range() {
        Observable<Integer> observable = Observable.range(1, 10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void fromArray() {
        Observable<String> observable = Observable.from(new String[]{"red", "green", "blue", "black"});

        observable.subscribe(
                val -> log.info("Subscriber received: {}"));
    }

    /**
     * We can also create an Observable from Future, making easier to switch from legacy code to reactive
     */
    @Test
    public void fromFuture() {
        CompletableFuture<String> completableFuture = CompletableFuture.
                supplyAsync(() -> { //starts a background thread the ForkJoin common pool
                      Helpers.sleepMillis(100);
                      return "red";
                });

        Observable<String> observable = Observable.from(completableFuture);
        observable.subscribe(val -> log.info("Subscriber received: {}", val));


        completableFuture = CompletableFuture.completedFuture("green");
        observable = Observable.from(completableFuture);
        observable.subscribe(val -> log.info("Subscriber2 received: {}", val));
    }


    /**
     * Using Observable.create to handle the actual emissions of events with the events like onNext, onCompleted, onError
     * <p>
     * When subscribing to the Observable with observable.subscribe() the lambda code inside create() gets executed.
     * Observable.subscribe can take 3 handlers for each type of event - onNext, onError and onCompleted
     * <p>
     * When using Observable.create you need to be aware of <b>Backpressure</b> and that Observables based on 'create' method
     * are not Backpressure aware {@see Part07BackpressureHandling}.
     */
    @Test
    public void createSimpleObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            log.info("Emitting 2nd");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        log.info("Subscribing");
        Subscription subscription = observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event"));
    }


    /**
     * Observable emits an Error event which is a terminal operation and the subscriber is no longer executing
     * it's onNext callback. We're actually breaking the the Observable contract that we're still emitting events
     * after onComplete or onError have fired.
     */
    @Test
    public void createSimpleObservableThatEmitsError() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            subscriber.onError(new RuntimeException("Test exception"));

            log.info("Emitting 2nd");
            subscriber.onNext(2);
        });

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }

    /**
     * Observables are lazy meaning that the code inside create() doesn't get executed without subscribing to the Observable
     * So event if we sleep for a long time inside create() method(to simulate a costly operation),
     * without subscribing to this Observable the code is not executed and the method returns immediately.
     */
    @Test
    public void observablesAreLazy() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting but sleeping for 5 secs"); //this is not executed
            Helpers.sleepMillis(5000);
            subscriber.onNext(1);
        });
        log.info("Finished");
    }

    /**
     * When subscribing to an Observable, the create() method gets executed for each subscription
     * this means that the events inside create are re-emitted to each subscriber. So every subscriber will get the
     * same events and will not lose any events.
     */
    @Test
    public void multipleSubscriptionsToSameObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st event");
            subscriber.onNext(1);

            log.info("Emitting 2nd event");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        log.info("Subscribing 1st subscriber");
        observable.subscribe(val -> log.info("First Subscriber received: {}", val));

        log.info("=======================");

        log.info("Subscribing 2nd subscriber");
        observable.subscribe(val -> log.info("Second Subscriber received: {}", val));
    }

    /**
     * Inside the create() method, we can check is there are still active subscribers to our Observable.
     * It's a way to prevent to do extra work(like for ex. querying a datasource for entries) if no one is listening
     * In the following example we'd expect to have an infinite stream, but because we stop if there are no active
     * subscribers we stop producing events.
     * The **take()** operator unsubscribes from the Observable after it's received the specified amount of events
     */
    @Test
    public void showUnsubscribeObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {

            int i = 1;
            while(true) {
                if(subscriber.isUnsubscribed()) {
                    break;
                }

                subscriber.onNext(i++);
            }
            //subscriber.onCompleted(); too late to emit Complete event since subscriber already unsubscribed
        });

        observable
                .take(5)
                .subscribe(
                        val -> log.info("Subscriber received: {}", val),
                        err -> log.error("Subscriber received error", err),
                        () -> log.info("Subscriber got Completed event") //The Complete event is triggered by 'take()' operator
        );
    }


    /**
     * defer acts as a factory of Observables, just when subscribed it actually invokes the logic
     * to create the Observable to be emitted
     */
    @Test
    public void deferCreateObservable() {
        log.info("Starting");
        Observable<Long> observable = Observable.defer(() -> {
            log.info("Computing");
            long value = System.currentTimeMillis();
            return Observable.just(value);
        });

        log.info("Sleeping");
        Helpers.sleepMillis(2000);

        subscribeWithLog(observable);
    }

}
