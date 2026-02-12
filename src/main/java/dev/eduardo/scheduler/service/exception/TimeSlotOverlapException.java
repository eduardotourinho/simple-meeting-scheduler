package dev.eduardo.scheduler.service.exception;

public class TimeSlotOverlapException extends RuntimeException {
    public TimeSlotOverlapException(String message) {
        super(message);
    }
}
