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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;

public class Core {
    public ConcurrentHashMap<Player, Boolean> flying = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Double> fuel = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Double> broken = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player, Boolean> falling = new ConcurrentHashMap<>();
    public List<String> disabledWorlds;
    public File playersFile;
    public FileConfiguration players;
    public File stringsFile;
    public FileConfiguration strings;

    // Completely remove a player
    public void clearPlayer(Player player) {
        if (flying.containsKey(player)) { flying.remove(player); }
        if (fuel.containsKey(player)) { fuel.remove(player); }
        if (broken.containsKey(player)) { broken.remove(player); }
        if (falling.containsKey(player)) { falling.remove(player); }
    }
    
    // Store a player in players.yml (for players leaving)
    public void storePlayer(Player player) {
        players.createSection(player.getUniqueId().toString());
        if (flying.containsKey(player)) { players.set(player.getUniqueId() + ".flying", flying.get(player).booleanValue()); }
        if (fuel.containsKey(player)) { players.set(player.getUniqueId() + ".fuel", fuel.get(player).intValue()); }
        if (broken.containsKey(player)) { players.set(player.getUniqueId() + ".broken", broken.get(player).intValue()); }
        if (falling.containsKey(player)) { players.set(player.getUniqueId() + ".falling", falling.get(player).booleanValue()); }
        try { players.save(playersFile); } catch (IOException e) { return; }
    }
    
    // Retrieve a player from players.yml (for players joining)
    public void retrievePlayer (Player player) {
        if (players.isConfigurationSection(player.getUniqueId().toString())) {
            ConfigurationSection section = players.getConfigurationSection(player.getUniqueId().toString());

            setFlying(player, section.getBoolean("flying"));
            increaseFuelCount(player, section.getDouble("fuel"));
            if (section.getDouble("broken") > 0.0) { setBrokenCounter(player, section.getDouble("broken")); }
            setFalling(player, section.getBoolean("falling"));
            if (isBroken(player)) { messagePlayer(player, "wResumed", null); }
            
            if (isFlying(player)) {
                player.setAllowFlight(true);
                player.setFlying(true);
                messagePlayer(player, "fResumed", null);
            }

            players.set(player.getUniqueId().toString(), null);
            try { players.save(playersFile); } catch (IOException e) { return; }
        }
    }
    
    // Store all players in players.yml (for onDisable)
    public void storeAll() {
        if (! flying.isEmpty()) {
            Iterator<Player> iter = flying.keySet().iterator();
            while (iter.hasNext()) {
                Player player = (Player)iter.next();
                players.createSection(player.getUniqueId().toString());
                if (flying.containsKey(player)) { players.set(player.getUniqueId() + ".flying", flying.get(player).booleanValue()); }
                if (fuel.containsKey(player)) { players.set(player.getUniqueId() + ".fuel", fuel.get(player).intValue()); }
                if (broken.containsKey(player)) { players.set(player.getUniqueId() + ".broken", broken.get(player).intValue()); }
                if (falling.containsKey(player)) { players.set(player.getUniqueId() + ".falling", falling.get(player).booleanValue()); }
            }
            try { players.save(playersFile); } catch (IOException e) { return; }
        }
    }
    
    // Retrieve all players from players.yml (for onEnable)
    public void retrieveAll() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (players.isConfigurationSection(player.getUniqueId().toString())) {
                ConfigurationSection section = players.getConfigurationSection(player.getUniqueId().toString());

                setFlying(player, section.getBoolean("flying"));
                increaseFuelCount(player, section.getDouble("fuel"));
                if (section.getDouble("broken") > 0.0) { setBrokenCounter(player, section.getDouble("broken")); }
                setFalling(player, section.getBoolean("falling"));
                
                if (isFlying(player)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                
                players.set(player.getUniqueId().toString(), null);
            }
        }

        try { players.save(playersFile); } catch (IOException e) { return; }
    }
    
    // Send a configurable message to a command sender
    public void messagePlayer (CommandSender sender, String type, HashMap<String, String> keywords) {
        if (strings.contains(type)) {

            // Get the header and the string
            String message = strings.getString("header") + strings.getString(type);
            
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
    public void setFalling(Player player, Boolean val) {
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