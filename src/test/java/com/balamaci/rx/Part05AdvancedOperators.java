package com.balamaci.rx;

import javafx.util.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;
import rx.observables.GroupedObservable;

import java.util.concurrent.TimeUnit;

/**
 * @author sbalamaci
 */
public class Part05AdvancedOperators implements BaseTestObservables {

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
        Observable<String> numbers = Observable.from(new String[] { "red", "green", "blue",
                "red", "yellow", "green", "green"});

        Observable<GroupedObservable<String, String>> groupedColorsStream = numbers
                .groupBy(val -> val);

        Observable<Pair<String, Integer>> colorCountStream = groupedColorsStream
                .flatMap(groupedColor -> groupedColor
                            .count()
                            .map(count -> new Pair<>(groupedColor.getKey(), count)));

        subscribeWithLog(colorCountStream.toBlocking());
    }
}
