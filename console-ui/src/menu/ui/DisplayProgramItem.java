package menu.ui;

import com.api.Api;

import java.util.Scanner;

public class DisplayProgramItem implements MenuItem{

    @Override
    public String getTitle() {
        return "Display Program";
    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;

        System.out.println("The loaded program is: ");
        System.out.println("======================");
        System.out.println(Api.getProgram(0));
    }
}
