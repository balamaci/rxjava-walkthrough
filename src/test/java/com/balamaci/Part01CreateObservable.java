package com.balamaci;

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
    public void createSimpleObservable() {
        Observable<Integer> observable = simpleObservable();

        observable.subscribe(
                val -> log.info("Subscriber received: {}"),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }


}
