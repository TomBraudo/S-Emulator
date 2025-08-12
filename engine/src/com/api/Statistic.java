package com.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Statistic {
    static List<Statistic> statistics = new ArrayList<>();
    private static int globalIndex = 1;
    private int index;
    private int expansionLevel;
    private List<Integer> input;
    private int result;
    private int cyclesCount;

    public static void saveRunDetails(int expansionLevel, List<Integer> input, int result, int cyclesCount) {
        Statistic statistic = new Statistic();
        statistic.index = globalIndex++;
        statistic.expansionLevel = expansionLevel;
        statistic.input = List.copyOf(input);
        statistic.cyclesCount = cyclesCount;
        statistic.result = result;
        statistics.add(statistic);
    }

    public static List<Statistic> getStatistics() {
        return Collections.unmodifiableList(statistics);
    }

    public int getIndex() {
        return index;
    }
    public int getExpansionLevel() {
        return expansionLevel;
    }
    public List<Integer> getInput() {
        return Collections.unmodifiableList(input);
    }
    public int getResult() {
        return result;
    }
    public int getCyclesCount() {
        return cyclesCount;
    }
}
