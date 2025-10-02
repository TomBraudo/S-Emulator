package menu.ui;

import com.api.Api;

import java.io.IOException;

public class SaveStateItem implements MenuItem {
    @Override
    public String getTitle() {
        return "Save State";
    }

    @Override
    public void onSelect() {
        if(!MenuUtils.ensureProgramLoaded()) return;
        System.out.print("Please enter the folder path you wish to save the state to: ");
        String path = MenuUtils.SCANNER.nextLine();
        try {
            Api.saveState(path);
            System.out.println("Saved state to: " + path);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
