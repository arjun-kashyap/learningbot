package org.arjunkashyap.learningbot.Entity;

public class BotRequest {
    private String input;
    private String context;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "BotRequest{" +
                "input='" + input + '\'' +
                '}';
    }
}
