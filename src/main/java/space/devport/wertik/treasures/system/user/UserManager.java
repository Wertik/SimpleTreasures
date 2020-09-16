package space.devport.wertik.treasures.system.user;

import com.google.gson.reflect.TypeToken;
import space.devport.utils.ConsoleOutput;
import space.devport.wertik.treasures.TreasurePlugin;
import space.devport.wertik.treasures.system.GsonHelper;
import space.devport.wertik.treasures.system.user.struct.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserManager {

    private final TreasurePlugin plugin;

    private final GsonHelper gsonHelper;

    private final Map<UUID, User> loadedUsers = new HashMap<>();

    public UserManager(TreasurePlugin plugin) {
        this.plugin = plugin;
        this.gsonHelper = plugin.getGsonHelper();
    }

    public User getOrCreateUser(UUID uniqueID) {
        return !this.loadedUsers.containsKey(uniqueID) ? createUser(uniqueID) : this.loadedUsers.get(uniqueID);
    }

    public User createUser(UUID uniqueID) {
        User user = new User(uniqueID);
        this.loadedUsers.put(uniqueID, user);
        ConsoleOutput.getInstance().debug("Created user " + uniqueID.toString());
        return user;
    }

    public void deleteAllReferences(UUID uniqueID) {
        CompletableFuture.runAsync(() -> {
            int count = 0;
            for (User user : this.loadedUsers.values()) {
                user.removeFind(uniqueID);
                count++;
            }
            ConsoleOutput.getInstance().debug("Removed " + count + " reference(s) of treasure " + uniqueID);
        });
    }

    public void load() {
        this.loadedUsers.clear();

        Map<UUID, User> loadedData = gsonHelper.load(plugin.getDataFolder() + "/user-data.json", new TypeToken<Map<UUID, User>>() {
        }.getType());

        if (loadedData == null) loadedData = new HashMap<>();

        this.loadedUsers.putAll(loadedData);

        plugin.getConsoleOutput().info("Loaded " + this.loadedUsers.size() + " user(s)...");
    }

    public void save() {

        // Purge empty
        int count = 0;
        for (UUID uniqueID : new HashSet<>(this.loadedUsers.keySet())) {
            User user = this.loadedUsers.get(uniqueID);
            if (user.getFoundTreasures().isEmpty()) {
                this.loadedUsers.remove(uniqueID);
                count++;
            }
        }
        plugin.getConsoleOutput().info("Purged " + count + " empty user(s)...");

        gsonHelper.save(this.loadedUsers, plugin.getDataFolder() + "/user-data.json");
        plugin.getConsoleOutput().info("Saved " + this.loadedUsers.size() + " user(s)...");
    }
}