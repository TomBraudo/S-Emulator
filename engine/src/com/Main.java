package com;

import com.api.Api;
import com.program.Program;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Api.loadSProgram("C:\\Users\\Tom\\OneDrive\\Desktop\\EX 1\\error-1.xml");

        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Stack Trace:" + Arrays.toString(e.getStackTrace()));
        }



    }
}
