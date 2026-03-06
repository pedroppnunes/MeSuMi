package robbery.util;

import org.bukkit.Bukkit;
import robbery.core.Robbery;
import robbery.items.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ItemsReloader {

    private ItemsReloader() {}

    /**
     * Reloads additems.yml templates (calls Robbery.addItemstoMap()) then synchronizes
     * all currently loaded Items instances with the new templates.
     *
     * @param main the plugin instance
     * @return number of live items that were updated
     */
    public static int reloadAndSync(Robbery main) {
        // First, reload templates (this is your existing method)
        main.addItemstoMap();

        // Grab templates and live list
        Map<String, Items> templates = Robbery.getItemsMap();
        List<Items> liveItems = new ArrayList<>(main.getItems()); // copy to avoid concurrent-mod problems

        AtomicInteger updatedCount = new AtomicInteger(0);

        Runnable syncTask = () -> {
            for (Items live : liveItems) {
                if (live == null) continue;
                String id = live.getId();
                if (id == null) continue;
                Items template = templates.get(id.toLowerCase());
                if (template == null) continue;

                boolean changed = applyTemplateToLive(live, template);
                if (changed) {
                    updatedCount.incrementAndGet();
                    tryInvokeRefresh(live);
                }
            }

            // Persist items.yml so the updated runtime values are saved (mirrors "saveItems" purpose)
            try {
                main.saveItems();
            } catch (Exception e) {
                main.getLogger().warning("Failed to save items after reload sync: " + e.getMessage());
            }
        };

        if (Bukkit.isPrimaryThread()) {
            syncTask.run();
        } else {
            Bukkit.getScheduler().runTask(main, syncTask);
        }

        return updatedCount.get();
    }

    /**
     * Applies hp/value/name/playername/time from template to live item.
     * Uses public setters if present; falls back to reflective field writes.
     *
     * @return true if at least one field changed
     */
    private static boolean applyTemplateToLive(Items live, Items template) {
        boolean changed = false;

        // 1) Try setters first (common names). If a setter exists we call it.
        changed |= tryCallSetter(live, "setHp", double.class, template.getHp());
        changed |= tryCallSetter(live, "setValue", int.class, template.getValue());
        changed |= tryCallSetter(live, "setTime", int.class, template.getTime());
        changed |= tryCallSetter(live, "setName", String.class, template.getName());
        // attempt both playername variants
        changed |= tryCallSetter(live, "setPlayerName", String.class, template.getPlayername());
        changed |= tryCallSetter(live, "setPlayername", String.class, template.getPlayername());

        // 2) If any setter was missing, try writing fields using common field names
        // (covers private fields like 'hp', 'value', 'time', 'name', 'playername')
        changed |= setFieldIfDifferent(live, "hp", template.getHp());
        changed |= setFieldIfDifferent(live, "value", template.getValue());
        changed |= setFieldIfDifferent(live, "time", template.getTime());
        changed |= setFieldIfDifferent(live, "name", template.getName());
        changed |= setFieldIfDifferent(live, "playername", template.getPlayername());
        changed |= setFieldIfDifferent(live, "playerName", template.getPlayername());

        return changed;
    }

    private static boolean tryCallSetter(Object target, String methodName, Class<?> paramType, Object value) {
        try {
            Method m = target.getClass().getMethod(methodName, paramType);
            m.invoke(target, value);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Throwable t) {
            // method exists but invocation failed; log and continue
            try {
                Robbery.getInstance().getLogger().warning("Failed to call setter " + methodName + " on " + target.getClass().getSimpleName() + ": " + t.getMessage());
            } catch (Exception ignored) {}
            return false;
        }
    }

    private static boolean setFieldIfDifferent(Object target, String fieldName, Object newValue) {
        try {
            Field f = findFieldRecursive(target.getClass(), fieldName);
            if (f == null) return false;
            f.setAccessible(true);
            Object current = f.get(target);

            if (!Objects.equals(current, newValue)) {
                f.set(target, newValue);
                return true;
            }
        } catch (Throwable t) {
            try {
                Robbery.getInstance().getLogger().warning("Failed to set field " + fieldName + " on " + target.getClass().getSimpleName() + ": " + t.getMessage());
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static Field findFieldRecursive(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Tries to call a refresh / visual update method on the item so the world shows newest values.
     * Common names attempted: refreshDisplay, updateVisual, updateArmorStand, applyTemplate
     */
    private static void tryInvokeRefresh(Object item) {
        String[] candidates = {"refreshDisplay", "updateVisual", "updateArmorStand", "applyTemplate", "update"};
        for (String name : candidates) {
            try {
                Method m = item.getClass().getMethod(name);
                m.invoke(item);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                try {
                    Robbery.getInstance().getLogger().warning("Failed to invoke " + name + " on " + item.getClass().getSimpleName() + ": " + t.getMessage());
                } catch (Exception ignored) {}
            }
        }
    }
}