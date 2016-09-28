package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;

/**
 * Errors are for exception situations. The Observable contract
 *
 */
public class Part08ErrorHandling implements BaseTestObservables {

    @Test
    public void errorIsTerminalOperation() {
        Observable<String> colors = Observable.just("green", "blue", "red", "white")
                .map(color -> {
                    if("red".equals(color)) {
                        throw new RuntimeException("Encountered red");
                    }
                    return color + color.length();
                });
        subscribeWithLog(colors);
    }

    @Test
    public void onErrorReturn() {
        Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
                .map(color -> {
                    if("red".equals(color)) {
                        throw new RuntimeException("Encountered red");
                    }
                    return color + color.length();
                })
                .onErrorReturn(throwable -> "black");
        subscribeWithLog(colors);
    }

    private Observable<String> simulateRemoteOperation(String color) {
        return Observable.<String>create(subscriber -> {
            if("red".equals(color)) {
                throw new RuntimeException("Color red raises exception");
            }

            for(int i=0; i < color.length(); i++) {
                subscriber.onNext(color + i);
                Helpers.sleepMillis(200);
            }

            subscriber.onCompleted();
        });
//                .subscribeOn(Schedulers.io());
    }


}
