package com.dksd.dvim.event;

public class VimEvent {
    private final EventType eventType;
    private final String viewName;
    private final int bufNo;
    private final String value;

    public VimEvent(String viewName, int bufNo, EventType eventType, String value) {
        this.viewName = viewName;
        this.bufNo = bufNo;
        this.eventType = eventType;
        this.value = value;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getBufNo() {
        return bufNo;
    }

    public String getValue() {
        return value;
    }

    public String getViewName() {
        return viewName;
    }

    @Override
    public String toString() {
        return "VimEvent{" +
                "eventType=" + eventType +
                ", bufNo=" + bufNo +
                ", value=" + value +
                '}';
    }
}
