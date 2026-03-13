package robbery.ranks;

/**
 * Manages the different ranks available in the robbery system.
 * <p>
 * Provides predefined {@link Rank} instances and a utility method
 * to retrieve a rank by its identifier string.
 */
public class RankManager {

    /** The highest rank available. */
    public static final Rank MAFIA_BOSS = new Rank("Mafia Boss", 2.0, 8, 0.6, 0.25,"rank7");

    /** Second-highest rank. */
    public static final Rank KINGPIN = new Rank("Kingpin", 1.75, 7, 0.5, 0.20,"rank6");

    /** High-level rank for experienced players. */
    public static final Rank HEISTER = new Rank("Heister", 1.5, 6, 0.4, 0.15,"rank5");

    /** Mid-level rank representing an outlaw. */
    public static final Rank OUTLAW = new Rank("Outlaw", 1.25, 5, 0.3, 0.10,"rank4");

    /** Mid-low rank representing a bandit. */
    public static final Rank BANDIT = new Rank("Bandit", 1.0, 4, 0.2, 0.075,"rank3");

    /** Low-level rank representing a basic robber. */
    public static final Rank ROBBER = new Rank("Robber", 0.75, 3, 0.15, 0.05,"rank2");

    /** Entry-level rank representing a burglar. */
    public static final Rank BURGLAR = new Rank("Burglar", 0.5, 2, 0.1, 0.025,"rank1");

    /** Default empty rank for players without a rank. */
    public static final Rank NONE = new Rank("", 0.0, 0, 0, 0,"rank0");

    /**
     * Returns the {@link Rank} corresponding to the given identifier string.
     *
     * @param rank the rank identifier (e.g., "rank1", "rank2", ..., "rank7")
     * @return the corresponding {@link Rank} object, or {@link #NONE} if the identifier is invalid
     */
    public static Rank getRank(String rank) {
        return switch (rank) {
            case "rank1" -> BURGLAR;
            case "rank2" -> ROBBER;
            case "rank3" -> BANDIT;
            case "rank4" -> OUTLAW;
            case "rank5" -> HEISTER;
            case "rank6" -> KINGPIN;
            case "rank7" -> MAFIA_BOSS;
            default -> NONE;
        };
    }
}
