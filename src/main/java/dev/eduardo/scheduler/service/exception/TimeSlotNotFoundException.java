package dev.eduardo.scheduler.service.exception;

public class TimeSlotNotFoundException extends RuntimeException {
    public TimeSlotNotFoundException(String message) {
        super(message);
    }
}
