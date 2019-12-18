package de.botschmot.exceptions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Exception that gets fired when a not ready object is expected
 */
public class NotReadyException extends RuntimeException {

    /**
     * Time the faulty called object will be ready
     */
    private final LocalDateTime time;

    /**
     * Creates the Exception with a nice message that is understandable
     *
     * @param time Time the faulty called object will be ready
     */
    public NotReadyException(LocalDateTime time) {
        super(String.format("Your object is not ready yet. It will be ready in %ss (%s)", ChronoUnit.SECONDS.between(LocalDateTime.now(), time), time));
        this.time = time;
    }

    /**
     * Get the time the object that was called faulty will be released
     *
     * @return Time the faulty called object will be ready
     */
    public LocalDateTime readyTime() {
        return this.time;
    }

}
