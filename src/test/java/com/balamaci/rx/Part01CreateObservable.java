package com.balamaci.rx;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * @author sbalamaci
 */
public class Part01CreateObservable implements BaseTestObservables {

    private static final Logger log = LoggerFactory.getLogger(Part01CreateObservable.class);


    @Test
    public void just() {
        Observable<Integer> observable = Observable.just(1,5,10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void range() {
        Observable<Integer> observable = Observable.range(1,10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}"));
    }

    @Test
    public void createSimpleObservable() {
        Observable<Integer> observable = simpleObservable();

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }

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

            subscriber.onCompleted();
        });

        observable
                .take(5)
                .subscribe(val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }

}
