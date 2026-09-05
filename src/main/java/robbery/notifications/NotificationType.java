package robbery.notifications;

public enum NotificationType {
    ABILITY_PROCS("Ability Procs", "Skill tree ability procs (Steal Speed, Money Mult, Double Catch, etc.)"),
    HIDEOUT_VALUE("Hideout Value", "Hideout value contribution messages on /sell"),
    HIDEOUT_DQ("Hideout Disqualification", "Hideout disqualification warnings on /sell"),
    ROBBERY_XP("Robbery XP", "Robbery XP earned messages on /sell"),
    CRYPTO_MACHINE("Crypto Machine", "Crypto machine alerts (battery empty, battery loaded, etc.)");

    private final String displayName;
    private final String description;

    NotificationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
