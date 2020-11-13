package de.botschmot;

import de.botschmot.exceptions.NotReadyException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Object that wraps another object and "releases" it after a given amount of time.
 *
 * @param <T> Type of Object you want to wrap
 */
public final class DelayObject<T> {

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
     * Wraps an object to be released at a later time
     *
     * @param content Content that should be wrapped
     * @param delay   Nanoseconds that should pass till the release
     * @param <T>     Type of object to be wrapped
     * @return new {@link DelayObject} that wraps content
     */
    public static <T> DelayObject<T> of(T content, long delay) {
        return new DelayObject<>(content, LocalDateTime.now().plusNanos(delay));
    }

    /**
     * Wraps an object to be released at a later time
     *
     * @param content Content that should be wrapped
     * @param amount  Amount of {@see TemporalUnit} that should be added
     * @param unit    Unit that defines the amount
     * @param <T>     Type of object to be wrapped
     * @return new {@link DelayObject} that wraps content
     */
    public static <T> DelayObject<T> of(T content, long amount, TemporalUnit unit) {
        return new DelayObject<>(content, LocalDateTime.now().plus(amount, unit));
    }

    /**
     * Wraps an object to be released at a later time
     *
     * @param content Content that should be wrapped
     * @param time    Time it should be released
     * @param <T>     Type of object to be wrapped
     * @return new {@link DelayObject} that wraps content
     */
    public static <T> DelayObject<T> of(T content, LocalDateTime time) {
        return new DelayObject<>(content, time);
    }

    /**
     * Returns the time the object is ready
     *
     * @return Time the object is ready
     */
    public LocalDateTime readyAt() {
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
        throw new NotReadyException(this.readyAt());
    }

    /**
     * Determines if the content is ready to be released
     *
     * @return {@link Boolean#TRUE} if object is ready else {@link Boolean#FALSE}
     */
    public boolean isReady() {
        return readyAt().isBefore(LocalDateTime.now());
    }

    /**
     * If the content is ready, performs the given action with the content,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if the content is ready
     * @throws NullPointerException if content is ready and the given action is
     *                              {@code null}
     */
    public void ifReady(Consumer<? super T> action) {
        if (isReady()) action.accept(get());
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise performs the given empty-based action.
     *
     * @param action      the action to be performed, if the content is ready
     * @param emptyAction the empty-based action to be performed, if the content is not ready
     * @throws NullPointerException if content is ready and the given action is
     *                              {@code null}, or the content is not ready and the given empty-based
     *                              action is {@code null}.
     */
    public void ifReadyOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isReady()) {
            action.accept(get());
        } else {
            emptyAction.run();
        }
    }

    /**
     * If content is ready, returns the value, otherwise returns
     * {@code other}.
     *
     * @param other the value to be returned, if the content is not ready.
     *              May be {@code null}.
     * @return the value, if ready, otherwise {@code other}
     */
    public T orElse(T other) {
        return isReady() ? this.content : other;
    }

    /**
     * If content is ready, returns the value, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if ready, otherwise the result produced by the
     * supplying function
     * @throws NullPointerException if content is ready and the supplying
     *                              function is {@code null}
     */
    public T orElseGet(Supplier<? extends T> supplier) {
        return isReady() ? get() : supplier.get();
    }

    /**
     * If the content is ready, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *                          exception to be thrown
     * @return the value, if ready
     * @throws X                    if content is not ready
     * @throws NullPointerException if content is not ready and the exception
     *                              supplying function is {@code null}
     * @apiNote A method reference to the exception constructor with an empty argument
     * list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isReady()) return get();
        throw exceptionSupplier.get();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelayObject<?> that = (DelayObject<?>) o;
        return Objects.equals(content, that.content) &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, time);
    }

    @Override
    public String toString() {
        return String.format("Content: [%s] with ready time [%s]", content, time);
    }
}
