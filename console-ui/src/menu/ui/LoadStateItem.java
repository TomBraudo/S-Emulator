package menu.ui;

import com.api.Api;

public class LoadStateItem implements MenuItem {
    @Override
    public String getTitle() {
        return "Load State";
    }

    @Override
    public void onSelect() {
        System.out.print("Please enter the state file: ");
        String path = MenuUtils.SCANNER.nextLine();

        try {
            Api.loadState(path);
            System.out.println("State file loaded successfully." + path);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}
