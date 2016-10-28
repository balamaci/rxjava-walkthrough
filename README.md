# RxJava 1.x

## Contents 
 
   - [Observable](#observable)
   - [Simple Operators](#simple-operators)
   - [Merging Streams](#merging-streams)
   - [FlatMap Operator](#flatmap-operator)
   - [Schedulers](#schedulers)
   - [Error Handling](#error-handling)


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

### defer


## Simple Operators
Code is available at [Part02SimpleOperators.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part02SimpleOperators.java)

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
22:27:44 [main] - Starting
22:27:49 [main] - Tick 0
22:27:54 [main] - Tick 1
22:27:59 [main] - Tick 2
22:28:04 [main] - Tick 3
22:28:04 [main] - Completed
```

The delay operator uses a [Scheduler](#schedulers) by default, which actually means it's
running the operators and the subscribe operations on a different thread and so the test method
will terminate before we see the text from the log.

To prevent this we use the **.toBlocking()** operator which returns a **BlockingObservable**. Operators on
**BlockingObservable** block(wait) until upstream Observable is completed

### scan
Takes an initial value and a function(accumulator, currentValue). It goes through the events 
sequence and combines the current event value with the previous result(accumulator) emitting downstream the
The initial value is used for the first event

```
Observable<Integer> numbers = 
                Observable.just(3, 5, -2, 9)
                    .scan(0, (totalSoFar, currentValue) -> {
                               log.info("TotalSoFar={}, currentValue={}", totalSoFar, currentValue);
                               return totalSoFar + currentValue;
                    });
```

```
16:09:17 [main] - Subscriber received: 0
16:09:17 [main] - TotalSoFar=0, currentValue=3
16:09:17 [main] - Subscriber received: 3
16:09:17 [main] - TotalSoFar=3, currentValue=5
16:09:17 [main] - Subscriber received: 8
16:09:17 [main] - TotalSoFar=8, currentValue=-2
16:09:17 [main] - Subscriber received: 6
16:09:17 [main] - TotalSoFar=6, currentValue=9
16:09:17 [main] - Subscriber received: 15
16:09:17 [main] - Subscriber got Completed event
```

### reduce
reduce operator acts like the scan operator but it only passes downstream the final result 
(doesn't pass the intermediate results downstream) so the subscriber receives just one event

```
Observable<Integer> numbers = Observable.just(3, 5, -2, 9)
                            .reduce(0, (totalSoFar, val) -> {
                                         log.info("totalSoFar={}, emitted={}", totalSoFar, val);
                                         return totalSoFar + val;
                            });
```

```
17:08:29 [main] - totalSoFar=0, emitted=3
17:08:29 [main] - totalSoFar=3, emitted=5
17:08:29 [main] - totalSoFar=8, emitted=-2
17:08:29 [main] - totalSoFar=6, emitted=9
17:08:29 [main] - Subscriber received: 15
17:08:29 [main] - Subscriber got Completed event
```

### collect
collect operator acts similar to the _reduce_ operator, but while the _reduce_ operator uses a reduce function
which returns a value, the _collect_ operator takes a container supplier and a function which doesn't return
anything(a consumer). The mutable container is passed for every event and thus you get a chance to modify it
in this collect consumer function.

```
Observable<List<Integer>> numbers = Observable.just(3, 5, -2, 9)
                                        .collect(ArrayList::new, (container, value) -> {
                                            log.info("Adding {} to container", value);
                                            container.add(value);
                                            //notice we don't need to return anything
                                        });
```
```
17:40:18 [main] - Adding 3 to container
17:40:18 [main] - Adding 5 to container
17:40:18 [main] - Adding -2 to container
17:40:18 [main] - Adding 9 to container
17:40:18 [main] - Subscriber received: [3, 5, -2, 9]
17:40:18 [main] - Subscriber got Completed event
```

because the usecase to store to a List container is so common, there is a **.toList()** operator that is just a collector adding to a List. 


## Merging Streams
Operators for working with multiple streams
Code at [Part03MergingStreams.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part03MergingStreams.java)

### zip
Zip operator operates sort of like a zipper in the sense that it takes an event from one stream and waits
for an event from another other stream. Once an event for the other stream arrives, it uses the zip function
to merge the two events.

This is an useful scenario when for example you want to make requests to remote services in parallel and
wait for both responses before continuing. It also takes a function which will produce the combined result
of the zipped streams once each has emitted a value.

![Zip](https://raw.githubusercontent.com/reactor/projectreactor.io/master/src/main/static/assets/img/marble/zip.png)

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

Another good example of 'zip' is to slow down a stream by another basically **implementing a periodic emitter of events**:
```  
Observable<String> colors = Observable.just("red", "green", "blue");
Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
```
Since the zip operator needs a pair of events, the slow stream will work like a timer by periodically emitting 
with zip setting the pace of emissions downstream every 2 seconds.

**Zip is not limited to just two streams**, it can merge 2,3,4,.. streams and wait for groups of 2,3,4 'pairs' of events which it combines with the zip function and sends downstream.

### merge
Merge operator combines one or more stream and passes events downstream as soon as they appear.
![merge](https://raw.githubusercontent.com/reactor/projectreactor.io/master/src/main/static/assets/img/marble/merge.png)

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
![concat](https://raw.githubusercontent.com/reactor/projectreactor.io/master/src/main/static/assets/img/marble/concat.png)

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


## Schedulers
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


## Flatmap operator
The flatMap operator is so important and has so many different uses it deserves it's own category to explain it.
Code at [Part06FlatMapOperator.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part06FlatMapOperator.java)

I like to think of it as a sort of **fork-join** operation because what flatMap does is it takes individual stream items
and maps each of them to an Observable(so it creates new Streams from each object) and then 'flattens' the events from 
these Streams back as coming from a single stream.

Why this looks like fork-join because for each element you can fork some jobs that keeps emitting results,
and these results are emitted back as elements to the subscribers downstream

Rules of thumb to consider before getting comfortable with flatMap: 
   
   - When you have an 'item' and you'll get Observable&lt;X&gt;, instead of X, you need flatMap. Most common example is when you want 
   to make a remote call that returns an Observable. For ex if you have a stream of customerIds, and downstream you
    want to work with actual Customer objects:
   ```   
   Observable<Customer> getCustomer(Integer customerId) {..}
    ...
   
   Observable<Integer> customerIds = Observable.of(1,2,3,4);
   Observable<Customer> customers = customerIds
                                        .flatMap(customerId -> getCustomer(customerId));
   ```
   
   - When you have Observable&lt;Observable&lt;T&gt;&gt; you probably need flatMap.

We use a simulated remote call that might return asynchronous as many events as the length of the color string
```
    private Observable<String> simulateRemoteOperation(String color) {
        return Observable.<String>create(subscriber -> {
                    Runnable asyncRun = () -> {
                        for (int i = 0; i < color.length(); i++) {
                            subscriber.onNext(color + i);
                            Helpers.sleepMillis(200);
                        }
        
                        subscriber.onCompleted();
                    };
                    new Thread(asyncRun).start();
                });
    }
```

If we have a stream of color names:
```
Observable<String> colors = Observable.just("orange", "red", "green")
```
to invoke the remote operation: 
```
Observable<String> colors = Observable.just("orange", "red", "green")
         .flatMap(colorName -> simulateRemoteOperation(colorName));

colors.subscribe(val -> log.info("Subscriber received: {}", val));         
```
returns
```
16:44:15 [Thread-0]- Subscriber received: orange0
16:44:15 [Thread-2]- Subscriber received: green0
16:44:15 [Thread-1]- Subscriber received: red0
16:44:15 [Thread-0]- Subscriber received: orange1
16:44:15 [Thread-2]- Subscriber received: green1
16:44:15 [Thread-1]- Subscriber received: red1
16:44:15 [Thread-0]- Subscriber received: orange2
16:44:15 [Thread-2]- Subscriber received: green2
16:44:15 [Thread-1]- Subscriber received: red2
16:44:15 [Thread-0]- Subscriber received: orange3
16:44:15 [Thread-2]- Subscriber received: green3
16:44:16 [Thread-0]- Subscriber received: orange4
16:44:16 [Thread-2]- Subscriber received: green4
16:44:16 [Thread-0]- Subscriber received: orange5
```

Notice how the results are coming intertwined. This is because flatMap actually subscribes to it's inner Observables 
returned from 'simulateRemoteOperation'. You can specify the concurrency level of flatMap as a parameter. Meaning 
you can say how many of the substreams should be subscribed "concurrently" - aka before the onComplete 
event is triggered on the substreams.

By setting the concurrency to **1** we don't subscribe to other substreams until the current one finishes:
```
Observable<String> colors = Observable.just("orange", "red", "green")
                     .flatMap(val -> simulateRemoteOperation(val), 1); //

```
Notice now there is a sequence from each color before the next one appears
```
17:15:24 [Thread-0]- Subscriber received: orange0
17:15:24 [Thread-0]- Subscriber received: orange1
17:15:25 [Thread-0]- Subscriber received: orange2
17:15:25 [Thread-0]- Subscriber received: orange3
17:15:25 [Thread-0]- Subscriber received: orange4
17:15:25 [Thread-0]- Subscriber received: orange5
17:15:25 [Thread-1]- Subscriber received: red0
17:15:26 [Thread-1]- Subscriber received: red1
17:15:26 [Thread-1]- Subscriber received: red2
17:15:26 [Thread-2]- Subscriber received: green0
17:15:26 [Thread-2]- Subscriber received: green1
17:15:26 [Thread-2]- Subscriber received: green2
17:15:27 [Thread-2]- Subscriber received: green3
17:15:27 [Thread-2]- Subscriber received: green4
```

There is actually an operator which is basically this flatMap with 1 concurrency called **concatMap**.


Inside the flatMap we can operate on the substream with the same stream operators
```
Observable<Pair<String, Integer>> colorsCounted = colors
    .flatMap(colorName -> {
               Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

               return simulateRemoteOperation(colorName) // <- Still a stream
                              .zipWith(timer, (val, timerVal) -> val)
                              .count()
                              .map(counter -> new Pair<>(colorName, counter));
               }
    );
```

## Error handling
Code at [Part08ErrorHandling.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part08ErrorHandling.java)

Exceptions are for exceptional situations.
The Observable contract specifies that exceptions are terminal operations. 
That means in case an error reaches the Subscriber, after invoking the 'onError' handler, it also unsubscribes:


```
Observable<String> colors = Observable.just("green", "blue", "red", "yellow")
       .map(color -> {
              if ("red".equals(color)) {
                        throw new RuntimeException("Encountered red");
              }
              return color + "*";
       })
       .map(val -> val + "XXX");

colors.subscribe(
         val -> log.info("Subscriber received: {}", val),
         exception -> log.error("Subscriber received error '{}'", exception.getMessage()),
         () -> log.info("Subscriber completed")
);
```
returns:
```
23:30:17 [main] INFO - Subscriber received: green*XXX
23:30:17 [main] INFO - Subscriber received: blue*XXX
23:30:17 [main] ERROR - Subscriber received error 'Encountered red'
```
After the map() operator encounters an error, it triggers the error handler in the subscriber which also unsubscribes(cancels the subscription) from the stream,
therefore 'yellow' is not even sent downstream.


However there are operators to deal with error flow control. 

Let's introduce a more realcase scenario of a simulated remote request that might fail 
```
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
```


### onErrorReturn

The 'onErrorReturn' operator replaces an exception with a value:
```
Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color))
                .onErrorReturn(throwable -> "-blank-");
                
subscribeWithLog(colors);
```
returns:
```
22:15:51 [main] INFO - Emitting **green**
22:15:51 [main] INFO - Subscriber received: **green**
22:15:51 [main] INFO - Emitting **blue**
22:15:51 [main] INFO - Subscriber received: **blue**
22:15:51 [main] INFO - Emitting RuntimeException for red
22:15:51 [main] INFO - Subscriber received: -blank-
22:15:51 [main] INFO - Subscriber got Completed event
```
flatMap encounters an error when it subscribes to 'red' substreams and thus still unsubscribe from 'colors' 
stream and the remaining colors are not longer emitted


```
Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color)
                                    .onErrorReturn(throwable -> "-blank-")
                );
```
onErrorReturn() is applied to the flatMap substream and thus translates the exception to a value and so flatMap 
continues on with the other colors after red

returns:
```
22:15:51 [main] INFO - Emitting **green**
22:15:51 [main] INFO - Subscriber received: **green**
22:15:51 [main] INFO - Emitting **blue**
22:15:51 [main] INFO - Subscriber received: **blue**
22:15:51 [main] INFO - Emitting RuntimeException for red
22:15:51 [main] INFO - Subscriber received: -blank-
22:15:51 [main] INFO - Emitting **white**
22:15:51 [main] INFO - Subscriber received: **white**
22:15:51 [main] INFO - Emitting **blue**
22:15:51 [main] INFO - Subscriber received: **blue**
22:15:51 [main] INFO - Subscriber got Completed event
```

### onErrorResumeNext
onErrorResumeNext() returns a stream instead of an exception, useful for example to invoke a fallback 
method that returns also a stream

```
Observable<String> colors = Observable.just("green", "blue", "red", "white", "blue")
     .flatMap(color -> simulateRemoteOperation(color)
                        .onErrorResumeNext(th -> {
                            if (th instanceof IllegalArgumentException) {
                                return Observable.error(new RuntimeException("Fatal, wrong arguments"));
                            }
                            return fallbackRemoteOperation();
                        })
     );
...
private Observable<String> fallbackRemoteOperation() {
        return Observable.just("blank");
}
```

## Retrying

### timeout()
Timeout operator raises exception when there are no events incoming before it's predecessor in the specified time limit.

### retry()
**retry()** - resubscribes in case of exception to the Observable

```
Observable<String> colors = Observable.just("red", "blue", "green", "yellow")
       .concatMap(color -> delayedByLengthEmitter(TimeUnit.SECONDS, color) //we're delaying by string length secs
                             .timeout(6, TimeUnit.SECONDS) //if there are no events flowing in the timeframe 
                             .retry(2)
                             .onErrorResumeNext(Observable.just("blank"))
       );

subscribeWithLog(colors.toBlocking());
```

returns
```
12:40:16 [main] INFO - Received red delaying for 3 
12:40:19 [main] INFO - Subscriber received: red
12:40:19 [RxComputationScheduler-2] INFO - Received blue delaying for 4 
12:40:23 [main] INFO - Subscriber received: blue
12:40:23 [RxComputationScheduler-4] INFO - Received green delaying for 5 
12:40:28 [main] INFO - Subscriber received: green
12:40:28 [RxComputationScheduler-6] INFO - Received yellow delaying for 6 
12:40:34 [RxComputationScheduler-7] INFO - Received yellow delaying for 6 
12:40:40 [RxComputationScheduler-1] INFO - Received yellow delaying for 6 
12:40:46 [main] INFO - Subscriber received: blank
12:40:46 [main] INFO - Subscriber got Completed event
```

When you want to retry considering the thrown exception type:
```
Observable<String> colors = Observable.just("blue", "red", "black", "yellow")
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
```

```
13:21:37 [main] INFO - Emitting **blue**
13:21:37 [main] INFO - Emitting RuntimeException for red
13:21:37 [main] INFO - Retry attempt 1 for red
13:21:37 [main] INFO - Emitting RuntimeException for red
13:21:37 [main] INFO - Retry attempt 2 for red
13:21:37 [main] INFO - Emitting RuntimeException for red
13:21:37 [main] INFO - Retry attempt 3 for red
13:21:37 [main] INFO - Emitting IllegalArgumentException for black
13:21:37 [main] ERROR - black encountered non retry exception 
13:21:37 [main] INFO - Emitting **yellow**
13:21:37 [main] INFO - Subscriber received: **blue**
13:21:37 [main] INFO - Subscriber received: generic color
13:21:37 [main] INFO - Subscriber received: generic color
13:21:37 [main] INFO - Subscriber received: **yellow**
13:21:37 [main] INFO - Subscriber got Completed event
```

### retryWhen
A more complex retry logic like implementing a backoff strategy in case of exception
This can be obtained with **retryWhen**(exceptionObservable -> Observable)

retryWhen resubscribes when an event from an Observable is emitted. It receives as parameter an exception stream
     
we zip the exceptionsStream with a .range() stream to obtain the number of retries,
however we want to wait a little before retrying so in the zip function we return a delayed event - .timer()

The delay also needs to be subscribed to be effected so we also flatMap

```
Observable<String> colors = Observable.just("blue", "green", "red", "black", "yellow");

colors.flatMap(colorName -> 
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
```

```
15:20:23 [main] INFO - Emitting **blue**
15:20:23 [main] INFO - Emitting **green**
15:20:23 [main] INFO - Emitting RuntimeException for red
15:20:23 [main] INFO - Emitting IllegalArgumentException for black
15:20:23 [main] INFO - Emitting **yellow**
15:20:23 [main] INFO - Subscriber received: **blue**
15:20:23 [main] INFO - Subscriber received: **green**
15:20:23 [main] INFO - Subscriber received: generic color
15:20:23 [main] INFO - Subscriber received: **yellow**
15:20:25 [RxComputationScheduler-1] INFO - Emitting RuntimeException for red
15:20:29 [RxComputationScheduler-2] INFO - Emitting RuntimeException for red
15:20:29 [main] INFO - Subscriber received: generic color
15:20:29 [main] INFO - Subscriber got Completed event
```

**retryWhen vs repeatWhen** 
With similar names it worth noting the difference.
 
   - repeat() resubscribes when it receives onCompleted().
   - retry() resubscribes when it receives onError().
   
Example using repeatWhen() to implement periodic polling
```
remoteOperation.repeatWhen(completed -> completed
                                         .delay(2, TimeUnit.SECONDS))                                                       
```   