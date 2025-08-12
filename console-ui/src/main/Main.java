package main;

import menu.ui.*;

public class Main {
    public static void main(String[] args) {
        Menu mainMenu = new Menu("Main Menu", true);

        mainMenu.addItem(new LoadSProgramItem());
        mainMenu.addItem(new DisplayProgramItem());
        mainMenu.addItem(new ExpandProgramItem());
        mainMenu.addItem(new ExecuteProgramItem());
        mainMenu.addItem(new HistoryItem());

        mainMenu.run();
    }
}
