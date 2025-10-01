package robbery.booster;

/**
 * Represents a booster in the Robbery plugin.
 * <p>
 * A booster provides a temporary multiplier effect to a player. Each booster has a name,
 * multiplier value, duration in seconds, priority, and a unique tag identifier.
 * </p>
 */
public class Booster {

    /** Multiplier value of the booster (e.g., 1.5 for +50% boost). */
    private final double boost;

    /** Remaining duration of the booster in seconds. */
    private int seconds;

    /** Display name of the booster. */
    private final String name;

    /** Priority level of the booster. Higher priority boosters override lower ones. */
    private final int priority;

    /** Unique identifier tag for the booster. */
    private final String tag;

    /**
     * Constructs a new Booster with the given parameters.
     *
     * @param name display name of the booster
     * @param boost multiplier value of the booster
     * @param seconds duration of the booster in seconds
     * @param priority priority of the booster (higher priority overrides lower)
     * @param tag unique identifier for the booster
     */
    public Booster(String name, double boost, int seconds, int priority, String tag) {
        this.name = name;
        this.boost = boost;
        this.seconds = seconds;
        this.priority = priority;
        this.tag = tag;
    }

    /**
     * Constructs a copy of an existing Booster.
     *
     * @param boost the Booster to copy
     */
    public Booster(Booster boost) {
        this.boost = boost.boost;
        this.seconds = boost.seconds;
        this.name = boost.name;
        this.priority = boost.priority;
        this.tag = boost.tag;
    }

    /** @return the multiplier value of the booster. */
    public double getMultiplier() {
        return boost;
    }

    /** @return the remaining duration of the booster in seconds. */
    public int getSeconds() {
        return seconds;
    }

    /** @return the display name of the booster. */
    public String getName() {
        return name;
    }

    /** @return the priority of the booster. */
    public int getPriority() {
        return priority;
    }

    /** @return the unique tag identifier of the booster. */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the remaining duration of the booster.
     *
     * @param seconds new duration in seconds
     */
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    /**
     * Adds time to the remaining duration of the booster.
     *
     * @param seconds number of seconds to add
     */
    public void addTime(int seconds) {
        this.seconds += seconds;
    }
}
