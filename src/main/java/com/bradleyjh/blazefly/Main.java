////////////////////////////////////////////////////////////////////////////
// This file is part of BlazeFly.                                         //
//                                                                        //
// BlazeFly is free software: you can redistribute it and/or modify       //
// it under the terms of the GNU General Public License as published by   //
// the Free Software Foundation, either version 3 of the License, or      //
// (at your option) any later version.                                    //
//                                                                        //
// BlazeFly is distributed in the hope that it will be useful,            //
// but WITHOUT ANY WARRANTY; without even the implied warranty of         //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the           //
// GNU General Public License for more details.                           //
//                                                                        //
// You should have received a copy of the GNU General Public License      //
// along with BlazeFly. If not, see <http://www.gnu.org/licenses/>.       //
////////////////////////////////////////////////////////////////////////////

package com.bradleyjh.blazefly;

import java.util.HashMap;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;

public class Main extends JavaPlugin implements Listener {
    Core core = new Core();
    String updateAvailable;
    
    public void onEnable() {
        saveDefaultConfig();
        core.stringsFile = new File(getDataFolder() + File.separator + "strings.yml");
        if (! core.stringsFile.exists()) { saveResource("strings.yml", false); }
        core.strings = YamlConfiguration.loadConfiguration(core.stringsFile);
        core.playersFile = new File(getDataFolder() + File.separator + "players.yml");
        core.players = YamlConfiguration.loadConfiguration(core.playersFile);
        
        core.retrieveAll();
        core.disabledWorlds = getConfig().getStringList("disabledWorlds");
        getServer().getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("updateChecker")) {
            getServer().getScheduler().runTaskAsynchronously(this, new Updater(this, getDescription().getVersion()));
        }
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Timer(this), 10L, 10L);
    }
    
    public void onDisable() {
        core.storeAll();
    }

    // Replaces the deprecated getServer().getPlayer()
    public Player getPlayer(String name) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) { return player; }
        }
        return null;
    }
    
    // Check permissions
    public boolean hasPermission (CommandSender sender, String permission) {
        if (sender.hasPermission("blazefly." + permission)) { return true; }
        return false;
    }
    
    // Get the fuel block currently used
    public String fuelBlock(Player player) {
        if (hasPermission(player, "vip")) { return getConfig().getString("VIPBlock").toUpperCase(); }
        else { return getConfig().getString("fuelBlock").toUpperCase(); }
    }
    
    // Get the fuel subdata currently used
    public Integer fuelSubdata(Player player) {
        if (hasPermission(player, "vip")) { return getConfig().getInt("VIPSubdata"); }
        else { return getConfig().getInt("fuelSubdata"); }
    }
    
    // Get the time each fuel block should last (-1 because Timer adds a second at 0)
    public Double fuelTime(Player player) {
        if (hasPermission(player, "vip")) { return getConfig().getDouble("VIPTime") - 1; }
        else { return getConfig().getDouble("fuelTime") - 1; }
    }
    
    // Make sure the gamemode is applicable for BlazeFly
    public Boolean correctMode (Player player) {
        if (player.getGameMode() == GameMode.SURVIVAL) { return true; }
        if (player.getGameMode() == GameMode.ADVENTURE) { return true; }
        return false;
    }

    // Process incoming commands
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        
        // Reload the configuration file
        if (commandLabel.equalsIgnoreCase("bfreload")) {
            if (! hasPermission(sender, "reload")) {
                core.messagePlayer(sender, "permission", null);
                return true;
            }
            reloadConfig();
            core.stringsFile = new File(getDataFolder() + File.separator + "strings.yml");
            core.strings = YamlConfiguration.loadConfiguration(core.stringsFile);
            core.disabledWorlds = getConfig().getStringList("disabledWorlds");
            core.messagePlayer(sender, "reload", null);
            return true;
        }

        // Disable a given users flight
        if (commandLabel.equalsIgnoreCase("flyoff")) {
            if (hasPermission(sender, "flyoff")) {
                if (args.length != 1) { return false; }
                Player target = getPlayer(args[0]);

                if ((target == null) || (!target.isOnline())) {
                    HashMap<String, String> keywords = new HashMap<>();
                    keywords.put("%player%", args[0]);
                    core.messagePlayer(sender, "notOnline", keywords);
                    return true;
                }
                if (! target.getAllowFlight()) {
                    HashMap<String, String> keywords = new HashMap<>();
                    keywords.put("%player%", args[0]);
                    core.messagePlayer(sender, "notFlying", keywords);
                    return true;
                }

                core.setFalling(target, true);
                core.setFlying(target, false);
                HashMap<String, String> keywords = new HashMap<>();
                keywords.put("%player%", args[0]);
                core.messagePlayer(sender, "flyoffAdmin", keywords);
                core.messagePlayer(target, "flyoffPlayer", null);
                return true;
            }
            else { core.messagePlayer(sender, "permission", null); }
            return true;
        }

        // Prevent console from using flight commands
        if (! (sender instanceof Player)) {
            sender.sendMessage("Console cannot fly");
            return true;
        }
        Player player = (Player)sender;
        
        // If they aren't in the correct mode, ignore commands
        if (! correctMode(player)) {
            core.messagePlayer(player, "mode", null);
            return true;
        }

        if ((core.disabledWorlds.contains(player.getWorld().getName())) && (! hasPermission(player, "anyworld"))) {
            core.messagePlayer(player, "disabled", null);
            return true;
        }

        // Fly (or bfly) command to enable/disable flight
        if (commandLabel.equalsIgnoreCase("fly") || commandLabel.equalsIgnoreCase("bfly")) {
            if (! hasPermission(player, "use")) {
                core.messagePlayer(player, "permission", null);
                return true;
            }
            if (args.length > 0) { return false; }

            // Disable flight
            if (player.getAllowFlight()) {
                core.setFalling(player, true);
                core.setFlying(player, false);
                player.setAllowFlight(false);
                core.messagePlayer(player, "fDisabled", null);
                return true;
            }
            // Enable flight
            else {
                // Wings are broken, can't fly
                if (core.isBroken(player)) {
                    core.messagePlayer(player, "wBroken", null);
                    return true;
                }
                // Doesn't require fuel
                if (hasPermission(player, "nofuel")) {
                    core.messagePlayer(player, "fEnabled", null);
                    core.setFlying(player, true);
                    player.setAllowFlight(true);
                    return true;
                }

                // Regular and VIP players
                if (core.hasFuel(player, fuelBlock(player), fuelSubdata(player)) || core.hasFuelCount(player)) {
                    core.messagePlayer(player, "fEnabled", null);
                    core.setFlying(player, true);
                    player.setAllowFlight(true);
                    
                    if (! core.hasFuelCount(player)) {
                        core.increaseFuelCount(player, fuelTime(player));
                        core.removeFuel(player, fuelBlock(player), fuelSubdata(player));
                    }
                    
                    if (! core.hasFuel(player, fuelBlock(player), fuelSubdata(player))) {
                        core.messagePlayer(player, "fLast", null);
                    }
                }
                else {
                    String fuelName = getConfig().getString("fuelName");
                    if (hasPermission(player, "vip")) { fuelName = getConfig().getString("VIPName"); }
                    HashMap<String, String> keywords = new HashMap<>();
                    keywords.put("%fuel%", fuelName);
                    core.messagePlayer(player, "fRequired", keywords);
                    return true;
                }
                return true;
            }
        }

        // Change your speed of flight (1x, 2x or 4x speed)
        if (commandLabel.equalsIgnoreCase("flyspeed")) {
            if (! hasPermission(player, "flyspeed")) {
                core.messagePlayer(player, "permission", null);
                return true;
            }
            if (getConfig().getString("allowSpeed").equalsIgnoreCase("false")) {
                core.messagePlayer(player, "fsDisabled", null);
                return true;
            }
            if (! player.getAllowFlight()) {
                core.messagePlayer(player, "fsFlight", null);
                return true;
            }

            if (args.length == 0) {
                if (player.getFlySpeed() == 0.1F) {
                    core.messagePlayer(player, "fs1", null);
                    return true;
                }
                if (player.getFlySpeed() == 0.2F) {
                    core.messagePlayer(player, "fs2", null);
                    return true;
                }
                if (player.getFlySpeed() == 0.4F) {
                    core.messagePlayer(player, "fs4", null);
                    return true;
                }
            }
            else {
                if (args[0].equals("1")) {
                    player.setFlySpeed(0.1F);
                    core.messagePlayer(player, "fs1", null);
                    return true;
                }
                if (args[0].equals("2")) {
                    player.setFlySpeed(0.2F);
                    core.messagePlayer(player, "fs2", null);
                    return true;
                }
                if (args[0].equals("4")) {
                    player.setFlySpeed(0.4F);
                    core.messagePlayer(player, "fs4", null);
                    return true;
                }
                
                core.messagePlayer(player, "fsOptions", null);
                return true;
            }
        }
        
        // Fuel command to check how many seconds of fuel are remaining
        if (commandLabel.equalsIgnoreCase("fuel")) {
            if (! hasPermission(player, "use")) {
                core.messagePlayer(player, "permission", null);
                return true;
            }
            if (args.length > 0) { return false; }
        
            if (! core.hasFuelCount(player)) {
                if (hasPermission(player, "nofuel")) {
                    core.messagePlayer(player, "fuelNA", null);
                    return true;
                }
                else {
                    core.messagePlayer(player, "fuelStart", null);
                    return true;
                }
            }

            HashMap<String, String> keywords = new HashMap<>();
            Integer flySpeed = Math.round(player.getFlySpeed() * 10);
            Double rawSeconds = core.getFuelCount(player) / flySpeed;
            Integer roundSeconds = Math.round(rawSeconds.longValue());
            keywords.put("%seconds%", roundSeconds.toString());
            keywords.put("%flyspeed%", flySpeed.toString());
            core.messagePlayer(sender, "fuelMessage", keywords);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if ((event.getEntity() instanceof Player)) {
            Player player = (Player)event.getEntity();

            // Protect players falling due to running out of fuel, if enabled
            if ((event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) && (core.isFalling(player))) {
                if (getConfig().getBoolean("fallProtection")) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Ignore these types of damage
            if (event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING)) { return; }
            if (event.getCause().equals(EntityDamageEvent.DamageCause.STARVATION)) { return; }
            if (event.getDamage() == 0) { return; }

            // PvP stuff, break wings (disallow flight) if player takes damage
            if ((getConfig().getBoolean("breakableWings")) && (! hasPermission(player, "superwings")) && (core.isFlying(player))) {
                core.setFalling(player, true);
                core.setFlying(player, false);
                core.setBrokenCounter(player, getConfig().getDouble("healTime"));
                player.setAllowFlight(false);
                core.messagePlayer(player, "wBroke", null);
            }
        }
    }

    // Reset flyspeed & message admins for new versions
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.setFlySpeed(0.1F);

        core.retrievePlayer(player);
        
        if (hasPermission(player, "updates") && updateAvailable != null) {
            String header = getConfig().getString("header");
            String message = header + updateAvailable + " is available for download";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    // Clear broken wing counter if player dies
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if ((event.getEntity() instanceof Player)) {
            Player player = (Player)event.getEntity();
        
            if (core.isBroken(player)) {
                core.removeBroken(player);
                player.setAllowFlight(true);
                core.setFlying(player, true);
                core.messagePlayer(player, "wHealed", null);
            }
        }
    }
    
    // Prevent flight toggle for mode switch
    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SURVIVAL || event.getNewGameMode() == GameMode.ADVENTURE) {
            if (core.isFlying(event.getPlayer())) {
                final Player player = event.getPlayer();
                getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    public void run() {
                        player.setAllowFlight(true);
                        player.getPlayer().setFlying(true);
                    }
                }, 2L);
            }
        }
    }
}