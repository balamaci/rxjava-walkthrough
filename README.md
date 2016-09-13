# RxJava 1.x

## Contents 
 
   - [Observable](#observable)
   - [Schedulers](#schedulers)


### Observable
Code is available at [Part01CreateObservable.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part01CreateObservable.java)

#### Simple operators to create Observables

```
Observable<Integer> observable = Observable.just(1, 5, 10);
Observable<Integer> observable = Observable.range(1, 10);
Observable<String> observable = Observable.from(new String[] {"red", "green", "blue", "black"});
```

#### Observable from Future

```
CompletableFuture<String> completableFuture = CompletableFuture
            .supplyAsync(() -> { //starts a background thread the ForkJoin common pool
                    log.info("CompletableFuture work starts");  
                    Helpers.sleepMillis(100);
                    return "red";
            });

Observable<String> observable = Observable.from(completableFuture);
```

#### Creating your own Observable

Using **Observable.create** to handle the actual emissions of events with the events like **onNext**, **onCompleted**, **onError**

When subscribing to the Observable with observable.subscribe(...) the lambda code inside create() gets executed.
Observable.subscribe(...) can take 3 handlers for each type of event - onNext, onError and onCompleted.

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

#### Observables are lazy 
Observables are lazy meaning that the code inside create() doesn't get executed without subscribing to the Observable.
So event if we sleep for a long time inside create() method(to simulate a costly operation),
without subscribing to this Observable the code is not executed and the method returns immediately.

```
public void observablesAreLazy() {
    Observable<Integer> observable = Observable.create(subscriber -> {
        log.info("Started emitting but sleeping for 5 secs"); //this is not executed
        Helpers.sleepMillis(5000);
        subscriber.onNext(1);
    });
    log.info("Finished"); 
}
```

#### Multiple subscriptions to the same Observable 
When subscribing to an Observable, the create() method gets executed for each subscription this means that the events 
inside create are re-emitted to each subscriber. 

So every subscriber will get the same events and will not lose any events - this behavior is named **'cold observable'**

```
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
```

will output

```
[main] INFO Part01CreateObservable - Subscribing 1st subscriber
[main] INFO Part01CreateObservable - Started emitting
[main] INFO Part01CreateObservable - Emitting 1st event
[main] INFO Part01CreateObservable - First Subscriber received: 1
[main] INFO Part01CreateObservable - Emitting 2nd event
[main] INFO Part01CreateObservable - First Subscriber received: 2
[main] INFO Part01CreateObservable - =======================
[main] INFO Part01CreateObservable - Subscribing 2nd subscriber
[main] INFO Part01CreateObservable - Started emitting
[main] INFO Part01CreateObservable - Emitting 1st event
[main] INFO Part01CreateObservable - Second Subscriber received: 1
[main] INFO Part01CreateObservable - Emitting 2nd event
[main] INFO Part01CreateObservable - Second Subscriber received: 2
```

#### Checking if there are any active subscribers 
Inside the create() method, we can check is there are still active subscribers to our Observable.
It's a way to prevent to do extra work(like for ex. querying a datasource for entries) if no one is listening
In the following example we'd expect to have an infinite stream, but because we stop if there are no active subscribers we stop producing events.
The **take()** operator unsubscribes from the Observable after it's received the specified amount of events.

```
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
    .subscribe(val -> log.info("Subscriber received: {}", val),
               err -> log.error("Subscriber received error", err),
               () -> log.info("Subscriber got Completed event") //The Complete event 
               //is triggered by 'take()' operator

```

### Schedulers
RxJava provides some high level concepts for concurrent execution, like ExecutorService we're not dealing
with the low level constructs like creating the Threads ourselves. Instead we're using a **Scheduler** which create
Workers who are responsible for scheduling and running code. By default RxJava will not introduce concurrency 
and will run the operations on the subscription thread.

There are two methods through which we can introduce Schedulers into our chain of operations:

   - **subscribeOn** allows to specify which Scheduler invokes the code contained in the lambda code for Observable.create()
   - **observeOn** allows control to which Scheduler executes the code in the downstream operators

RxJava provides some general use Schedulers:
 
  - **Schedulers.computation()** - to be used for CPU intensive tasks. A threadpool. Should not be used for tasks involving blocking IO.
  - **Schedulers.io()** - to be used for IO bound tasks  
  - **Schedulers.from(Executor)** - custom ExecutorService
  - **Schedulers.newThread()** - always creates a new thread when a worker is needed. Since it's not thread pooled and 
  always creates a new thread instead of reusing one, this scheduler is not very useful 
 
Although we said by default RxJava doesn't introduce concurrency, lots of operators involve waiting like **delay**,
**interval**, **zip** need to run on a Scheduler, otherwise they would just block the subscribing thread. 
By default **Schedulers.computation()** is used, but the Scheduler can be passed as a parameter.
