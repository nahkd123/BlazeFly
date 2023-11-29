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
	// TODO store all of these inside PlayerData or something
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
		// No need to check containsKey here; remove() will do nothing if not yet existed.
		flying.remove(player);
		fuel.remove(player);
		broken.remove(player);
		falling.remove(player);
	}

	// Store a player in players.yml (for players leaving)
	public void storePlayer(Player player) {
		players.createSection(player.getUniqueId().toString());
		if (flying.containsKey(player)) players.set(player.getUniqueId() + ".flying", flying.get(player).booleanValue());
		if (fuel.containsKey(player)) players.set(player.getUniqueId() + ".fuel", fuel.get(player).intValue());
		if (broken.containsKey(player)) players.set(player.getUniqueId() + ".broken", broken.get(player).intValue());
		if (falling.containsKey(player)) players.set(player.getUniqueId() + ".falling", falling.get(player).booleanValue());

		try {
			players.save(playersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Retrieve a player from players.yml (for players joining)
	public void retrievePlayer(Player player) {
		if (players.isConfigurationSection(player.getUniqueId().toString())) {
			ConfigurationSection section = players.getConfigurationSection(player.getUniqueId().toString());

			setFlying(player, section.getBoolean("flying"));
			increaseFuelCount(player, section.getDouble("fuel"));
			if (section.getDouble("broken") > 0.0) {
				setBrokenCounter(player, section.getDouble("broken"));
			}
			setFalling(player, section.getBoolean("falling"));
			if (isBroken(player)) {
				messagePlayer(player, "wResumed", null);
			}

			if (isFlying(player)) {
				player.setAllowFlight(true);
				player.setFlying(true);
				messagePlayer(player, "fResumed", null);
			}

			players.set(player.getUniqueId().toString(), null);
			try {
				players.save(playersFile);
			} catch (IOException e) {
				return;
			}
		}
	}

	// Store all players in players.yml (for onDisable)
	public void storeAll() {
		if (!flying.isEmpty()) {
			Iterator<Player> iter = flying.keySet().iterator();

			while (iter.hasNext()) {
				Player player = (Player) iter.next();
				players.createSection(player.getUniqueId().toString());
				if (flying.containsKey(player)) players.set(player.getUniqueId() + ".flying", flying.get(player).booleanValue());
				if (fuel.containsKey(player)) players.set(player.getUniqueId() + ".fuel", fuel.get(player).intValue());
				if (broken.containsKey(player)) players.set(player.getUniqueId() + ".broken", broken.get(player).intValue());
				if (falling.containsKey(player)) players.set(player.getUniqueId() + ".falling", falling.get(player).booleanValue());
			}

			try {
				players.save(playersFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Retrieve all players from players.yml (for onEnable)
	public void retrieveAll() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (players.isConfigurationSection(player.getUniqueId().toString())) {
				ConfigurationSection section = players.getConfigurationSection(player.getUniqueId().toString());

				setFlying(player, section.getBoolean("flying"));
				increaseFuelCount(player, section.getDouble("fuel"));
				if (section.getDouble("broken") > 0.0) setBrokenCounter(player, section.getDouble("broken"));
				setFalling(player, section.getBoolean("falling"));

				if (isFlying(player)) {
					player.setAllowFlight(true);
					player.setFlying(true);
				}

				players.set(player.getUniqueId().toString(), null);
			}
		}

		try {
			players.save(playersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send a configurable message to a command sender
	public void messagePlayer(CommandSender sender, String type, HashMap<String, String> keywords) {
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
	public void setFlying(Player player, boolean val) {
		flying.put(player, val);
	}

	public boolean isFlying(Player player) {
		return flying.getOrDefault(player, false);
	}

	// Fuel counter stuff
	public void increaseFuelCount(Player player, double val) {
		double fuelVal = fuel.getOrDefault(player, 0.0);
		fuel.put(player, fuelVal + val);
	}

	public void decreaseFuelCount(Player player, double val) {
		fuel.put(player, fuel.get(player) - val);
	}

	public double getFuelCount(Player player) {
		return fuel.getOrDefault(player, 0.0);
	}

	public boolean hasFuelCount(Player player) {
		return fuel.containsKey(player) && fuel.get(player) > 0;
	}

	// Falling stuff (Some hacky stuff to prevent occasional damage happening)
	public void setFalling(Player player, boolean val) {
		falling.put(player, val);
	}

	public boolean isFalling(Player player) {
		return falling.getOrDefault(player, false);
	}

	// Broken wings stuff
	public void setBrokenCounter(Player player, double val) {
		broken.put(player, val);
	}

	public void decreaseBrokenCounter(Player player, double val) {
		broken.put(player, broken.getOrDefault(player, 0.0) - val);
	}

	public double getBrokenCount(Player player) {
		return broken.getOrDefault(player, 0.0);
	}

	public boolean isBroken(Player player) {
		return broken.containsKey(player);
	}

	public void removeBroken(Player player) {
		broken.remove(player);
	}

	// Check if the player has fuel
	public Boolean hasFuel(Player player, String material, Integer subdata) {
		PlayerInventory inv = player.getInventory();
		ItemStack[] contents = inv.getContents();
		ItemStack[] arrayOfItemStack;
		int total = (arrayOfItemStack = contents).length;

		for (int i = 0; i < total; i++) {
			ItemStack stack = arrayOfItemStack[i];
			if (stack == null) continue;
			if (stack.getDurability() != subdata) continue;
			if (stack.getData().getItemType().equals(Material.getMaterial(material.toUpperCase()))) return true;
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
			if (stack == null) continue;
			if (stack.getDurability() != subdata) continue;

			if (stack.getType() == Material.getMaterial(material.toUpperCase())) {
				if (stack.getAmount() > 1) {
					stack.setAmount(stack.getAmount() - 1);
				} else {
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