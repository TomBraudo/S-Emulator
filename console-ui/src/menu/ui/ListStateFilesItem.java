package menu.ui;

import com.api.Api;

import java.util.List;

public class ListStateFilesItem implements MenuItem {

    @Override
    public String getTitle() {
        return "List State Files";
    }

    @Override
    public void onSelect() {
        System.out.print("Please enter the path to the folder with the state files: ");
        String path = MenuUtils.SCANNER.nextLine();
        try {
            List<String> files = Api.getStateFileNames(path);
            System.out.println("Available state files in this folder: ");
            for(String file : files) {
                System.out.println(file);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
