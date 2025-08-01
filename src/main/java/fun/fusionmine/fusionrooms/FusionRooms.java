package fun.fusionmine.fusionrooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FusionRooms extends JavaPlugin implements Listener {

    @Getter
    private static FusionRooms instance;

    private Map<String, PVPRoom> rooms = new HashMap();

    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadRooms();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadRooms() {
        ConfigurationSection roomsCfgSection = this.getConfig().getConfigurationSection("rooms");
        roomsCfgSection.getKeys(false).forEach((roomId) -> {
            ConfigurationSection roomCfgSection = roomsCfgSection.getConfigurationSection(roomId);
            String worldName = roomCfgSection.getString("world");
            String region = roomCfgSection.getString("region");
            int endOfPVPTimer = roomCfgSection.getInt("end_of_pvp_timer");
            int needPlayers = roomCfgSection.getInt("need_players");
            Location outsideTeleportLocation = this.getLocationFromConfigurationSection(worldName, roomCfgSection.getConfigurationSection("outside_teleport_location"));
            List<Location> doorLocations = new ArrayList();
            ConfigurationSection doorLocationsCfgSection = roomCfgSection.getConfigurationSection("door_locations");
            doorLocations.add(this.getLocationFromConfigurationSection(worldName, doorLocationsCfgSection.getConfigurationSection("from")));
            doorLocations.add(this.getLocationFromConfigurationSection(worldName, doorLocationsCfgSection.getConfigurationSection("to")));
            PVPRoom pvpRoom = new PVPRoom(worldName, endOfPVPTimer, needPlayers, outsideTeleportLocation, doorLocations);
            this.rooms.put(region, pvpRoom);
        });
        this.getLogger().info("Загружено " + this.rooms.size() + " кабинок!");
    }

    @EventHandler
    public void onEntered(RegionEnteredEvent event) {
        Player player = event.getPlayer();
        String regionName = event.getRegionName();
        if (this.rooms.containsKey(regionName)) {
            PVPRoom pvpRoom = this.rooms.get(regionName);
            if (player.getWorld().getName().equalsIgnoreCase(pvpRoom.getWorldName())) {
                if (pvpRoom.isRunning()) {
                    player.sendMessage(this.translateString("room_busy"));
                    player.teleportAsync(pvpRoom.getOutsideTeleportLocation());
                    return;
                }

                List<Player> players = pvpRoom.getPlayers();
                players.add(player);
                players.forEach((p) -> {
                    p.sendMessage(this.translateString("room_join").replace("%player%", player.getName()).replace("%current_players%", String.valueOf(players.size())).replace("%need_players%", String.valueOf(pvpRoom.getNeedPlayers())));
                });
                pvpRoom.update();
            }
        }
    }

    @EventHandler
    public void onLeft(RegionLeftEvent event) {
        Player player = event.getPlayer();
        String regionName = event.getRegionName();
        if (this.rooms.containsKey(regionName)) {
            PVPRoom pvpRoom = this.rooms.get(regionName);
            if (player.getWorld().getName().equalsIgnoreCase(pvpRoom.getWorldName())) {
                List<Player> players = pvpRoom.getPlayers();
                if (players.contains(player)) {
                    players.forEach((p) -> {
                        p.sendMessage(this.translateString("room_quit").replace("%player%", player.getName()));
                    });
                    players.remove(player);
                    pvpRoom.update();
                }
            }
        }
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.rooms.values().stream().filter((room) -> {
            return room.getPlayers().contains(player);
        }).forEach((room) -> {
            room.getPlayers().remove(player);
            room.update();
            player.teleport(room.getOutsideTeleportLocation());
        });
    }

    public Location getLocationFromConfigurationSection(String worldName, ConfigurationSection configurationSection) {
        double x = configurationSection.getDouble("x");
        double y = configurationSection.getDouble("y");
        double z = configurationSection.getDouble("z");
        float yaw = (float)configurationSection.getDouble("yaw");
        float pitch = (float)configurationSection.getDouble("pitch");
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public String translateString(String path) {
        return (Objects.requireNonNull(this.getConfig().getString("messages.prefix")) + Objects.requireNonNull(this.getConfig().getString("messages." + path))).replace("&", "§");
    }

}
