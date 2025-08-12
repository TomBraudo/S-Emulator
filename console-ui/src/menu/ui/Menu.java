package menu.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Menu {
    private final List<MenuItem> items = new ArrayList<>();
    private final boolean isRoot;
    private final String title; // ★ Custom menu title

    public Menu(String title, boolean isRoot) {
        this.title = title;
        this.isRoot = isRoot;
    }

    public void addItem(MenuItem item) {
        items.add(item);
    }

    public void display() {
        System.out.println("\n=== " + title + " ==="); // ★ Use menu's title
        for (int i = 0; i < items.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, items.get(i).getTitle());
        }
        if (isRoot) {
            System.out.println("0. Exit Program");
        } else {
            System.out.println("0. Back");
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            display();
            System.out.print("Select option: ");
            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
                continue;
            }
            int choice = scanner.nextInt();

            if (choice == 0) {
                if (isRoot) {
                    System.out.println("Exiting program...");
                    System.exit(0);
                } else {
                    System.out.println("Returning to previous menu...");
                    return; // back to parent
                }
            }

            if (choice > 0 && choice <= items.size()) {
                items.get(choice - 1).onSelect();
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }
}
