package me.aleksilassila.islands;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import me.aleksilassila.islands.Islands;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IslandLayout {
    private final Islands islands;

    public final int islandSpacing;
    public final int verticalSpacing;

    public IslandLayout(Islands instance) {
        this.islands = instance;

        this.islandSpacing = instance.plugin.getConfig().getInt("generation.islandSpacing");
        this.verticalSpacing = islands.plugin.getConfig().getInt("generation.islandVerticalSpacing");
    }

    private FileConfiguration getIslandsConfig() {
        return islands.plugin.getIslandsConfig();
    }

    public String createIsland(UUID uuid, int islandSize) {
        int index = 0;

        while (true) {
            int[] pos = placement.getIslandPos(index);

            if (!getIslandsConfig().getKeys(false).contains(posToIslandId(pos[0], pos[1]))) {
                return addIslandToConfig(pos[0], pos[1], islandSize, uuid, String.valueOf(getNewHomeId(uuid)));
            }

            index++;
        }
    }

    @NotNull
    private String addIslandToConfig(int xIndex, int zIndex, int islandSize, UUID uuid, String name) {
        int realX = xIndex * islandSpacing + islandSpacing / 2 - islandSize / 2;
        int realY = getIslandY(xIndex, zIndex);
        int realZ = zIndex * islandSpacing + islandSpacing / 2 - islandSize / 2;

        int home = getNewHomeId(uuid);

        String islandId = posToIslandId(xIndex, zIndex);

        getIslandsConfig().set(islandId + ".xIndex", xIndex);
        getIslandsConfig().set(islandId + ".zIndex", zIndex);

        getIslandsConfig().set(islandId + ".x", realX);
        getIslandsConfig().set(islandId + ".y", realY);
        getIslandsConfig().set(islandId + ".z", realZ);

        getIslandsConfig().set(islandId + ".spawnPoint.x", realX + islandSize / 2);
        getIslandsConfig().set(islandId + ".spawnPoint.z", realZ + islandSize / 2);

        getIslandsConfig().set(islandId + ".UUID", uuid.toString());
        getIslandsConfig().set(islandId + ".name", name);
        getIslandsConfig().set(islandId + ".home", home);
        getIslandsConfig().set(islandId + ".size", islandSize);
        getIslandsConfig().set(islandId + ".public", 0);

        islands.plugin.saveIslandsConfig();

        return islandId;
    }

    @Nullable
    public String getIslandId(int x, int z) {
        for (String islandId : getIslandsConfig().getKeys(false)) {
            if (x / islandSpacing == getIslandsConfig().getInt(islandId + ".xIndex")) {
                if (z / islandSpacing == getIslandsConfig().getInt(islandId + ".zIndex")) {
                    return islandId;
                }
            }
        }

        return null;
    }

    @NotNull
    public List<String> getAllIslandIds(UUID uuid) {
        List<String> islands = new ArrayList<>();

        for (String islandId : getIslandsConfig().getKeys(false)) {
            String islandUUID = getIslandsConfig().getString(islandId + ".UUID");

            if (islandUUID != null && islandUUID.equals(uuid.toString()))
                islands.add(islandId);
        }

        return islands;
    }

    @Nullable
    public String getIslandByName(String name) {
        for (String islandId : getIslandsConfig().getKeys(false)) {
            if (getIslandsConfig().getString(islandId + ".name").equalsIgnoreCase(name) && getIslandsConfig().getInt(islandId + ".public") == 1) {
                return islandId;
            }
        }

        return null;
    }

    @Nullable
    public String getHomeIsland(UUID uuid, int homeId) {
        List<String> allIslands = getAllIslandIds(uuid);

        for (String islandId : allIslands) {
            if (getIslandsConfig().getInt(islandId + ".home") == homeId) {
                return islandId;
            }
        }

        return null;
    }

    @Nullable
    public Location getIslandSpawn(String islandId) {
        if (getIslandsConfig().getKeys(false).contains(islandId)) {
            return new Location(
                    islands.plugin.islandsWorld,
                    getIslandsConfig().getInt(islandId + ".spawnPoint.x"),
                    getIslandsConfig().getInt(islandId + ".y") + 100,
                    getIslandsConfig().getInt(islandId + ".spawnPoint.z")
            );
        } else {
            return null;
        }
    }

    @Nullable
    public String getSpawnIsland() {
        for (String islandId : getIslandsConfig().getKeys(false)) {
            if (getIslandsConfig().getBoolean(islandId + ".isSpawn")) {
                return islandId;
            }
        }

        return null;
    }

    public boolean isBlockInIslandSphere(int x, int y, int z) {
        int xIndex = x / islandSpacing;
        int zIndex = z / islandSpacing;
        int islandLowY = getIslandY(xIndex, zIndex);

        int islandSize = getIslandsConfig().getInt(posToIslandId(xIndex, zIndex)  + ".size");

        int relativeX = x - (xIndex * islandSpacing + islandSpacing / 2 - islandSize / 2);
        int relativeZ = z - (zIndex * islandSpacing + islandSpacing / 2 - islandSize / 2);
        int relativeY = y - islandLowY;

        return islands.islandGeneration.isBlockInIslandSphere(relativeX, relativeY, relativeZ, islandSize);
    }

    @Nullable
    public String getBlockOwnerUUID(int x, int z) {
        int xIndex = x / islandSpacing;
        int zIndex = z / islandSpacing;

        int islandSize = getIslandsConfig().getInt(posToIslandId(xIndex, zIndex)  + ".size");

        int relativeX = x - (xIndex * islandSpacing + islandSpacing / 2 - islandSize / 2);
        int relativeZ = z - (zIndex * islandSpacing + islandSpacing / 2 - islandSize / 2);

        boolean isInside = islands.islandGeneration.isBlockInIslandCylinder(relativeX + 2, relativeZ + 2, islandSize + 4);

        if (!isInside) return null;

        return getIslandsConfig().getString(posToIslandId(xIndex, zIndex) + ".UUID");
    }

    public int getNewHomeId(UUID uuid) {
        List<String> ids = getAllIslandIds(uuid);
        List<Integer> homeIds = new ArrayList<>();

        for (String islandId : ids) {
            int homeNumber = getIslandsConfig().getInt(islandId + ".home");
            homeIds.add(homeNumber);
        }

        int home = getNumberOfIslands(uuid) + 1;

        for (int i = 1; i <= getNumberOfIslands(uuid) + 1; i++) {
            if (!homeIds.contains(i)) home = i;
        }

        return home;
    }

    // UTILS

    private int getIslandY(int xIndex, int zIndex) {
        return 10 + ((xIndex + zIndex) % 3) * verticalSpacing;
    }

    public int getNumberOfIslands(UUID uuid) {
        return getAllIslandIds(uuid).size();
    }

    @NotNull
    public List<String> getTrusted(String islandId) {
        return getIslandsConfig().getStringList(islandId + ".trusted");
    }

    String posToIslandId(int xIndex, int zIndex) {
        return xIndex + "x" + zIndex;
    }

    // MANAGMENT

    public void updateIslandSize(String islandId, int islandSize) {
        int xIndex = getIslandsConfig().getInt(islandId + ".xIndex");
        int zIndex = getIslandsConfig().getInt(islandId + ".zIndex");

        int realX = xIndex * islandSpacing + islandSpacing / 2 - islandSize / 2;
        int realZ = zIndex * islandSpacing + islandSpacing / 2 - islandSize / 2;

        getIslandsConfig().set(islandId + ".x", realX);
        getIslandsConfig().set(islandId + ".z", realZ);

        getIslandsConfig().set(islandId + ".size", islandSize);

        islands.plugin.saveIslandsConfig();
    }

    public void addTrusted(String islandId, String UUID) {
        List<String> trusted = getIslandsConfig().getStringList(islandId + ".trusted");
        trusted.add(UUID);
        getIslandsConfig().set(islandId + ".trusted", trusted);

        islands.plugin.saveIslandsConfig();
    }

    public void removeTrusted(String islandId, String UUID) {
        List<String> trusted = getIslandsConfig().getStringList(islandId + ".trusted");
        trusted.remove(UUID);
        getIslandsConfig().set(islandId + ".trusted", trusted);
        islands.plugin.saveIslandsConfig();
    }

    public void setSpawnPoint(String islandId, int x, int z) {
        getIslandsConfig().set(islandId + ".spawnPoint.x", x);
        getIslandsConfig().set(islandId + ".spawnPoint.z", z);

        islands.plugin.saveIslandsConfig();
    }

    public void unnameIsland(String islandId) {
        int homeId = getIslandsConfig().getInt(islandId + ".home");

        getIslandsConfig().set(islandId + ".name", String.valueOf(homeId));
        getIslandsConfig().set(islandId + ".public", 0);

        islands.plugin.saveIslandsConfig();
    }

    public void nameIsland(String islandId, String name){
            getIslandsConfig().set(islandId + ".name", name);
            getIslandsConfig().set(islandId + ".public", 1);

            islands.plugin.saveIslandsConfig();
    }

    public void giveIsland(String islandId, Player player) {
        getIslandsConfig().set(islandId + ".home", getNewHomeId(player.getUniqueId()));
        getIslandsConfig().set(islandId + ".UUID", player.getUniqueId().toString());

        islands.plugin.saveIslandsConfig();
    }

    public void giveIsland(String islandId) {
        getIslandsConfig().set(islandId + ".home", -1);
        getIslandsConfig().set(islandId + ".UUID", null);

        islands.plugin.saveIslandsConfig();
    }

    public void deleteIsland(String islandId) {
        getIslandsConfig().set(islandId, null);

        islands.plugin.saveIslandsConfig();
    }

    static class placement {
        static int getLayer(int index) {
            return (int) Math.floor(Math.sqrt(index));
        }

        static int getLayerSize(int layer) {
            return 2 * layer + 1;
        }

        static int firstOfLayer(int layer) {
            return layer * layer;
        }

        static int[] getIslandPos(int index) {
            int layer = getLayer(index);

            int x = Math.min(index - firstOfLayer(layer), layer);
            int z = (index - firstOfLayer(layer) < layer + 1) ? layer : firstOfLayer(layer) + getLayerSize(layer) - 1 - index;

            return new int[]{x, z};
        }
    }
}