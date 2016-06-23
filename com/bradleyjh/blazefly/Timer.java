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

import java.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.Location;

public class Timer implements Runnable {
    private Main plugin;
    Integer lagAdjustment = 0;

    public Timer(Main plugin) {
        this.plugin = plugin;
        plugin.timerLag = 0;
        plugin.lastUpdate = System.currentTimeMillis();
    }

    public void run() {
        // Timer lag compensation stuff
        plugin.timerLag = plugin.timerLag + (Math.round(System.currentTimeMillis() - plugin.lastUpdate)) - 1000;
        plugin.lastUpdate = System.currentTimeMillis();
        if (plugin.timerLag >= 1000) {
            lagAdjustment = 1;
            plugin.timerLag = plugin.timerLag - 1000;
        }

        // Check if any of those with flight disabled have left the server and remove them
        if (this.plugin.disabled != null) {
            Iterator<Player> iter = this.plugin.disabled.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                if (!player.isOnline()) { this.plugin.disabled.remove(player); }
            }
        }

        // Update those who are falling due to running out of fuel and need to be protected from damage
        if (this.plugin.fallers != null) {
            Iterator<Player> iter = this.plugin.fallers.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                Integer val = (Integer)this.plugin.fallers.get(player);
                Location block = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY() - 1, player.getLocation().getBlockZ());

                if ((val.intValue() == 2) && (!block.getBlock().getType().equals(Material.AIR))) {
                    this.plugin.fallers.put(player, Integer.valueOf(1));
                }
                if (val.intValue() == 1) {
                    if (!block.getBlock().getType().equals(Material.AIR)) {
                        this.plugin.fallers.remove(player);
                    }
                    else {
                        this.plugin.fallers.put(player, Integer.valueOf(2));
                    }
                }
            }
        }

        // Update those who have "broken wings" and cannot fly until "healed"
        if (this.plugin.broken != null) {
            Iterator<Player> iter = this.plugin.broken.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                Integer val = (Integer)this.plugin.broken.get(player);

                if (!player.isOnline()) {
                    this.plugin.broken.remove(player);
                }
                else if (val.intValue() == 0) {
                    this.plugin.broken.remove(player);
                    if (!this.plugin.getConfig().getString("wBroken").isEmpty()) {
                        if (! plugin.disabled.containsKey(player)) { player.setAllowFlight(true); }
                        player.sendMessage(this.plugin.messageHeader + this.plugin.getConfig().getString("wHealed"));
                    }
                }
                else {
                    this.plugin.broken.put(player, Integer.valueOf(val.intValue() - (1 + lagAdjustment)));
                }
            }
        }

        // Update the flight counter to take more fuel or give the warning fuel is about to run out
        if (this.plugin.flyers != null) {
            Iterator<Player> iter = this.plugin.flyers.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                Integer val = (Integer)this.plugin.flyers.get(player);

                // They're offline
                if (!player.isOnline()) {
                    this.plugin.flyers.remove(player);
                }
                // They moved to a world where flight is disable and don't have the anyworld permission
                else if ((this.plugin.disabledWorlds.contains(player.getWorld().getName())) && (!player.hasPermission("blazefly.anyworld"))) {
                    this.plugin.fallers.put(player, Integer.valueOf(2));
                    this.plugin.flyers.remove(player);
                    player.setFlySpeed(0.1F);
                    player.setAllowFlight(false);
                    if (!this.plugin.getConfig().getString("disabled").isEmpty()) {
                        player.sendMessage(this.plugin.messageHeader + this.plugin.getConfig().getString("disabled"));
                    }
                }
                // They're normal or VIP (op is -1)
                else if (val.intValue() != -1) {
                    PlayerInventory inv = player.getInventory();
                    if (val.intValue() <= 1) {
                        if (this.plugin.getFuel(player) == 1) {
                            float flySpeed = player.getFlySpeed() * 10.0F;
                            if (this.plugin.getConfig().getString("speedFuel") != "true") { flySpeed = 1.0F; }
                            if (player.hasPermission("blazefly.vip")) {
                                this.plugin.flyers.put(player, Integer.valueOf(this.plugin.getConfig().getInt("VIPTime") / (int)flySpeed));
                                inv.removeItem(new ItemStack[] { new ItemStack(Material.getMaterial(this.plugin.getConfig().getString("VIPBlock")), 1) });
                            }
                            else {
                                this.plugin.flyers.put(player, Integer.valueOf(this.plugin.getConfig().getInt("fuelTime") / (int)flySpeed));
                                inv.removeItem(new ItemStack[] { new ItemStack(Material.getMaterial(this.plugin.getConfig().getString("fuelBlock")), 1) });
                            }
                  
                            if ((this.plugin.getFuel(player) != 1) && (!this.plugin.getConfig().getString("fLast").isEmpty())) {
                                player.sendMessage(this.plugin.messageHeader + this.plugin.getConfig().getString("fLast"));
                            }
                        }
                        else {
                            if (!this.plugin.getConfig().getString("fOut").isEmpty()) {
                                player.sendMessage(this.plugin.messageHeader + this.plugin.getConfig().getString("fOut"));
                            }
                            this.plugin.fallers.put(player, Integer.valueOf(1));
                            this.plugin.flyers.remove(player);
                            player.setAllowFlight(false);
                        }
                    }
                    // Update the players remaining time
                    else {
                        if (plugin.broken.containsKey(player) || plugin.disabled.containsKey(player)) { continue; }
                        this.plugin.flyers.put(player, Integer.valueOf(val.intValue() - (1 + lagAdjustment)));
                        if ((((this.plugin.getFuel(player) != 1 ? 1 : 0) & (val.intValue() - 1 == 10 ? 1 : 0)) != 0) && 
                        (!this.plugin.getConfig().getString("fLow").isEmpty())) {
                            player.sendMessage(this.plugin.messageHeader + this.plugin.getConfig().getString("fLow"));
                        }
                    }
                }
            }
        }
        lagAdjustment = 0;
    }
}