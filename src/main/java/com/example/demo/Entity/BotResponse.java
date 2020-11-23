package com.example.demo.Entity;

public class BotResponse {
    private String status;
    private long responseTime;
    private Object[] debugInfo;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public Object[] getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(Object[] debugInfo) {
        this.debugInfo = debugInfo;
    }
}
