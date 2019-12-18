package de.botschmot;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Object that wraps another object and "releses" it after a given amount of time.
 *
 * @param <T> Type of Object you want to wrap
 */
public class DelayObject<T> {

    /**
     * Potential scheduler to call the callbacks
     */
    private static ScheduledExecutorService scheduler = null;
    /**
     * Content that got wrapped
     */
    private final T content;
    /**
     * Time it should be released
     */
    private final LocalDateTime time;

    /**
     * Private constructor so users cant use it
     *
     * @param content Content that should be wrapped
     * @param time    Time it should be released
     */
    private DelayObject(T content, LocalDateTime time) {
        this.content = content;
        this.time = time;
    }

    /**
     * Wrapps an object to be released at a later time
     *
     * @param content Content that should be wrapped
     * @param delay   Nanoseconds that should pass till the release
     * @param <T>     Type of object to be wrapped
     * @return new {@link DelayObject} that wrapps content
     */
    public static <T> DelayObject<T> delayObject(T content, long delay) {
        return new DelayObject<T>(content, LocalDateTime.now().plusNanos(delay));
    }

    /**
     * Wrapps an object to be released at a later time
     *
     * @param content Content that should be wrapped
     * @param time    Time it should be released
     * @param <T>     Type of object to be wrapped
     * @return new {@link DelayObject} that wrapps content
     */
    public static <T> DelayObject<T> delayObject(T content, LocalDateTime time) {
        return new DelayObject<T>(content, time);
    }

    /**
     * Returns the time the object is ready
     *
     * @return Time the object is ready
     */
    public LocalDateTime ReadyAt() {
        return this.time;
    }

    /**
     * Returns the wrapped object if it is ready. Uses {@link DelayObject#isReady()}
     *
     * @return Wrapped object if it is ready
     * @throws NotReadyException Thrown if object is not ready
     */
    public T get() throws NotReadyException {
        if (isReady())
            return this.content;
        throw new NotReadyException(this.ReadyAt());
    }

    /**
     * Determines if the object is ready to be released
     *
     * @return {@link Boolean#TRUE} if object is ready else {@link Boolean#FALSE}
     */
    public boolean isReady() {
        return ReadyAt().isBefore(LocalDateTime.now());
    }

    /**
     * Returns the wrapped object if it is ready. Uses {@link DelayObject#isReady()}. Does not throw an exception in contrast to {@link DelayObject#get()}
     *
     * @param defaultObject Object to be returned if not Ready
     * @param <K>           Type of default object
     * @return Wrapped object if it is ready
     */
    public <K extends T> T get(K defaultObject) {
        return isReady() ? this.content : defaultObject;
    }

    /**
     * Adds a callback function that triggers when the object gets ready. Accuracy is determined by the accuracy of the {@link ScheduledExecutorService}.
     * This will call {@link DelayObject#registerCallback(Runnable, ScheduledExecutorService)} with a static SingleThreadExecutor that is shared between all {@link DelayObject} so there wont be too many threads.
     *
     * @param callback Callback to be executed upon object ready
     */
    public void registerCallback(Runnable callback) {
        if (DelayObject.scheduler == null)
            DelayObject.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.registerCallback(callback, DelayObject.scheduler);
    }

    /**
     * Adds a callback function to the given {@link ScheduledExecutorService} that triggers when the object gets ready. Accuracy is determined by the accuracy of the {@link ScheduledExecutorService}.
     *
     * @param callback  Callback to be executed upon object ready
     * @param scheduler Executor that should execute the callback
     */
    public void registerCallback(Runnable callback, ScheduledExecutorService scheduler) {
        scheduler.schedule(callback, ChronoUnit.MILLIS.between(LocalDateTime.now(), this.time), TimeUnit.MILLISECONDS);
    }

    /**
     * Exception that gets fired when a not ready object is expected
     */
    public static class NotReadyException extends RuntimeException {

        /**
         * Time the faulty called object will be ready
         */
        private final LocalDateTime time;

        /**
         * Creates the Exception with a nice message that is understandable
         *
         * @param time Time the faulty called object will be ready
         */
        private NotReadyException(LocalDateTime time) {
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

}
