package fun.fusionmine.fusionrooms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class PVPRoom {

    private String worldName;
    private int endOfPVPTimer;
    private int needPlayers;
    private Location outsideTeleportLocation;
    private List<Location> doorLocations;
    private List<Player> players;
    private boolean running;
    private BukkitTask winTask;

    public PVPRoom(String worldName, int endOfPVPTimer, int needPlayers, Location outsideTeleportLocation, List<Location> doorLocations) {
        this.worldName = worldName;
        this.endOfPVPTimer = endOfPVPTimer;
        this.needPlayers = needPlayers;
        this.outsideTeleportLocation = outsideTeleportLocation;
        this.doorLocations = doorLocations;
        this.players = new ArrayList();
    }

    public void update() {
        if (this.running) {
            if (this.players.isEmpty()) {
                this.reset();
                return;
            }

            if (this.players.size() == 1) {
                final Player winner = this.getPlayers().get(0);
                long startTime = System.currentTimeMillis();
                final long endTime = startTime + TimeUnit.SECONDS.toMillis(this.endOfPVPTimer);
                winner.sendMessage(FusionRooms.getInstance().translateString("you_win"));
                this.winTask = (new BukkitRunnable() {
                    public void run() {
                        if (System.currentTimeMillis() < endTime) {
                            long timeLeftInSeconds = (endTime - System.currentTimeMillis()) / 1000L;
                            winner.sendMessage(FusionRooms.getInstance().translateString("you_will_be_teleported_after_win").replace("%sec%", String.valueOf(timeLeftInSeconds)));
                        } else {
                            winner.teleport(PVPRoom.this.getOutsideTeleportLocation());
                        }

                    }
                }).runTaskTimer(FusionRooms.getInstance(), 0L, 21L);
                return;
            }
        }

        if (this.players.size() >= this.needPlayers && !this.running) {
            this.running = true;
            this.players.forEach((p) -> {
                p.sendMessage(FusionRooms.getInstance().translateString("room_started"));
            });
            this.setDoor(Material.BARRIER);
        }

    }

    public void reset() {
        this.running = false;
        this.players.clear();
        this.winTask.cancel();
        this.setDoor(Material.AIR);
    }

    private void setDoor(Material type) {
        List<Long> xCoords = Arrays.asList((long)(this.doorLocations.get(0)).getBlockX(), (long)(this.doorLocations.get(1)).getBlockX());
        List<Long> yCoords = Arrays.asList((long)(this.doorLocations.get(0)).getBlockY(), (long)(this.doorLocations.get(1)).getBlockY());
        List<Long> zCoords = Arrays.asList((long)(this.doorLocations.get(0)).getBlockZ(), (long)(this.doorLocations.get(1)).getBlockZ());

        for(long x = Collections.min(xCoords); x <= Collections.max(xCoords); ++x) {
            for(long z = Collections.min(zCoords); z <= Collections.max(zCoords); ++z) {
                for(long y = Collections.min(yCoords); y <= Collections.max(yCoords); ++y) {
                    try {
                        Bukkit.getWorld(this.worldName).getBlockAt((int)x, (int)y, (int)z).setType(type);
                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

}
