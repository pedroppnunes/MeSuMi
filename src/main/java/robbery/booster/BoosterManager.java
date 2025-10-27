package robbery.booster;

import robbery.player.PlayerData;
import java.util.Random;

/**
 * Manages creation, retrieval, and random generation of boosters in the Robbery plugin.
 * <p>
 * Provides static methods to get boosters by name, randomly generate boosters based on a
 * multiplier, and calculate chance-based boosters for players considering their bonuses.
 * </p>
 */
public class BoosterManager {
    private static final Random random = new Random();

    private static final Booster BOOST0 = new Booster("None", 1.0, 0, 0, "null");
    private static final Booster BOOST1_1 = new Booster("1.25x Money", 1.25, 5 * 60, 1, "boost1x1");
    private static final Booster BOOST1_2 = new Booster("1.25x Money", 1.25, 10 * 60, 1, "boost1x2");
    private static final Booster BOOST1_3 = new Booster("1.25x Money", 1.25, 15 * 60, 1, "boost1x3");
    private static final Booster BOOST2_1 = new Booster("1.5x Money", 1.5, 5 * 60, 2, "boost2x1");
    private static final Booster BOOST2_2 = new Booster("1.5x Money", 1.5, 10 * 60, 2, "boost2x2");
    private static final Booster BOOST2_3 = new Booster("1.5x Money", 1.5, 15 * 60, 2, "boost2x3");
    private static final Booster BOOST3_1 = new Booster("1.75x Money", 1.75, 5 * 60, 3, "boost3x1");
    private static final Booster BOOST3_2 = new Booster("1.75x Money", 1.75, 10 * 60, 3, "boost3x2");
    private static final Booster BOOST3_3 = new Booster("1.75x Money", 1.75, 15 * 60, 3, "boost3x3");
    private static final Booster BOOST4_1 = new Booster("2x Money", 2, 5 * 60, 4, "boost4x1");
    private static final Booster BOOST4_2 = new Booster("2x Money", 2, 10 * 60, 4, "boost4x2");
    private static final Booster BOOST4_3 = new Booster("2x Money", 2, 15 * 60, 4, "boost4x3");

    /**
     * Retrieves a booster by its unique identifier.
     *
     * @param name the booster ID string (e.g., "boost1x1")
     * @return a new {@link Booster} instance corresponding to the ID, or null if invalid
     */
    public static Booster getBooster(String name) {
        return switch (name) {
            case "null" -> new Booster(BOOST0);
            case "boost1x1" -> new Booster(BOOST1_1);
            case "boost1x2" -> new Booster(BOOST1_2);
            case "boost1x3" -> new Booster(BOOST1_3);
            case "boost2x1" -> new Booster(BOOST2_1);
            case "boost2x2" -> new Booster(BOOST2_2);
            case "boost2x3" -> new Booster(BOOST2_3);
            case "boost3x1" -> new Booster(BOOST3_1);
            case "boost3x2" -> new Booster(BOOST3_2);
            case "boost3x3" -> new Booster(BOOST3_3);
            case "boost4x1" -> new Booster(BOOST4_1);
            case "boost4x2" -> new Booster(BOOST4_2);
            case "boost4x3" -> new Booster(BOOST4_3);
            default -> null;
        };
    }

    /**
     * Returns a random booster from a given multiplier category.
     * <p>
     * Selects one of the three boosters corresponding to the multiplier (0.25, 0.5, 0.75, 1.0)
     * using a random index.
     * </p>
     *
     * @param multiplier the multiplier value to select from
     * @return a random {@link Booster} with the given multiplier, or null if invalid
     */
    public static Booster getRandomBoosterFromMultiplier(double multiplier) {
        int randomIndex = random.nextInt(3);
        String s = String.valueOf(multiplier);

        return switch (s) {
            case "0.25" -> switch (randomIndex) {
                case 0 -> BOOST1_1;
                case 1 -> BOOST1_2;
                case 2 -> BOOST1_3;
                default -> null;
            };
            case "0.5" -> switch (randomIndex) {
                case 0 -> BOOST2_1;
                case 1 -> BOOST2_2;
                case 2 -> BOOST2_3;
                default -> null;
            };
            case "0.75" -> switch (randomIndex) {
                case 0 -> BOOST3_1;
                case 1 -> BOOST3_2;
                case 2 -> BOOST3_3;
                default -> null;
            };
            case "1" -> switch (randomIndex) {
                case 0 -> BOOST4_1;
                case 1 -> BOOST4_2;
                case 2 -> BOOST4_3;
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Determines a random booster based on the player's store level and bonus chance.
     * <p>
     * Uses a random roll with an adjusted range based on the player's {@link PlayerData#getOutBoosterChance()}.
     * Higher bonus reduces the effective range, increasing the chance of getting a booster.
     * </p>
     *
     * @param store the player's store level
     * @param pd    the {@link PlayerData} of the player
     * @return a {@link Booster} based on chance, or null if no booster was won
     */
    public static Booster getRandomBoosterWithChance(int store, PlayerData pd) {
        double startChance = 1.0 / 2000.0;
        double endChance = 1.0 / 500.0;
        double baseChance = startChance + (endChance - startChance) * ((store - 1) / 11.0);

        double bonusMultiplier = 1.0 + pd.getOutBoosterChance();
        double finalChance = baseChance * bonusMultiplier;

        int rollRange = (int) Math.max(1, Math.round(1.0 / finalChance));
        int roll = random.nextInt(rollRange) + 1;

        if (roll <= 3) {
            return getRandomBoosterFromMultiplier(0.25);
        } else if (roll <= 5) {
            return getRandomBoosterFromMultiplier(0.5);
        } else if (roll <= 7) {
            return getRandomBoosterFromMultiplier(0.75);
        } else if (roll == 8) {
            return getRandomBoosterFromMultiplier(1);
        }

        return null;
    }

}
