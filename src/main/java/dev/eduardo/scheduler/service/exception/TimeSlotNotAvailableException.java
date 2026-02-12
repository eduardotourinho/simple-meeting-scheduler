package dev.eduardo.scheduler.service.exception;

public class TimeSlotNotAvailableException extends RuntimeException {
    public TimeSlotNotAvailableException(String message) {
        super(message);
    }
}
