package robbery.crypto;

import java.util.UUID;

public class StoredFuel {
    private final UUID id;
    private final double quality;

    public StoredFuel(UUID id, double quality) {
        this.id = id != null ? id : UUID.randomUUID();
        this.quality = quality;
    }

    public StoredFuel(double quality) {
        this(UUID.randomUUID(), quality);
    }

    public UUID getId() {
        return id;
    }

    public double getQuality() {
        return quality;
    }
}
