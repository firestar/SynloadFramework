package com.synload.eventsystem;

import com.synload.framework.modules.Responder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventClass implements Serializable {
    private Handler handler;
    private String[] trigger;
    private Type type;
    private String identifier = null;
    private Responder response;
    private Map<String, Object> sessionData = new HashMap<String, Object>();

    public Handler getHandler() {
        return handler;
    }
    
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public String[] getTrigger() {
        return trigger;
    }

    public void setTrigger(String[] trigger) {
        this.trigger = trigger;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void generateIdentifier() {
        this.identifier = UUID.randomUUID().toString();
    }

    public Responder getResponse() {
        return response;
    }

    public void setResponse(Responder response) {
        this.response = response;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, Object> getSessionData() {
        return sessionData;
    }

    public void setSessionData(Map<String, Object> sessionData) {
        this.sessionData = sessionData;
    }
}