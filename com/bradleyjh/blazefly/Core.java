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

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

public class Core {
    public ConcurrentHashMap<Player, Boolean> flying = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Double> fuel = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Double> broken = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Boolean> falling = new ConcurrentHashMap<>();
    public List<String> disabledWorlds;
    private Long lastTime;
    
    // Get the time that has passed since the last Timer() ran
    public Double getTimePassed() {
        if (lastTime == null) {
            lastTime = System.currentTimeMillis();
            return 0.500;
        }
        
        Long t = System.currentTimeMillis() - lastTime;
        lastTime = System.currentTimeMillis();
        return t.doubleValue() / 1000;
    }
    
    // Completely remove a player
    public void clearPlayer(Player player) {
        if (flying.containsKey(player)) { flying.remove(player); }
        if (fuel.containsKey(player)) { fuel.remove(player); }
        if (broken.containsKey(player)) { broken.remove(player); }
        if (falling.containsKey(player)) { falling.remove(player); }
    }
    
    // Flying stuff
    public void setFlying(Player player, Boolean val) {
        flying.put(player, val);
    }
    public Boolean isFlying(Player player) {
        if (flying.containsKey(player)) { return flying.get(player); }
        return false;
    }

    // Fuel counter stuff
    public void increaseFuelCount(Player player, Double val) {
        Double fuelVal = 0.0;
        if (fuel.containsKey(player)) { fuelVal = fuel.get(player); }
        fuel.put(player, fuelVal + val);
    }
    public void decreaseFuelCount(Player player, Double val) {
        fuel.put(player, fuel.get(player) - val);
    }
    public Double getFuelCount(Player player) {
        if (fuel.containsKey(player)) { return fuel.get(player); }
        return 0.0;
    }
    public Boolean hasFuelCount(Player player) {
        if (fuel.containsKey(player) && fuel.get(player) > 0) { return true; }
        return false;
    }

    // Falling stuff (Some hacky stuff to prevent occasional damage happening)
    public void setFallng(Player player, Boolean val) {
        falling.put(player, val);
    }
    public Boolean isFalling(Player player) {
        if (falling.containsKey(player)) { return falling.get(player); }
        return false;
    }

    // Broken wings stuff
    public void setBrokenCounter(Player player, Double val) {
        broken.put(player, val);
    }
    public void decreaseBrokenCounter(Player player, Double val) {
        broken.put(player, broken.get(player) - val);
    }
    public Double getBrokenCount(Player player) {
        if (broken.containsKey(player)) { return broken.get(player); }
        return 0.0;
    }
    public Boolean isBroken(Player player) {
        if (broken.containsKey(player)) {return true;}
        return false;
    }
    public void removeBroken(Player player) {
        if (broken.containsKey(player)) { broken.remove(player); }
    }

    // Check if the player has fuel
    public Boolean hasFuel(Player player, String material, Integer subdata) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        ItemStack[] arrayOfItemStack;
        int total = (arrayOfItemStack = contents).length;
        for (int i = 0; i < total; i++) {
            ItemStack stack = arrayOfItemStack[i];
            if (stack == null) { continue; }
            if (stack.getDurability() != subdata) { continue; }
            
            if (stack.getData().getItemType().equals(Material.getMaterial(material.toUpperCase()))) {
                return true;
            }
        }
        return false;
    }
    
    // Remove fuel item from player
    public void removeFuel(Player player, String material, Integer subdata) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        ItemStack[] arrayOfItemStack;
        int total = (arrayOfItemStack = contents).length;
        for (int i = 0; i < total; i++) {
            ItemStack stack = arrayOfItemStack[i];
            if (stack == null) { continue; }
            if (stack.getDurability() != subdata) { continue; }

            if (stack.getType() == Material.getMaterial(material.toUpperCase())) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                }
                else {
                    // Hacky fix for removing off-hand block
                    if (i == 40) {
                        inv.setItemInOffHand(null);
                        break;
                    }
                    
                    // Hacky fix for removing renamed block
                    ItemMeta temp = stack.getItemMeta();
                    temp.setDisplayName("null");
                    stack.setItemMeta(temp);
                    inv.remove(stack);
                }
                break;
            }
        }
    }
}