package menu.ui;

import com.api.Statistic;

public class HistoryItem implements MenuItem {
    @Override
    public String getTitle() {
        return "Program Runs History";

    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;

        System.out.println("Program Runs History: ");
        for(Statistic statistic : Statistic.getStatistics()) {
            System.out.println("Run #" + statistic.getIndex() + ":");
            System.out.println("Expansion Level: " + statistic.getExpansionLevel());
            System.out.println("Input: " + statistic.getInput());
            System.out.println("Result: " + statistic.getResult());
            System.out.println("Cycles: " + statistic.getCyclesCount());
        }
    }
}
