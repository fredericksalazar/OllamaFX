package com.org.ollamafx;

import com.org.ollamafx.manager.OllamaManager;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {

        System.out.println("Starting OllamaFX ...");
        OllamaManager ollamaManager = OllamaManager.getInstance();

        if(ollamaManager.isOllamaInstalled()){
            System.out.println("Ollama is installed locally");
            System.out.println("Ollama Version is : " + ollamaManager.getOllamaVersion());
        }else{
            System.out.println("Ollama isnt installed locally");
        }
    }
}
