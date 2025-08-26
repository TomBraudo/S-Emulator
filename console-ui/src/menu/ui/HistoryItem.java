package menu.ui;

import com.api.Statistic;

import java.util.List;

public class HistoryItem implements MenuItem {
    @Override
    public String getTitle() {
        return "Program Runs History";

    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;

        System.out.println("Program Runs History: ");
        List<Statistic> statistics = Statistic.getStatistics();
        if(statistics.isEmpty()){
            System.out.println("No runs made for the loaded program yet");
            return;
        }

        for(Statistic statistic : statistics) {
            System.out.println("Run #" + statistic.getIndex() + ":");
            System.out.println("Expansion Level: " + statistic.getExpansionLevel());
            System.out.println("Input: " + statistic.getInput());
            System.out.println("Result: " + statistic.getResult());
            System.out.println("Cycles: " + statistic.getCyclesCount() + "\n");
        }
    }
}
