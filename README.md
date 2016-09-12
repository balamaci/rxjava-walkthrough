## RxJava 1.x

### [Part01CreateObservable](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part01CreateObservable.java)

#### Simple operators to create Observables

```
Observable<Integer> observable = Observable.just(1, 5, 10);
Observable<Integer> observable = Observable.range(1, 10);
Observable<String> observable = Observable.from(new String[] {"red", "green", "blue", "black"});
```

#### From Future

```
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> { //starts a background task
            log.info("CompletableFuture work starts");  //in the ForkJoin common pool
            Helpers.sleepMillis(100);
            return "red";
        });

        Observable<String> observable = Observable.from(completableFuture);
```

#### Creating your own Observable

Using **Observable.create** to handle the actual emissions of events with the events like **onNext**, **onCompleted**, **onError**
When using Observable.create you need to be aware of [BackPressure]() and that Observables created with 'create' are not BackPressure aware

``` 
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
```
 