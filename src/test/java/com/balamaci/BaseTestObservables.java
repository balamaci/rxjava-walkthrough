package com.balamaci;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.concurrent.CountDownLatch;

/**
 * @author sbalamaci
 */
public interface BaseTestObservables {

    Logger log = LoggerFactory.getLogger(BaseTestObservables.class);

    default Observable<Integer> simpleObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            log.info("Emitting 2nd");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        return observable;
    }

    default Action1<Throwable> logError() {
        return err -> log.error("Subscriber received error", err);
    }

    default Action1<Throwable> logError(CountDownLatch latch) {
        return err -> {
            log.error("Subscriber received error", err);
            latch.countDown();
        };
    }

    default Action0 logComplete() {
        return () -> log.info("Subscriber got Completed event");
    }

    default Action0 logComplete(CountDownLatch latch) {
        return () -> {
            log.info("Subscriber got Completed event");
            latch.countDown();
        };
    }

}


