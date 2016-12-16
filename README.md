# RxJava 2.x

also available for [reactor-core](https://github.com/balamaci/reactor-core-playground) 

## Contents 
 
   - [Flowable, Single and Observable](#flowable)
   - [Simple Operators](#simple-operators)
   - [Merging Streams](#merging-streams)
   - [FlatMap Operator](#flatmap-operator)
   - [Schedulers](#schedulers)
   - [Error Handling](#error-handling)
   - [Backpressure](#backpressure)
   - [Articles and books](#articles)

## Reactive Streams
Reactive Streams is a programming concept for handling asynchronous 
data streams in a non-blocking manner while providing backpressure to stream publishers.
It has evolved into a [specification](https://github.com/reactive-streams/reactive-streams-jvm) that is based on the concept of **Publisher<T>** and **Subscriber<T>**.
A **Publisher** is the source of events **T** in the stream, and a **Subscriber** is the consumer for those events.
A **Subscriber** subscribes to a **Publisher** by invoking a "factory method" in the Publisher that will push
the stream items **<T>** starting a new **Subscription**:
```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```

When the Subscriber is ready to start handling events, it signals this via a **request** to that **Subscription** 
```
public interface Subscription {
    public void request(long n); //request n items
    public void cancel();
}
```

Upon receiving this signal, the Publisher begins to invoke **Subscriber::onNext(T)** for each event **T**. 
This continues until either completion of the stream (**Subscriber::onComplete()**) 
or an error occurs during processing (**Subscriber::onError(Throwable)**).
```
public interface Subscriber<T> {
    //signals to the Publisher to start sending events
    public void onSubscribe(Subscription s);     
    
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
```

## Flowable, Observable
RxJava provides more types of event publishers: 
   - **Flowable** Publisher that emits 0..N elements, and then completes successfully or with an error
   - **Observable** like Flowables but without a backpressure strategy
   
   - **Single** a specialized emitter that completes with a value successfully either an error.(doesn't have onComplete callback, instead onSuccess(val))
   - **Maybe** a specialized emitter that can complete with / without a value or complete with an error.
   - **Completable** a specialized emitter that just signals if it completed successfully or with an error.

Code is available at [Part01CreateFlowable.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part01CreateFlowable.java)

### Simple operators to create Streams
```
Flowable<Integer> flowable = Flowable.just(1, 5, 10);
Flowable<Integer> flowable = Flowable.range(1, 10);
Flowable<String> flowable = Flowable.fromArray(new String[] {"red", "green", "blue", "black"});
Flowable<String> flowable = Flowable.fromIterable(List.of("red", "green", "blue"));
```


### Flowable from Future

```
CompletableFuture<String> completableFuture = CompletableFuture
            .supplyAsync(() -> { //starts a background thread the ForkJoin common pool
                    log.info("CompletableFuture work starts");  
                    Helpers.sleepMillis(100);
                    return "red";
            });

Single<String> observable = Single.from(completableFuture);
single.subscribe(val -> log.info("Stream completed successfully : {}", val));
```


### Creating your own stream

Using **Flowable.create** to handle the actual emissions of events with the events like **onNext**, **onComplete**, **onError**

When subscribing to the Flowable with flowable.subscribe(...) the lambda code inside create() gets executed.
Flowable.subscribe(...) can take 3 handlers for each type of event - onNext, onError and onCompleted.

When using Observable.create you need to be aware of [backpressure](#backpressure) and that Observables created with 'create' are not BackPressure aware

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

### Streams are lazy 
Streams are lazy meaning that the code inside create() doesn't get executed without subscribing to the stream.
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

An easy way to switch from a blocking method to a reactive Single/Flowable is to use **.defer(() -> blockingOp())**.

Simply using **Flowable.just(blockingOp())** would still block, as Java needs to resolve the parameter when invoking
**Flux.just(param)** method, so _blockingOp()_ method would still be invoked(and block).
```
//NOT OK
Flowable<String> flowableBlocked = Flowable.just((blockingOp())); //blocks on this line
```
    
In order to get around this problem, we can use **Flowable.defer(() -> blockingOp())** and wrap the _blockingOp()_ call inside a lambda which 
will be invoked lazy **at subscribe time**.

```
Flowable<String> stream = Flowable.defer(() -> Flowable.just(blockingOperation())); 
stream.subscribe(val -> log.info("Val " + val)); //only now the code inside defer() is executed
```

### Multiple subscriptions to the same Observable / Flowable 
When subscribing to an Observable/Flowable, the create() method gets executed for each subscription this means that the events 
inside create are re-emitted to each subscriber. 

So every subscriber will get the same events and will not lose any events - this behavior is named **'cold observable'**

```
Observable<Integer> observable = Observable.create(subscriber -> {
   log.info("Started emitting");

   log.info("Emitting 1st event");
   subscriber.onNext(1);

   log.info("Emitting 2nd event");
   subscriber.onNext(2);

   subscriber.onComplete();
});

log.info("Subscribing 1st subscriber");
observable.subscribe(val -> log.info("First Subscriber received: {}", val));

log.info("=======================");

log.info("Subscribing 2nd subscriber");
observable.subscribe(val -> log.info("Second Subscriber received: {}", val));
```

will output

```
[main] - Subscribing 1st subscriber
[main] - Started emitting
[main] - Emitting 1st event
[main] - First Subscriber received: 1
[main] - Emitting 2nd event
[main] - First Subscriber received: 2
[main] - =======================
[main] - Subscribing 2nd subscriber
[main] - Started emitting
[main] - Emitting 1st event
[main] - Second Subscriber received: 1
[main] - Emitting 2nd event
[main] - Second Subscriber received: 2
```

### Observable / Flowable lifecycle
Inside the create() method, we can check is there are still active subscribers to our Flowable/Observable.
It's a way to prevent to do extra work(like for ex. querying a datasource for entries) if no one is listening
In the following example we'd expect to have an infinite stream, but because we stop if there are no active subscribers, we stop producing events.

As seen above the code inside create doesn't execute the code inside _.create(() -> {...})_ until we subscribed.   

**take(limit)** is a simple operator. It's role is to count the number of events and then unsubscribe from the source.

It's important to understand that events are passed from the Observable/Flowable to the operator chain. 

For our 
 unsubscribes from the Observable after it's received the specified amount of events.

```
Observable<Integer> observable = Observable.create(subscriber -> {

    int i = 1;
    while(true) {
        if(subscriber.isDisposed()) {
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
Code is available at [Part02SimpleOperators.java](https://github.com/balamaci/rxjava-playground/blob/master/src/test/java/com/balamaci/rx/Part02SimpleOperators.java)

### interval
Periodically emits a number starting from 0 and then increasing the value on each emission.
```
log.info("Starting");
Flowable.interval(5, TimeUnit.SECONDS)
       .take(4)
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

The delay(), interval() operators uses a [Scheduler](#schedulers) by default, which actually means it's
running the operators and the subscribe operations on a different thread and so the test method
will terminate before we see the text from the log.


### scan
Takes an **initial value** and a **function(accumulator, currentValue)**. It goes through the events
sequence and combines the current event value with the previous result(accumulator) emitting downstream the function's
result for each event(the initial value is used for the first event)

```
Flowable<Integer> numbers = 
                Flowable.just(3, 5, -2, 9)
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
Flowable<Integer> numbers = Flowable.just(3, 5, -2, 9)
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
Flowable<List<Integer>> numbers = Flowable.just(3, 5, -2, 9)
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
Single<Boolean> isUserBlockedStream = 
                    Single.fromFuture(CompletableFuture.supplyAsync(() -> {
                            Helpers.sleepMillis(200);
                            return Boolean.FALSE;
                    }));

Single<Integer> userCreditScoreStream = 
                    Single.fromFuture(CompletableFuture.supplyAsync(() -> {
                            Helpers.sleepMillis(2300);
                            return 5;
                    }));

Single<Pair<Boolean, Integer>> userCheckStream = Single.
           zip(isUserBlockedStream, userCreditScoreStream, 
                      (blocked, creditScore) -> new Pair<Boolean, Integer>(blocked, creditScore));

userCheckStream.subscribe(pair -> log.info("Received " + pair));
```
Even if the 'isUserBlockedStream' finishes after 200ms, 'userCreditScoreStream' is slow at 2.3secs, 
the 'zip' method applies the combining function(new Pair(x,y)) after it received both values and passes it 
to the subscriber.


Another good example of 'zip' is to slow down a stream by another basically **implementing a periodic emitter of events**:

```  
Flowable<String> colors = Flowable.just("red", "green", "blue");
Flowable<Long> timer = Flowable.interval(2, TimeUnit.SECONDS);

Flowable<String> periodicEmitter = Flowable.zip(colors, timer, (key, val) -> key);
```

Since the zip operator needs a pair of events, the slow stream will work like a timer by periodically emitting 
with zip setting the pace of emissions downstream every 2 seconds.

**Zip is not limited to just two streams**, it can merge 2,3,4,.. streams and wait for groups of 2,3,4 'pairs' of 
events which it combines with the zip function and sends downstream.

### merge
Merge operator combines one or more stream and passes events downstream as soon as they appear.
![merge](https://raw.githubusercontent.com/reactor/projectreactor.io/master/src/main/static/assets/img/marble/merge.png)

```
Flowable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

Flowable<Long> numbers = Flowable.interval(1, TimeUnit.SECONDS)
                .take(5);
Flowable flowable = Flowable.merge(colors, numbers);                
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
Flowable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

Flowable<Long> numbers = Flowable.interval(1, TimeUnit.SECONDS)
                .take(4);

Flowable events = Flowable.concat(colors, numbers);
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
After the map() operator encounters an error it unsubscribes(cancels the subscription) from upstream 
- therefore 'yellow' is not even emitted-. The error travels downstream and triggers the error handler in the subscriber.


There are operators to deal with error flow control:
 
### onErrorReturn

The 'onErrorReturn' operator replaces an exception with a value:

```
Flowable<Integer> numbers = Flowable.just("1", "3", "a", "4", "5", "c")
                            .map(Integer::parseInt) 
                            .onErrorReturn(0);      
subscribeWithLog(numbers);
=====
Subscriber received: 1
Subscriber received: 3
Subscriber received: 0
Subscriber got Completed event
```

Notice though how it didn't prevent map() operator from unsubscribing from the Flowable, but it did 
trigger the normal onNext callback instead of onError in the subscriber.


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

Flowable<String> colors = Flowable.just("green", "blue", "red", "white", "blue")
                .flatMap(color -> simulateRemoteOperation(color))
                .onErrorReturn(throwable -> "-blank-");
                
subscribeWithLog(colors);

============

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
Flowable<String> colors = Flowable.just("green", "blue", "red", "white", "blue")
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
Flowable<String> colors = Flowable.just("red", "blue", "green", "yellow")
       .concatMap(color -> delayedByLengthEmitter(TimeUnit.SECONDS, color) 
                             //if there are no events flowing in the timeframe   
                             .timeout(6, TimeUnit.SECONDS)  
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
   
## Backpressure

It can be the case of a slow consumer that cannot keep up with the producer that is producing too many events
that the subscriber cannot process. 

Backpressure relates to a feedback mechanism through which the subscriber can signal to the producer how much data 
it can consume and so to produce only that amount.

The [reactive-streams](https://github.com/reactive-streams/reactive-streams-jvm) section above we saw that besides the onNext, onError and onComplete handlers, the Subscriber
has an **onSubscribe(Subscription)**

```
public interface Subscriber<T> {
    //signals to the Publisher to start sending events
    public void onSubscribe(Subscription s);     
    
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
```

Subscription through which it can signal upstream it's ready to receive a number of items and after it
processes the items request another batch.

```
public interface Subscription {
    public void request(long n); //request n items
    public void cancel();
}
```
So in theory the Subscriber can prevent being overloaded by requesting an initial number of items. The Publisher would
send those items downstream and not produce any more, until the Subscriber would request more. We say in theory because
until now we did not see a custom **onSubscribe** request being implemented. This is because if not specified explicitly,
there is a default implementation which requests of Long.MAX_VALUE which basically means "send all you have".

Neither did we see the code in the producer that takes consideration of the number of items requested by the subscriber. 
```
Flowable.create(subscriber -> {
      log.info("Started emitting");

      for(int i=0; i < 300; i++) {
           if(subscriber.isCanceled()) {
              return;
           }
           log.info("Emitting {}", i);
           subscriber.next(i);
      }

      subscriber.complete();
}, BackpressureStrategy.BUFFER); //BackpressureStrategy will be explained further bellow
```
Looks like it's not possible to slow down production based on request(as there is no reference to the requested items),
we can at most stop production if the subscriber canceled subscription. 

This can be done if we extend Flowable so we can pass our custom Subscription type to the downstream subscriber:
```
private class CustomRangeFlowable extends Flowable<Integer> {

        private int startFrom;
        private int count;

        CustomRangeFlowable(int startFrom, int count) {
            this.startFrom = startFrom;
            this.count = count;
        }

        @Override
        public void subscribeActual(Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new CustomRangeSubscription(startFrom, count, subscriber));
        }

        class CustomRangeSubscription implements Subscription {

            volatile boolean cancelled;
            boolean completed = false;
            
            private int count;
            private int currentCount;
            private int startFrom;

            private Subscriber<? super Integer> actualSubscriber;

            CustomRangeSubscription(int startFrom, int count, Subscriber<? super Integer> actualSubscriber) {
                this.count = count;
                this.startFrom = startFrom;
                this.actualSubscriber = actualSubscriber;
            }

            @Override
            public void request(long items) {
                log.info("Downstream requests {} items", items);
                for(int i=0; i < items; i++) {
                    if(cancelled || completed) {
                        return;
                    }

                    if(currentCount == count) {
                        completed = true;
                        if(cancelled) {
                            return;
                        }

                        actualSubscriber.onComplete();
                        return;
                    }

                    int emitVal = startFrom + currentCount;
                    currentCount++;
                    actualSubscriber.onNext(emitVal);
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }
```   
Now lets see how we can custom control how many items we request from upstream, to simulate an initial big request, 
and then a request for other smaller batches of items as soon as the subscriber finishes and is ready for another batch.
  
```
Flowable<Integer> flowable = new CustomRangeFlowable(5, 10);

flowable.subscribe(new Subscriber<Integer>() {

       private Subscription subscription;
       private int backlogItems;

       private final int BATCH = 2;
       private final int INITIAL_REQ = 5;

       @Override
       public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                backlogItems = INITIAL_REQ;

                log.info("Initial request {}", backlogItems);
                subscription.request(backlogItems);
            }

            @Override
            public void onNext(Integer val) {
                log.info("Subscriber received {}", val);
                backlogItems --;

                if(backlogItems == 0) {
                    backlogItems = BATCH;
                    subscription.request(BATCH);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("Subscriber encountered error");
            }

            @Override
            public void onComplete() {
                log.info("Subscriber completed");
            }
        });
=====================
Initial request 5
Downstream requests 5 items
Subscriber received 5
Subscriber received 6
Subscriber received 7
Subscriber received 8
Subscriber received 9
Downstream requests 2 items
Subscriber received 10
Subscriber received 11
Downstream requests 2 items
Subscriber received 12
Subscriber received 13
Downstream requests 2 items
Subscriber received 14
Subscriber completed        
```  

Returning to the _Flowable.create()_ example since it's not taking any account of the requested 
items by the subscriber, does it mean it might overwhelm a slow Subscriber? 
```
private Flowable<Integer> createFlowable(int items,
                     BackpressureStrategy backpressureStrategy) {

return Flowable.create(subscriber -> {
        log.info("Started emitting");

        for (int i = 0; i < items; i++) {
            if(subscriber.isCancelled()) {
                 return;
            }
                
            log.info("Emitting {}", i);
            subscriber.onNext(i);
        }

        subscriber.onComplete();
}, backpressureStrategy); //can be BackpressureStrategy.DROP, BUFFER, LATEST,..
```
This is where the 2nd parameter _BackpressureStrategy_ comes in that allows you to specify what to do 
in the case.
 
   - BackpressureStrategy.BUFFER buffer in memory the events that overflow. Of course is we don't drop over some threshold, it might lead to OufOfMemory. 
   - BackpressureStrategy.DROP just drop the overflowing events
   - BackpressureStrategy.LATEST keep only recent event and discards previous unconsumed events.
   - BackpressureStrategy.ERROR we get an error in the subscriber immediately  
   - BackpressureStrategy.MISSING means we don't care about backpressure(we let one of the downstream operators
   onBackpressureXXX handle it -explained further down-)

  
Still what does it mean to 'overwhelm' the subscriber? 
It means to emit more items than requested by downstream subscriber.
But we said that by default the subscriber requests Long.MAX_VALUE since the code 
**flowable.subscribe(onNext(), onError, onComplete)** uses a default **onSubscribe**:
```
(subscription) -> subscription.request(Long.MAX_VALUE);
```

so unless we override it like in our custom Subscriber above, it means it would never overflow. But between the Publisher and the Subscriber you'd have a series of operators. 
When we subscribe, a Subscriber travels up through all operators to the original Publisher and some operators override 
the requested items upstream. One such operator is **observeOn**() which makes it's own request to the upstream Publisher(256 by default),
but can take a parameter to specify the request size.

```
Flowable<Integer> flowable = createFlowable(5, BackpressureStrategy.DROP)
                .observeOn(Schedulers.io(), false, 3);
flowable.subscribe((val) -> {
                               log.info("Subscriber received: {}", val);
                               Helpers.sleepMillis(millis);
                           }, logError(), logComplete());
======
[main] - Started emitting
[main] - Emitting 0
[main] - Emitting 1
[main] - Emitting 2
[main] - Emitting 3
[main] - Emitting 4
[RxCachedThreadScheduler-1] - Subscriber received: 0
[RxCachedThreadScheduler-1] - Subscriber received: 1
[RxCachedThreadScheduler-1] - Subscriber received: 2
[RxCachedThreadScheduler-1] - Subscriber got Completed event  
```
This is expected, as the subscription travels upstream through the operators to the source Flowable, while initially
the Subscriber requesting Long.MAX_VALUE from the upstream operator **observeOn**, which in turn subscribes to the source and it requests just 3 items from the source instead.
Since we used **BackpressureStrategy.DROP** all the items emitted outside the expected 3, get discarded and thus never reach our subscriber.

You may wonder what would have happened if we didn't use **observeOn**. We had to use it if we wanted to be able
to produce faster than the subscriber(it wasn't just to show a limited request operator), because we'd need a 
separate thread to produce events faster than the subscriber processes them.   

Also you can transform an Observable to Flowable by specifying a BackpressureStrategy, otherwise Observables 
just throw exception on overflowing(same as using BackpressureStrategy.DROP in Flowable.create()).
```
Flowable flowable = observable.toFlowable(BackpressureStrategy.DROP)
```
so can a hot Publisher be converted to a Flowable:
```
PublishSubject<Integer> subject = PublishSubject.create();

Flowable<Integer> flowable = subject
                .toFlowable(BackpressureStrategy.DROP)
```

There are also specialized operators to handle backpressure the onBackpressureXXX operators: **onBackpressureBuffer**,
**onBackpressureDrop**, **onBackpressureLatest**

These operators request Long.MAX_VALUE(unbounded amount) from upstream and then take it upon themselves to manage the 
requests from downstream. 
In the case of _onBackpressureBuffer_ it adds in an internal queue and send downstream the events as requested,
_onBackpressureDrop_ just discards events that are received from upstream more than requested from downstream, 
_onBackpressureLatest_ also drops emitted events excluding the last emitted event(most recent).  

```
Flowable<Integer> flowable = createFlowable(10, BackpressureStrategy.MISSING)
                .onBackpressureBuffer(5, () -> log.info("Buffer has overflown"));

flowable = flowable
                .observeOn(Schedulers.io(), false, 3);
subscribeWithSlowSubscriber(flowable);                

=====                
[main] - Started emitting
[main] - Emitting 0
[main] - Emitting 1
[RxCachedThreadScheduler-1] - Subscriber received: 0
[main] - Emitting 2
[main] - Emitting 3
[main] - Emitting 4
[main] - Emitting 5
[main] - Emitting 6
[main] - Emitting 7
[main] - Emitting 8
[main] - Emitting 9
[main] - Buffer has overflown
[RxCachedThreadScheduler-1] ERROR - Subscriber received error 'Buffer is full'                
```

We create the Flowable with _BackpressureStrategy.MISSING_ saying we don't care about backpressure
but let one of the onBackpressureXXX operators handle it.
Notice however 



Chaining together multiple onBackpressureXXX operators doesn't actually make sense
Using something like
```
Flowable<Integer> flowable = createFlowable(10, BackpressureStrategy.MISSING)
                 .onBackpressureBuffer(5)
                 .onBackpressureDrop((val) -> log.info("Dropping {}", val))
flowable = flowable
                .observeOn(Schedulers.io(), false, 3);
                 
subscribeWithSlowSubscriber(flowable);
```
is not behaving as maybe you'd expected - buffer 5 values, and then dropping overflowing events-.
Because _onBackpressureDrop_ subscribes to the previous _onBackpressureBuffer_ operator
signaling it's requesting **Long.MAX_VALUE**(unbounded amount) from it. 
Thus onBackpressureBuffer will never feel its subscriber is overwhelmed and never "trigger", meaning that the last 
onBackpressureXXX operator overrides the previous one if they are chained.

Of course for implementing an event dropping strategy after a full buffer, there is the special overrided
version of onBackpressureBuffer that takes a **BackpressureOverflowStrategy**.
```
Flowable<Integer> flowable = createFlowable(10, BackpressureStrategy.MISSING)
                .onBackpressureBuffer(5, () -> log.info("Buffer has overflown"),
                                            BackpressureOverflowStrategy.DROP_OLDEST);

flowable = flowable
                .observeOn(Schedulers.io(), false, 3);

subscribeWithSlowSubscriber(flowable);

===============
[main] - Started emitting
[main] - Emitting 0
[main] - Emitting 1
[RxCachedThreadScheduler-1] - Subscriber received: 0
[main] - Emitting 2
[main] - Emitting 3
[main] - Emitting 4
[main] - Emitting 5
[main] - Emitting 6
[main] - Emitting 7
[main] - Emitting 8
[main] - Buffer has overflown
[main] - Emitting 9
[main] - Buffer has overflown
[RxCachedThreadScheduler-1] - Subscriber received: 1
[RxCachedThreadScheduler-1] - Subscriber received: 2
[RxCachedThreadScheduler-1] - Subscriber received: 5
[RxCachedThreadScheduler-1] - Subscriber received: 6
[RxCachedThreadScheduler-1] - Subscriber received: 7
[RxCachedThreadScheduler-1] - Subscriber received: 8
[RxCachedThreadScheduler-1] - Subscriber received: 9
[RxCachedThreadScheduler-1] - Subscriber got Completed event
```


onBackpressureXXX operators can be added whenever necessary and it's not limited to cold publishers and we can use them 
on hot publishers also.

## Articles and books further reading
[Reactive Programming with RxJava](http://shop.oreilly.com/product/0636920042228.do)
  