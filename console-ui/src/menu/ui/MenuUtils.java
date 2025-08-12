package menu.ui;

import com.api.Api;

import java.util.InputMismatchException;
import java.util.Scanner;

public class MenuUtils {

    public static final Scanner SCANNER = new Scanner(System.in);

    public static boolean ensureProgramLoaded() {
        if (!Api.isLoaded()) {
            System.out.println("Load a program before choosing this option\n");
            return false; // not loaded
        }
        return true; // loaded
    }

    public static int getLevelInput(){
        int maxLevel = Api.getMaxLevel();
        int level;

        do {
            System.out.println("The max expansion level is: " + maxLevel);
            System.out.printf("Pick your desired expansion level [0-%d], or '-1' to return: ", maxLevel);

            try {
                level = SCANNER.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Input must be a number");
                SCANNER.nextLine(); // clear the invalid token from the input buffer
                level = -1;
                continue;           // restart the loop immediately
            }

            if (level == -1) return -1;

            if (level > maxLevel || level < 0) {
                System.out.println("Invalid level, please try again.");
            }
            SCANNER.nextLine();

        } while (level > maxLevel || level < 0);

        return level;
    }
}
