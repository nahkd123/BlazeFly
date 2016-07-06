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
import java.util.Iterator;

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
import org.bukkit.GameMode;

public class Main extends JavaPlugin implements Listener {
    Core core = new Core();
    String updateAvailable;

    public void onEnable() {
        saveDefaultConfig();
        core.disabledWorlds = getConfig().getStringList("disabledWorlds");
        getServer().getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("updateChecker")) {
            getServer().getScheduler().runTaskAsynchronously(this, new Updater(this, getDescription().getVersion()));
        }
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Timer(this), 5L, 5L);

        // For server reload disable flight and protect fallers
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getAllowFlight() && correctMode(player)) {
                messagePlayer(player, "fReload", null);
                core.setFallng(player, true);
                core.setFlying(player, false);
                player.setAllowFlight(false);
            }
        }
    }

    // Replaces the deprecated getServer().getPlayer()
    public Player getPlayer(String name) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) { return player; }
        }
        return null;
    }
    
    // Send a configurable message to a command sender
    public void messagePlayer (CommandSender sender, String type, HashMap<String, String> keywords) {
        if (! Main.this.getConfig().getString(type).isEmpty()) {
            
            // Get the header and the string
            String message = getConfig().getString("header") + getConfig().getString(type);
            
            // Replace keywords if they were provided
            if (keywords != null) {
                Iterator<String> iter = keywords.keySet().iterator();
                while (iter.hasNext()) {
                    String keyword = iter.next();
                    String replacement = keywords.get(keyword);
                    message = message.replace(keyword, replacement);
                }
            }
            
            // Apply formatting
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    // Check permissions
    public boolean hasPermission (CommandSender sender, String permission) {
        if (sender.hasPermission("blazefly." + permission)) { return true; }
        return false;
    }
    
    // Get the fuel block currently used
    public String fuelBlock(Player player) {
        if (hasPermission(player, "vip")) { return getConfig().getString("VIPBlock"); }
        else { return getConfig().getString("fuelBlock"); }
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
                messagePlayer(sender, "permission", null);
                return true;
            }
            reloadConfig();
            core.disabledWorlds = getConfig().getStringList("disabledWorlds");
            messagePlayer(sender, "reload", null);
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
                    messagePlayer(sender, "notOnline", keywords);
                    return true;
                }
                if (! target.getAllowFlight()) {
                    HashMap<String, String> keywords = new HashMap<>();
                    keywords.put("%player%", args[0]);
                    messagePlayer(sender, "notFlying", keywords);
                    return true;
                }

                core.setFallng(target, true);
                core.setFlying(target, false);
                HashMap<String, String> keywords = new HashMap<>();
                keywords.put("%player%", args[0]);
                messagePlayer(sender, "flyoffAdmin", keywords);
                messagePlayer(target, "flyoffPlayer", null);
                return true;
            }
            else { messagePlayer(sender, "permission", null); }
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
            messagePlayer(player, "mode", null);
            return true;
        }

        if ((core.disabledWorlds.contains(player.getWorld().getName())) && (! hasPermission(player, "anyworld"))) {
            messagePlayer(player, "disabled", null);
            return true;
        }

        // Fly (or bfly) command to enable/disable flight
        if (commandLabel.equalsIgnoreCase("fly") || commandLabel.equalsIgnoreCase("bfly")) {
            if (! hasPermission(player, "use")) {
                messagePlayer(player, "permission", null);
                return true;
            }
            if (args.length > 0) { return false; }

            // Disable flight
            if (player.getAllowFlight()) {
                core.setFallng(player, true);
                core.setFlying(player, false);
                player.setAllowFlight(false);
                messagePlayer(player, "fDisabled", null);
                return true;
            }
            // Enable flight
            else {
                // Wings are broken, can't fly
                if (core.isBroken(player)) {
                    messagePlayer(player, "wBroken", null);
                    return true;
                }
                // Doesn't require fuel
                if (hasPermission(player, "nofuel")) {
                    messagePlayer(player, "fEnabled", null);
                    core.setFlying(player, true);
                    player.setAllowFlight(true);
                    return true;
                }

                // Regular and VIP players
                if (core.hasFuel(player, fuelBlock(player), fuelSubdata(player)) || core.hasFuelCount(player)) {
                    messagePlayer(player, "fEnabled", null);
                    core.setFlying(player, true);
                    player.setAllowFlight(true);
                    
                    if (! core.hasFuelCount(player)) {
                        core.increaseFuelCount(player, fuelTime(player));
                        core.removeFuel(player, fuelBlock(player), fuelSubdata(player));
                    }
                    
                    if (! core.hasFuel(player, fuelBlock(player), fuelSubdata(player))) {
                        messagePlayer(player, "fLast", null);
                    }
                }
                else {
                    String fuel = getConfig().getString("fuelName");
                    if (hasPermission(player, "VIP")) { fuel = getConfig().getString("VIPName"); }
                    
                    HashMap<String, String> keywords = new HashMap<>();
                    keywords.put("%fuel%", fuel);
                    messagePlayer(player, "fRequired", keywords);
                    return true;
                }
                return true;
            }
        }

        // Change your speed of flight (1x, 2x or 4x speed)
        if (commandLabel.equalsIgnoreCase("flyspeed")) {
            if (! hasPermission(player, "flyspeed")) {
                messagePlayer(player, "permission", null);
                return true;
            }
            if (getConfig().getString("allowSpeed").equalsIgnoreCase("false")) {
                messagePlayer(player, "fsDisabled", null);
                return true;
            }
            if (! player.getAllowFlight()) {
                messagePlayer(player, "fsFlight", null);
                return true;
            }

            if (args.length == 0) {
                if (player.getFlySpeed() == 0.1F) {
                    messagePlayer(player, "fs1", null);
                    return true;
                }
                if (player.getFlySpeed() == 0.2F) {
                    messagePlayer(player, "fs2", null);
                    return true;
                }
                if (player.getFlySpeed() == 0.4F) {
                    messagePlayer(player, "fs4", null);
                    return true;
                }
            }
            else {
                if (args[0].equals("1")) {
                    player.setFlySpeed(0.1F);
                    messagePlayer(player, "fs1", null);
                    return true;
                }
                if (args[0].equals("2")) {
                    player.setFlySpeed(0.2F);
                    messagePlayer(player, "fs2", null);
                    return true;
                }
                if (args[0].equals("4")) {
                    player.setFlySpeed(0.4F);
                    messagePlayer(player, "fs4", null);
                    return true;
                }
                
                messagePlayer(player, "fsOptions", null);
                return true;
            }
        }
        
        // Fuel command to check how many seconds of fuel are remaining
        if (commandLabel.equalsIgnoreCase("fuel")) {
            if (! hasPermission(player, "use")) {
                messagePlayer(player, "permission", null);
                return true;
            }
            if (args.length > 0) { return false; }
        
            if (! core.hasFuelCount(player)) {
                if (hasPermission(player, "nofuel")) {
                    messagePlayer(player, "fuelNA", null);
                    return true;
                }
                else {
                    messagePlayer(player, "fuelStart", null);
                    return true;
                }
            }

            HashMap<String, String> keywords = new HashMap<>();
            Integer flySpeed = Math.round(player.getFlySpeed() * 10);
            Double rawSeconds = core.getFuelCount(player) / flySpeed;
            Integer roundSeconds = Math.round(rawSeconds.longValue());
            keywords.put("%seconds%", roundSeconds.toString());
            keywords.put("%flyspeed%", flySpeed.toString());
            messagePlayer(sender, "fuelMessage", keywords);
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
                core.setFallng(player, true);
                core.setFlying(player, false);
                core.setBrokenCounter(player, getConfig().getDouble("healTime"));
                player.setAllowFlight(false);
                messagePlayer(player, "wBroke", null);
            }
        }
    }

    // Reset flyspeed & message admins for new versions
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        event.getPlayer().setFlySpeed(0.1F);
        
        if (hasPermission(event.getPlayer(), "updates") && updateAvailable != null) {
            String header = getConfig().getString("header");
            String message = header + updateAvailable + " is available for download";
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
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
                messagePlayer(player, "wHealed", null);
            }
        }
    }
}