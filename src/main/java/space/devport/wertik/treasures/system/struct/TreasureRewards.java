package space.devport.wertik.treasures.system.struct;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import space.devport.utils.ConsoleOutput;
import space.devport.utils.configuration.Configuration;
import space.devport.utils.struct.Rewards;
import space.devport.wertik.treasures.system.treasure.struct.Treasure;
import space.devport.wertik.treasures.system.user.struct.User;

import java.util.HashSet;
import java.util.Set;

public class TreasureRewards extends Rewards {

    @Getter
    @Setter
    private Set<CountingRewards> cumulative = new HashSet<>();
    @Getter
    @Setter
    private Set<CountingRewards> repeat = new HashSet<>();

    public TreasureRewards() {
    }

    public TreasureRewards(Rewards rewards) {
        super(rewards);
    }

    public TreasureRewards(TreasureRewards rewards) {
        super(rewards);
        this.cumulative = new HashSet<>(rewards.getCumulative());
        this.repeat = new HashSet<>(rewards.getRepeat());
    }

    public void give(User user, Treasure treasure) {

        Player player = user.getPlayer();

        if (player == null) {
            ConsoleOutput.getInstance().warn("Player " + user.getOfflinePlayer().getName() + " is not online, cannot reward him.");
            return;
        }

        this.give(player);

        int count = user.getFindCount((t) -> t.getTool() != null && t.getTool().getRootTemplate().equals(treasure.getTool().getRootTemplate()));

        cumulative.forEach(r -> r.give(player, count));
        repeat.forEach(r -> r.give(player, count));
    }

    @Nullable
    public static TreasureRewards from(Configuration configuration, String path, boolean silent) {

        ConfigurationSection section = configuration.getFileConfiguration().getConfigurationSection(path);

        if (section == null) {
            if (!silent)
                ConsoleOutput.getInstance().warn("Could not load treasure rewards, section " + configuration.getFile().getName() + "@" + path + " is invalid.");
            return null;
        }

        Rewards rewards = configuration.getRewards(section.getCurrentPath());

        TreasureRewards treasureRewards = new TreasureRewards(rewards);

        ConfigurationSection cumulative = section.getConfigurationSection("cumulative");

        if (cumulative != null) {
            for (String count : cumulative.getKeys(false)) {
                CountingRewards countingRewards = CountingRewards.from(configuration, cumulative.getCurrentPath() + "." + count, Integer::equals, silent);
                if (countingRewards == null)
                    continue;
                treasureRewards.getCumulative().add(countingRewards);
            }
            ConsoleOutput.getInstance().debug("Loaded " + treasureRewards.getCumulative().size() + " repeating rewards...");
        }

        ConfigurationSection repeat = section.getConfigurationSection("repeat");

        if (repeat != null) {
            for (String count : repeat.getKeys(false)) {
                CountingRewards countingRewards = CountingRewards.from(configuration, repeat.getCurrentPath() + "." + count, Integer::equals, silent);
                if (countingRewards == null)
                    continue;
                treasureRewards.getRepeat().add(countingRewards);
            }
            ConsoleOutput.getInstance().debug("Loaded " + treasureRewards.getRepeat().size() + " repeating rewards...");
        }

        ConsoleOutput.getInstance().debug("Loaded treasure rewards at " + configuration.getFile().getName() + "@" + path);
        return treasureRewards;
    }

    public void to(Configuration configuration, String path) {

        configuration.setRewards(path, this);

        Set<CountingRewards> rewards = new HashSet<>(this.cumulative);
        rewards.addAll(this.repeat);

        for (CountingRewards reward : rewards) {
            reward.to(configuration, path + "." + reward.getCount());
        }
    }
}