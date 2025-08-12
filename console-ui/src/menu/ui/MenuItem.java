package menu.ui;

import com.api.Api;

public interface MenuItem {
    String getTitle();
    void onSelect();
    static boolean ensureProgramLoaded() {
        if (!Api.isLoaded()) {
            System.out.println("Load a program before choosing this option\n");
            return false; // not loaded
        }
        return true; // loaded
    }
}
