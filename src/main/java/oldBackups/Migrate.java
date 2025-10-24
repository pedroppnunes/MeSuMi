/*
public void migrateOldBackupIfNeeded() {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        if (!backupFile.exists()) return;

        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        Object itemsObj = backupConfig.get("items");

        // Check if it's the old format (a list)
        if (itemsObj instanceof List<?> oldList) {
            Map<String, Object> newItems = new LinkedHashMap<>();
            for (Object obj : oldList) {
                if (obj instanceof Map<?, ?> map) {
                    Object droppedItem = map.get("droppedItem");
                    if (droppedItem instanceof String id) {
                        newItems.put(id, map);
                    }
                }
            }

            // Replace and save
            backupConfig.set("items", newItems);
            try {
                backupConfig.save(backupFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 */