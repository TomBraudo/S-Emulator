package menu.ui;

import com.api.Api;

import java.util.Scanner;

public class LoadSProgramItem implements MenuItem {

    @Override
    public String getTitle() {
        return "Load S Program from XML file";
    }

    @Override
    public void onSelect() {
        System.out.print("Enter path to S program XML file: ");
        String path = MenuUtils.SCANNER.nextLine();
        if(path == null || path.isBlank() || !path.toLowerCase().endsWith(".xml")){
            System.out.println("Invalid path");
            return;
        }
        try {
            Api.loadSProgram(path);
            System.out.println("S Program loaded successfully");
        }
        catch (Exception e) {
            System.out.println("Failed to load S program from XML file: " + e.getMessage());
        }

    }
}
