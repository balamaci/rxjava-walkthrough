# RxJava 1.x

## Contents 
 
   - [Observable](#observable)
   - [Simple Operators](#simple-operators)
   - [Merging Streams](#merging-streams)
   - [Schedulers](#schedulers)


## Observable
Code is available at [Part01CreateObservable.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part01CreateObservable.java)

### Simple operators to create Observables

```
Observable<Integer> observable = Observable.just(1, 5, 10);
Observable<Integer> observable = Observable.range(1, 10);
Observable<String> observable = Observable.from(new String[] {"red", "green", "blue", "black"});
```

### Observable from Future

```
CompletableFuture<String> completableFuture = CompletableFuture
            .supplyAsync(() -> { //starts a background thread the ForkJoin common pool
                    log.info("CompletableFuture work starts");  
                    Helpers.sleepMillis(100);
                    return "red";
            });

Observable<String> observable = Observable.from(completableFuture);
```

### Creating your own Observable

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

### Observables are lazy 
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

### Multiple subscriptions to the same Observable 
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

### Checking if there are any active subscribers 
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
    .take(5) //unsubscribes after the 5th event
    .subscribe(val -> log.info("Subscriber received: {}", val),
               err -> log.error("Subscriber received error", err),
               () -> log.info("Subscriber got Completed event") //The Complete event 
               //is triggered by 'take()' operator

```

## Simple Operators

### interval
Periodically emits a number starting from 0 and then increasing the value on each emission.
```
log.info("Starting");
Observable.interval(5, TimeUnit.SECONDS)
       .take(4)
       .toBlocking()
       .subscribe(tick -> log.info("Tick {}", tick),
                  (ex) -> log.info("Error emitted"),
                  () -> log.info("Completed"));
//results
22:27:44 [main] INFO Part02SimpleOperators - Starting
22:27:49 [main] INFO Part02SimpleOperators - Tick 0
22:27:54 [main] INFO Part02SimpleOperators - Tick 1
22:27:59 [main] INFO Part02SimpleOperators - Tick 2
22:28:04 [main] INFO Part02SimpleOperators - Tick 3
22:28:04 [main] INFO Part02SimpleOperators - Completed
```

The delay operator uses a [Scheduler](#schedulers) by default, which actually means it's
running the operators and the subscribe operations on a different thread and so the test method
will terminate before we see the text from the log.

To prevent this we use the **.toBlocking()** operator which returns a **BlockingObservable**. Operators on
**BlockingObservable** block(wait) until upstream Observable is completed

## Merging Streams
Operators for working with multiple streams

### zip
Zip operator operates sort of like a zipper in the sense that it takes an event from one stream and waits
for an event from another other stream. Once an event for the other stream arrives, it uses the zip function
to merge the two events.

This is an useful scenario when for example you want to make requests to remote services in parallel and
wait for both responses before continuing. It also takes a function which will produce the combined result
of the zipped streams once each has emitted a value.

Zip operator besides the streams to zip, also takes as parameter a function which will produce the 
combined result of the zipped streams once each stream emitted it's value

```
Observable<Boolean> isUserBlockedStream = Observable.
                                            from(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(200);
            return Boolean.FALSE;
}));

Observable<Integer> userCreditScoreStream = Observable.
                                            from(CompletableFuture.supplyAsync(() -> {
            Helpers.sleepMillis(2300);
            return 5;
}));

Observable<Pair<Boolean, Integer>> userCheckStream = Observable.
           zip(isUserBlockedStream, userCreditScoreStream, 
                      (blocked, creditScore) -> new Pair<Boolean, Integer>(blocked, creditScore));

userCheckStream.subscribe(pair -> log.info("Received " + pair));
```
Even if the 'isUserBlockedStream' finishes after 200ms, 'userCreditScoreStream' is slow at 2.3secs, 
the 'zip' method applies the combining function(new Pair(x,y)) after it received both values and passes it 
to the subscriber.

Another good example of 'zip' is to slow down basically implementing a periodic emitter of events:
```  
Observable<String> colors = Observable.just("red", "green", "blue");
Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
```
Since the zip operator need a pair of events, the slow stream will work like a timer by periodically emitting 
with zip setting the pace of emissions downstream every 2 seconds.

Zip is also not limited to just two streams, it can merge 2,3,4,.. streams and group the 2,3,4 'pairs' of events.

### merge
Merge operator combines one or more stream and passes events downstream as soon as they appear.

```
Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(5);
```

```
21:32:15 - Subscriber received: 0
21:32:16 - Subscriber received: red
21:32:16 - Subscriber received: 1
21:32:17 - Subscriber received: 2
21:32:18 - Subscriber received: green
21:32:18 - Subscriber received: 3
21:32:19 - Subscriber received: 4
21:32:20 - Subscriber received: blue
```

### concat
Concat operator appends another streams at the end of another

```
Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(4);

Observable events = Observable.concat(colors, numbers);
```

```
22:48:23 - Subscriber received: red
22:48:25 - Subscriber received: green
22:48:27 - Subscriber received: blue
22:48:28 - Subscriber received: 0
22:48:29 - Subscriber received: 1
22:48:30 - Subscriber received: 2
22:48:31 - Subscriber received: 3
```

Even if the 'numbers' streams should start early, the 'colors' stream emits fully its events
before we see any 'numbers'.
This is because 'numbers' stream is actually subscribed only after the 'colors' complete.
Should the second stream be a 'hot' emitter, its events would be lost until the first one finishes
and the seconds stream is subscribed.


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


### Flatmap operator
The flatMap operator is so important and has so many different uses it deserves it's own category to explain it.

I like to think of it as a sort of **fork-join** operation because what flatMap does is it takes individual stream items
and maps each of them to an Observable(so it creates new Streams from each object) and then 'flattens' the events from 
these Streams back into a single Stream.

Why this looks like fork-join because for each element you can fork some jobs that keeps emitting results,
and these results are emitted back as elements to the subscribers downstream
 
**RuleOfThumb 1**: when you have an 'item' and you need Observable<X> you need flatMap