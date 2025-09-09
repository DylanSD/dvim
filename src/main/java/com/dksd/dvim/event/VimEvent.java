package com.dksd.dvim.event;

public class VimEvent {
    private final EventType eventType;
    private final int bufNo;

    public VimEvent(int bufNo, EventType eventType) {
        this.bufNo = bufNo;
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getBufNo() {
        return bufNo;
    }

    @Override
    public String toString() {
        return "VimEvent{" +
                "eventType=" + eventType +
                ", bufNo=" + bufNo +
                '}';
    }
}
