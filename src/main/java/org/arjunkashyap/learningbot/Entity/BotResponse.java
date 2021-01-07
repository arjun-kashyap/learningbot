package org.arjunkashyap.learningbot.Entity;

import java.util.List;
import java.util.Map;

public class BotResponse {
    private String status;
    private long responseTime;
    private Map<String, Object> debugInfo;
    private String context;

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

    public Map<String, Object> getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(Map<String, Object> debugInfo) {
        this.debugInfo = debugInfo;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
