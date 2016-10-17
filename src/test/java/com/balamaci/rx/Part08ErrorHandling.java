package com.balamaci.rx;

import org.junit.Test;
import rx.Observable;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Exceptions are for exceptional situations.
 * The Observable contract specifies that exceptions are terminal operations.
 * There are however operator available for error flow control
 */
public class Part08ErrorHandling implements BaseTestObservables {

    /**
     * After the map() operator encounters an error, it triggers the error handler
     * in the subscriber which also unsubscribes from the stream,
     * therefore 'yellow' is not even sent downstream.
     */
    @Test
    public void errorIsTerminalOperation() {
        Observable<String> colors = Observable.just("green", "blue", "red", "yellow")
                .map(color -> {
                    if ("red".equals(color)) {
                        throw new RuntimeException("Encountered red");
                    }
                    return color + "*";
                })
                .map(val -> val + "XXX");

        subscribeWithLog(colors);
    }


    /**
     * The 'onErrorReturn' operator doesn't prevent the unsubscription from the 'colors'
     * but it does translate the exception for the downstream operators and the final Subscriber
     * receives it in the 'onNext()' instead in 'onError()'
     */
    @Test
    public void onErrorReturn() {
        Observable<String> colors = Observable.just("green", "blue", "red", "yellow")
                .map(color -> {
                    if ("red".equals(color)) {
                        throw new RuntimeException("Encountered red");
                    }
                    return color + "*";
                })
                .onErrorReturn(th -> "-blank-")
                .map(val -> val + "XXX");

        subscribeWithLog(colors);
    }



    @Test
    public void onErrorReturnWithFlatMap() {
        //flatMap encounters an error when it subscribes to 'red' substreams and thus unsubscribe from
        // 'colors' stream and the remaining colors still are not longer emitted
        Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color))
                .onErrorReturn(throwable -> "-blank-"); //onErrorReturn just has the effect of translating

        subscribeWithLog(colors);

        log.info("*****************");

        //bellow onErrorReturn() is applied to the flatMap substream and thus translates the exception to
        //a value and so flatMap continues on with the other colors after red
        colors = Observable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color)
                                    .onErrorReturn(throwable -> "-blank-")  //onErrorReturn doesn't trigger
                        // the onError() inside flatMap so it doesn't unsubscribe from 'colors'
                );

        subscribeWithLog(colors);
    }


    /**
     * onErrorResumeNext() returns a stream instead of an exception and subscribes to that stream instead,
     * useful for example to invoke a fallback method that returns also a stream
     */
    @Test
    public void onErrorResumeNext() {
        Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color)
                        .onErrorResumeNext(th -> {
                            if (th instanceof IllegalArgumentException) {
                                return Observable.error(new RuntimeException("Fatal, wrong arguments"));
                            }
                            return fallbackRemoteOperation();
                        })
                );

        subscribeWithLog(colors);
    }

    private Observable<String> fallbackRemoteOperation() {
        return Observable.just("blank");
    }



    /**
     ************* Retry Logic ****************
     ****************************************** */

    /**
     * timeout operator raises exception when there are no events incoming before it's predecessor in the specified
     * time limit
     *
     * retry() resubscribes in case of exception to the Observable
     */
    @Test
    public void timeoutWithRetry() {
        Observable<String> colors = Observable.just("red", "blue", "green", "yellow")
                .concatMap(color ->  delayedByLengthEmitter(TimeUnit.SECONDS, color)
                                        .timeout(6, TimeUnit.SECONDS)
                                        .retry(2)
                                        .onErrorResumeNext(Observable.just("blank"))
                );

        subscribeWithLog(colors.toBlocking());
        //there is also
    }

    /**
     * When you want to retry based on the number considering the thrown exception type
     */
    @Test
    public void retryBasedOnAttemptsAndExceptionType() {
        Observable<String> colors = Observable.just("blue", "red", "black", "yellow");

        colors = colors
                .flatMap(colorName -> simulateRemoteOperation(colorName)
                            .retry((retryAttempt, exception) -> {
                                if (exception instanceof IllegalArgumentException) {
                                    log.error("{} encountered non retry exception ", colorName);
                                    return false;
                                }
                                log.info("Retry attempt {} for {}", retryAttempt, colorName);
                                return retryAttempt <= 2;
                            })
                            .onErrorResumeNext(Observable.just("generic color"))
                );

        subscribeWithLog(colors.toBlocking());
    }

    /**
     * A more complex retry logic like implementing a backoff strategy in case of exception
     * This can be obtained with retryWhen(exceptionObservable -> Observable)
     *
     * retryWhen resubscribes when an event from an Observable is emitted. It receives as parameter an exception stream
     *
     * we zip the exceptionsStream with a .range() stream to obtain the number of retries,
     * however we want to wait a little before retrying so in the zip function we return a delayed event - .timer()
     *
     * The delay also needs to be subscribed to be effected so we also need flatMap
     *
     */
    @Test
    public void retryWhenUsedForRetryWithBackoff() {
        Observable<String> colors = Observable.just("blue", "green", "red", "black", "yellow");

        colors = colors.flatMap(colorName ->
                                  simulateRemoteOperation(colorName)
                                    .retryWhen(exceptionStream -> exceptionStream
                                                .zipWith(Observable.range(1, 3), (exc, attempts) -> {
                                                    //don't retry for IllegalArgumentException
                                                    if(exc instanceof IllegalArgumentException) {
                                                        return Observable.error(exc);
                                                    }

                                                    if(attempts < 3) {
                                                        return Observable.timer(2 * attempts, TimeUnit.SECONDS);
                                                    }
                                                    return Observable.error(exc);
                                                })
                                                .flatMap(val -> val)
                                    )
                                  .onErrorResumeNext(Observable.just("generic color")
                        )
        );

        subscribeWithLog(colors.toBlocking());
    }

    /**
     * repeatWhen is identical to retryWhen only it responds to 'onCompleted' instead of 'onError'
     */
    @Test
    public void testRepeatWhen() {
        Observable<Integer> remoteOperation = Observable.defer(() -> {
                    Random random = new Random();
                    return Observable.just(random.nextInt(10));
                });

        remoteOperation = remoteOperation.repeatWhen(completed -> completed
                                                    .delay(2, TimeUnit.SECONDS)
                        ).take(10);
        subscribeWithLogWaiting(remoteOperation);
    }

    private Observable<String> simulateRemoteOperation(String color) {
        return Observable.<String>create(subscriber -> {
            if ("red".equals(color)) {
                log.info("Emitting RuntimeException for {}", color);
                throw new RuntimeException("Color red raises exception");
            }
            if ("black".equals(color)) {
                log.info("Emitting IllegalArgumentException for {}", color);
                throw new IllegalArgumentException("Black is not a color");
            }

            String value = "**" + color + "**";

            log.info("Emitting {}", value);
            subscriber.onNext(value);
            subscriber.onCompleted();
        });
    }


}
