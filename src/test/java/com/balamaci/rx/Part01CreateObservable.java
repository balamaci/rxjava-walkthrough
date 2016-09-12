package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

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
    public void fromFuture() {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> { //starts a background task
            log.info("CompletableFuture work starts");  //in the ForkJoin common pool
            Helpers.sleepMillis(100);
            return "red";
        });

        Observable<String> observable = Observable.from(completableFuture);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void fromArray() {
        Observable<String> observable = Observable.from(new String[] {"red", "green", "blue", "black"});

        observable.subscribe(
                val -> log.info("Subscriber received: {}"));
    }

    /**
     * Using Observable.create to handle the actual emissions of events with the events like onNext, onCompleted
     *
     * When using Observable.create you need to be aware of BackPressure and that Observables created with 'create'
     * are not BackPressure aware
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

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }

    /**
     * Event if we sleep for a long time inside create method(to simulate a costly operation,
     * without subscribing to this Observable the code is not executed and the method returns immediately.
     */
    @Test
    public void observablesAreLazy() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting but sleeping for 5 secs");
            Helpers.sleepMillis(5000);
            subscriber.onNext(1);
        });
    }

    /**
     * Inside the create() method, we can check is there are still active subscribers to our Observable.
     * It's a way to stop emissions
     */
    @Test
    public void showUnsubscribeObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {

            int i = 1;
            while(true) {
                if(! subscriber.isUnsubscribed()) {
                    subscriber.onNext(i++);
                } else {
                    break;
                }
            }
            //subscriber.onCompleted(); to late to emit Complete event since subscriber already unsubscribed
        });

        observable
                .take(5)
                .subscribe(
                        val -> log.info("Subscriber received: {}", val),
                        err -> log.error("Subscriber received error", err),
                        () -> log.info("Subscriber got Completed event") //The Complete event is triggered by 'take()' operator
        );
    }



}
