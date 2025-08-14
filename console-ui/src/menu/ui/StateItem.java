package menu.ui;

public class StateItem implements MenuItem {
    Menu stateMenu;
    public StateItem() {
        stateMenu = new Menu("State Menu", false);
        stateMenu.addItem(new ListStateFilesItem());
        stateMenu.addItem(new SaveStateItem());
        stateMenu.addItem(new LoadStateItem());
    }

    @Override
    public String getTitle() {
        return "State Management";
    }

    @Override
    public void onSelect() {
        stateMenu.run();
    }
}
