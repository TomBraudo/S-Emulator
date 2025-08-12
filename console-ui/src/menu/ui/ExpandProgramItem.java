package menu.ui;

import com.api.Api;

import java.util.Scanner;

public class ExpandProgramItem implements MenuItem {
    @Override
    public String getTitle() {
        return "Expand Program";
    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;

        int level = MenuUtils.getLevelInput();
        if(level == -1) return;


        System.out.println("The Expanded Program:");
        System.out.println("================");
        System.out.println(Api.getProgram(level));
    }
}
