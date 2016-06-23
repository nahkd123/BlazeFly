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
    
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import java.util.List;
import java.util.concurrent.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class Main extends JavaPlugin implements Listener {
    ConcurrentHashMap<Player, Integer> flyers = new ConcurrentHashMap<>();
    ConcurrentHashMap<Player, Integer> fallers = new ConcurrentHashMap<>();
    ConcurrentHashMap<Player, Integer> broken = new ConcurrentHashMap<>();
    ConcurrentHashMap<Player, Integer> disabled = new ConcurrentHashMap<>();
    List<String> disabledWorlds;
    String messageHeader = ChatColor.BLUE + "[BlazeFly] " + ChatColor.WHITE;
    Long lastUpdate;
    Integer timerLag;

    public void onEnable() {
        saveDefaultConfig();
        this.disabledWorlds = getConfig().getStringList("disabledWorlds");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Timer(this), 20L, 20L);
        for(Player player : getServer().getOnlinePlayers()) {
            player.setFlySpeed(0.1F);
            if (!player.hasPermission("blazefly.nofuel")) {
                if (player.hasPermission("blazefly.vip")) {
                    this.flyers.put(player, Integer.valueOf(getConfig().getInt("VIPTime")));
                }
                else {
                    this.flyers.put(player, Integer.valueOf(getConfig().getInt("fuelTime")));
                }
            }
        }
    }

    // Replaces the deprecated getServer().getPlayer()
    public Player getPlayer(String name) {
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name))
                return p;
        }
        return null;
    }

    // Do checks for fuel
    public int getFuel(Player player) {
        if (flyers.containsKey(player) && flyers.get(player) > 1) {return 1;}
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        int badFuel = 0;
        ItemStack[] arrayOfItemStack1;
        int j = (arrayOfItemStack1 = contents).length;
        for (int i = 0; i < j; i++) {
            ItemStack f = arrayOfItemStack1[i];
            if (f != null) {
                if (player.hasPermission("blazefly.vip")) {
                    if (f.getData().getItemType().equals(Material.getMaterial(getConfig().getString("VIPBlock")))) {
                        if (f.getItemMeta().hasDisplayName()) { badFuel = 1; }
                        if (!f.getItemMeta().hasDisplayName()) { return 1; }
                    }
                }
                else if (f.getData().getItemType().equals(Material.getMaterial(getConfig().getString("fuelBlock")))) {
                    if (f.getItemMeta().hasDisplayName()) { badFuel = 1; }
                    if (!f.getItemMeta().hasDisplayName()) { return 1; }
                }
            }
        }
        if (badFuel == 1) { return 2; }
        return 0;
    }

    // Process incoming commands
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // Reload the configuration file
        if (commandLabel.equalsIgnoreCase("bfreload")) {
            if (!sender.hasPermission("blazefly.reload")) {
                if (!getConfig().getString("permission").isEmpty()) {
                    sender.sendMessage(this.messageHeader + getConfig().getString("permission"));
                }
                return true;
            }
            reloadConfig();
            this.disabledWorlds = getConfig().getStringList("disabledWorlds");
            if (!getConfig().getString("reload").isEmpty()) {
                sender.sendMessage(this.messageHeader + getConfig().getString("reload"));
            }
            return true;
        }

        // Disable a given users flight
        if (commandLabel.equalsIgnoreCase("flyoff")) {
            if (sender.hasPermission("blazefly.flyoff")) {
                if (args.length != 1) {
                    return false;
                }

                Player target = getPlayer(args[0]);

                if ((target == null) || (!target.isOnline())) {
                    sender.sendMessage(this.messageHeader + "No such player online");
                    return true;
                }
                if (!target.getAllowFlight()) {
                    sender.sendMessage(this.messageHeader + args[0] + " isn't currently flying");
                    return true;
                }

                this.fallers.put(target, Integer.valueOf(2));
                this.disabled.put(target, 1);
                sender.sendMessage(this.messageHeader + "Flight has been disabled for " + args[0]);

                if (!getConfig().getString("flyoff").isEmpty()) {
                    target.sendMessage(this.messageHeader + getConfig().getString("flyoff"));
                }
                return true;
            }

            if (!getConfig().getString("permission").isEmpty()) {
                sender.sendMessage(this.messageHeader + getConfig().getString("permission"));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot fly");
            return true;
        }
        Player player = (Player)sender;

        if ((this.disabledWorlds.contains(player.getWorld().getName())) && (!player.hasPermission("blazefly.anyworld"))) {
            if (!getConfig().getString("disabled").isEmpty()) {
                player.sendMessage(this.messageHeader + getConfig().getString("disabled"));
            }
            return true;
        }

        // Enable flight
        if (((commandLabel.equalsIgnoreCase("fly")) || (commandLabel.equalsIgnoreCase("bfly"))) && (args.length == 0)) {
            if (!player.hasPermission("blazefly.use")) {
                if (!getConfig().getString("permission").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("permission"));
                }
                return true;
            }

            // Turn flight off
            if (player.getAllowFlight()) {
                this.fallers.put(player, Integer.valueOf(2));
                this.disabled.put(player, 1);
                player.setAllowFlight(false);
                if (!getConfig().getString("fDisabled").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("fDisabled"));
                }
                return true;
            }
            // Turn flight on
            else {
                if (this.broken.containsKey(player)) {
                    if (!getConfig().getString("wBroken").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("wBroken"));
                    }
                    return true;
                }
                if (((getFuel(player) == 0 ? 1 : 0) & (player.hasPermission("blazefly.nofuel") ? 0 : 1)) != 0) {
                    if (!getConfig().getString("fRequired").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fRequired"));
                    }
                    return true;
                }
                if (((getFuel(player) == 2 ? 1 : 0) & (player.hasPermission("blazefly.nofuel") ? 0 : 1)) != 0) {
                    if (!getConfig().getString("fBad").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fBad"));
                    }
                    return true;
                }

                if (player.hasPermission("blazefly.nofuel")) {
                    if (!getConfig().getString("fEnabled").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fEnabled"));
                    }
                    this.flyers.put(player, Integer.valueOf(-1));
                    player.setAllowFlight(true);
                }
                else {
                    if (! flyers.containsKey(player)) {
                        PlayerInventory inv = player.getInventory();
                        if (player.hasPermission("blazefly.vip")) {
                            this.flyers.put(player, Integer.valueOf(getConfig().getInt("VIPTime")));
                            inv.removeItem(new ItemStack[] { new ItemStack(Material.getMaterial(getConfig().getString("VIPBlock")), 1) });
                        }
                        else {
                            this.flyers.put(player, Integer.valueOf(getConfig().getInt("fuelTime")));
                            inv.removeItem(new ItemStack[] { new ItemStack(Material.getMaterial(getConfig().getString("fuelBlock")), 1) });
                        }
                    }

                    if (!getConfig().getString("fEnabled").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fEnabled"));
                    }
                    if (this.disabled.containsKey(player)) { this.disabled.remove(player); }
                    player.setAllowFlight(true);

                    if ((getFuel(player) == 0) && (!getConfig().getString("fLast").isEmpty())) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fLast"));
                    }
                }
                return true;
            }
        }

        // Change your speed of flight (1x, 2x or 4x speed)
        if (commandLabel.equalsIgnoreCase("flyspeed")) {
            if (!player.hasPermission("blazefly.flyspeed")) {
                if (!getConfig().getString("permission").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("permission"));
                }
                return true;
            }
            if (getConfig().getString("allowSpeed").equalsIgnoreCase("false")) {
                if (!getConfig().getString("fsDisabled").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("fsDisabled"));
                }
                return true;
            }
            if (!player.getAllowFlight()) {
                if (!getConfig().getString("fsFlight").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("fsFlight"));
                }
                return true;
            }

            if (args.length == 0) {
                if (player.getFlySpeed() == 0.1F) {
                    if (!getConfig().getString("fsIs1").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs1"));
                    }
                    return true;
                }
                if (player.getFlySpeed() == 0.2F) {
                    if (!getConfig().getString("fsIs2").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs2"));
                    }
                    return true;
                }
                if (player.getFlySpeed() == 0.4F) {
                    if (!getConfig().getString("fsIs4").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs4"));
                    }
                    return true;
                }
            }
            else {
                Float newSpeed = Float.valueOf(args[0]);
                Float oldSpeed = player.getFlySpeed() * 10;
                if (args[0].equals("1")) {
                    if (player.getFlySpeed() != 0.1F) {
                        player.setFlySpeed(0.1F);
                        if (!getConfig().getString("fsNow1").isEmpty()) { player.sendMessage(this.messageHeader + getConfig().getString("fsNow1")); }
                        if ((!player.hasPermission("blazefly.nofuel") & getConfig().getString("speedFuel").equalsIgnoreCase("true"))) {
                            if (oldSpeed < newSpeed) {
                                this.flyers.put(player, Math.round(this.flyers.get(player) / newSpeed));
                            }
                            else {
                                this.flyers.put(player, Math.round(this.flyers.get(player) * oldSpeed));
                            }
                        }
                        return true;
                    }

                    if (!getConfig().getString("fsIs1").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs1"));
                    }
                    return true;
                }

                if (args[0].equals("2")) {
                    if (player.getFlySpeed() != 0.2F) {
                        player.setFlySpeed(0.2F);
                        if (!getConfig().getString("fsNow2").isEmpty()) {
                            player.sendMessage(this.messageHeader + getConfig().getString("fsNow2"));
                        }
                        if ((!player.hasPermission("blazefly.nofuel") & getConfig().getString("speedFuel").equalsIgnoreCase("true"))) {
                            if (oldSpeed < newSpeed) {
                                this.flyers.put(player, Math.round(this.flyers.get(player) / newSpeed));
                            }
                            else {
                                this.flyers.put(player, Math.round(this.flyers.get(player) * oldSpeed));
                            }
                        }
                        return true;
                    }

                    if (!getConfig().getString("fsIs2").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs2"));
                    }
                    return true;
                }

                if (args[0].equals("4")) {
                    if (player.getFlySpeed() != 0.4F) {
                        player.setFlySpeed(0.4F);
                        if (!getConfig().getString("fsNow4").isEmpty()) {
                            player.sendMessage(this.messageHeader + getConfig().getString("fsNow4"));
                        }
                        if ((!player.hasPermission("blazefly.nofuel") & getConfig().getString("speedFuel").equalsIgnoreCase("true"))) {
                            if (oldSpeed < newSpeed) {
                                this.flyers.put(player, Math.round(this.flyers.get(player) / newSpeed));
                            }
                            else {
                                this.flyers.put(player, Math.round(this.flyers.get(player) * oldSpeed));
                            }
                        }
                        return true;
                    }

                    if (!getConfig().getString("fsIs4").isEmpty()) {
                        player.sendMessage(this.messageHeader + getConfig().getString("fsIs4"));
                    }
                    return true;
                }

                if (!getConfig().getString("fsOptions").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("fsOptions"));
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if ((e.getEntity() instanceof Player)) {
            Player player = (Player)e.getEntity();

            // Protect players falling due to running out of fuel, if enabled
            if ((e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) && (this.fallers.containsKey(player))) {
                if (getConfig().getBoolean("fallProtection")) {
                    e.setCancelled(true);
                    return;
                }
            }

            // Ignore these types of damage
            if (e.getCause().equals(EntityDamageEvent.DamageCause.DROWNING)) { return; }
            if (e.getCause().equals(EntityDamageEvent.DamageCause.STARVATION)) { return; }
            
            // PvP stuff, break wings (disallow flight) if player takes damage
            if ((getConfig().getBoolean("breakableWings")) && (! player.hasPermission("blazefly.superwings")) && (this.flyers.containsKey(player))) {
                this.fallers.put(player, Integer.valueOf(2));
                this.broken.put(player, Integer.valueOf(getConfig().getInt("healTime")));
                player.setAllowFlight(false);
                if (!getConfig().getString("wBroke").isEmpty()) {
                    player.sendMessage(this.messageHeader + getConfig().getString("wBroke"));
                }
            }
        }
    }
}