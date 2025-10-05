package com.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistic implements Serializable {
    // User-aware registries
    static Map<String, List<Statistic>> statisticsByUser = new HashMap<>();
    private static Map<String, Integer> nextIndexByUser = new HashMap<>();
    private int index;
    private int expansionLevel;
    private String architecture;
    private List<Integer> input;
    private int result;
    private int cyclesCount;
    private List<ProgramResult.VariableToValue> variableToValue;

    public static void saveRunDetails(String userId, int expansionLevel, String architecture, List<Integer> input, int result, int cyclesCount, List<ProgramResult.VariableToValue> variableToValue) {
        Statistic statistic = new Statistic();
        int next = nextIndexByUser.getOrDefault(userId, 1);
        statistic.index = next;
        nextIndexByUser.put(userId, next + 1);
        statistic.expansionLevel = expansionLevel;
        statistic.architecture = architecture;
        statistic.input = List.copyOf(input);
        statistic.cyclesCount = cyclesCount;
        statistic.result = result;
        statistic.variableToValue = List.copyOf(variableToValue);
        statisticsByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(statistic);
    }

    public static List<Statistic> getStatistics(String userId) {
        List<Statistic> list = statisticsByUser.get(userId);
        if (list == null) return List.of();
        return Collections.unmodifiableList(list);
    }
    public static Statistic getStatistic(String userId, int index) {
        List<Statistic> list = statisticsByUser.get(userId);
        if (list == null) throw new IndexOutOfBoundsException("No statistics for user: " + userId);
        return list.get(index);
    }

    static void clearStatistics(){
        statisticsByUser.clear();
        nextIndexByUser.clear();
    }

    public int getIndex() {
        return index;
    }
    public int getExpansionLevel() {
        return expansionLevel;
    }
    public String getArchitecture() { return architecture; }
    public List<Integer> getInput() {
        return Collections.unmodifiableList( input);
    }
    public int getResult() {
        return result;
    }
    public int getCyclesCount() {
        return cyclesCount;
    }
    public static int getGlobalIndex(String userId) {return nextIndexByUser.getOrDefault(userId, 1);}
    static void setGlobalIndex(String userId, int globalIndex) {nextIndexByUser.put(userId, globalIndex);} 
    static void setStatistics(String userId, List<Statistic> statistics) {statisticsByUser.put(userId, statistics);}    
    public List<ProgramResult.VariableToValue> getVariableToValue() {
        return Collections.unmodifiableList(variableToValue);
    }
}
