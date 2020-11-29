package org.arjunkashyap.learningbot.Entity;

public class BotRequest {
    private String input;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return "BotRequest{" +
                "input='" + input + '\'' +
                '}';
    }
}
