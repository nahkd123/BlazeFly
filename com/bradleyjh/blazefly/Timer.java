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

import java.util.Iterator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class Timer implements Runnable {
    private Main main;
    public Timer(Main plugin) { main = plugin; }

    public void run() {
        if (! main.core.flying.isEmpty()) {
            Iterator<Player> iter = main.core.flying.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                Double timePassed = main.core.getTimePassed();
                
                // If they went offline sanitize stuff
                if (! player.isOnline()) {
                    main.core.clearPlayer(player);
                }
                
                // Ensure flight is always in the correct state (changing worlds etc)
                if (main.core.isFlying(player)) { player.setAllowFlight(true); }
                else { player.setAllowFlight(false); }

                // They moved to a world where flight is disable and don't have the anyworld permission
                if (main.core.isFlying(player) && main.core.disabledWorlds.contains(player.getWorld().getName()) && ! main.hasPermission(player, "anyworld")) {
                    main.core.setFallng(player, true);
                    main.core.setFlying(player, false);
                    player.setAllowFlight(false);
                    main.messagePlayer(player, "disabled", null);
                }
                
                // Check if they have "landed" to disable fall protection
                if (main.core.isFalling(player)) {
                    Location block = new Location(player.getWorld(), player.getLocation().getBlockX(), Math.ceil(player.getLocation().getY()) - 1, player.getLocation().getBlockZ());
                    if (! block.getBlock().getType().equals(Material.AIR)) {
                        main.core.setFallng(player, false);
                    }
                }
                
                // Check if the players "wings" have "healed"
                if (main.core.isBroken(player)) {
                    if (main.core.getBrokenCount(player) > 1) {
                        main.core.decreaseBrokenCounter(player, timePassed);
                    }
                    else {
                        main.core.removeBroken(player);
                        player.setAllowFlight(true);
                        main.core.setFlying(player, true);
                        main.messagePlayer(player, "wHealed", null);
                    }  
                }

                // Check and update the players fuel counter
                if (main.core.isFlying(player) && ! main.hasPermission(player, "nofuel")) {
                    if (! main.core.hasFuelCount(player)) {
                            if (main.core.hasFuel(player, main.fuelBlock(player), main.fuelSubdata(player))) {
                                main.core.increaseFuelCount(player, main.fuelTime(player));
                                main.core.removeFuel(player, main.fuelBlock(player), main.fuelSubdata(player));
                                if (! main.core.hasFuel(player, main.fuelBlock(player), main.fuelSubdata(player))) { main.messagePlayer(player, "fLast", null); }
                            }
                            else {
                                main.messagePlayer(player, "fOut", null);
                                main.core.setFallng(player, true);
                                main.core.setFlying(player, false);
                                player.setAllowFlight(false);
                            }
                    }
                    // Update the players remaining time
                    else {
                        if (! main.core.isFlying(player)) { continue; } // If they aren't flying don't use fuel
                        Double fuelMultiplier = 1.0;
                        Location block = new Location(player.getWorld(), player.getLocation().getBlockX(), Math.ceil(player.getLocation().getY()) - 1, player.getLocation().getBlockZ());
                        if (! block.getBlock().getType().equals(Material.AIR)) {
                            fuelMultiplier = main.getConfig().getDouble("groundFuel");
                        }
                        else {
                            if (main.getConfig().getBoolean("speedFuel")) {
                                Float f = player.getFlySpeed();
                                fuelMultiplier = f.doubleValue() * 10;
                            }
                        }
                        
                        main.core.decreaseFuelCount(player, (timePassed * fuelMultiplier));
                    }
                }
            } 
        }
    }
}