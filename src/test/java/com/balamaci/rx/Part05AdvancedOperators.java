package com.balamaci.rx;

import javafx.util.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;
import rx.observables.GroupedObservable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part05AdvancedOperators implements BaseTestObservables {

    @Test
    public void buffer() {
        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS);

        BlockingObservable<List<Long>> delayedNumbersWindow = numbers
                .buffer(5).toBlocking();

        subscribeWithLog(delayedNumbersWindow);
    }

    @Test
    public void simpleWindow() {
        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS);

        BlockingObservable<Long> delayedNumbersWindow = numbers
                .window(5)
                .flatMap(window -> window.doOnCompleted(() -> log.info("Window completed")))
                .toBlocking();

        subscribeWithLog(delayedNumbersWindow);
    }


    @Test
    public void window() {
        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS);

        BlockingObservable<Long> delayedNumbersWindow = numbers
                .window(10, 5, TimeUnit.SECONDS)
                .flatMap(window -> window.doOnCompleted(() -> log.info("Window completed")))
                .toBlocking();

        subscribeWithLog(delayedNumbersWindow);
    }

    @Test
    public void groupBy() {
        Observable<String> colors = Observable.from(new String[]{"red", "green", "blue",
                "red", "yellow", "green", "green"});

        Observable<GroupedObservable<String, String>> groupedColorsStream = colors
                .groupBy(val -> val);

        Observable<Pair<String, Integer>> colorCountStream = groupedColorsStream
                .flatMap(groupedColor -> groupedColor
                                            .count()
                                            .map(count -> new Pair<>(groupedColor.getKey(), count))
                );

        subscribeWithLog(colorCountStream.toBlocking());
    }

    @Test
    public void bufferWithLimitTriggeredByObservable() {
        Observable<String> colors = Observable.from(new String[]{"red", "green", "blue",
                "red", "yellow", "#", "green", "green"});


        colors.publish(p -> p.filter(val -> ! val.equals("#"))
                             .buffer(() -> p.filter(val -> val.equals("#")))
                )
                .toBlocking()
                .subscribe(list -> {
            String listCommaSeparated = String.join(",", list);
            log.info("List {}", listCommaSeparated);
        });
    }
}
