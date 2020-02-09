package net.okocraft.wgguiextras;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import com.mirotcz.wg_gui.events.RegionBoundsModifyEvent;
import com.mirotcz.wg_gui.events.RegionCreateEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.config.WorldConfiguration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class WGGUIExtras extends JavaPlugin implements Listener {

	FileConfiguration config;
	FileConfiguration defaultConfig;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		config = getConfig();
		defaultConfig = getDefaultConfig();

		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Listener) this);
	};

	@EventHandler
	public void onRegionCreateWithWGGUI(RegionCreateEvent event) {
        World world = BukkitAdapter.adapt(event.getWorld());
		WorldConfiguration wgConfig = WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(world);

		if (!checkRegionCountAndSendMessage(event.getPlayer(), world, wgConfig.maxRegionCountPerPlayer)) {
			event.setCancelled(true);
			return;
		}
        
		Region selection = event.getSelection();
		if (!(selection instanceof CuboidRegion)) {
			return;
		}

		if (!expand(selection) || checkVolumeAndSendMessage(selection, wgConfig.maxClaimVolume, event.getPlayer())) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onRegionModifiedWithWGGUI(RegionBoundsModifyEvent event) {
		World world = BukkitAdapter.adapt(event.getWorld());
		WorldConfiguration wgConfig = WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(world);

		Region selection = event.getNewSelection();
		if (!(selection instanceof CuboidRegion)) {
			return;
		}

		if (!expand(selection) || checkVolumeAndSendMessage(selection, wgConfig.maxClaimVolume, event.getPlayer())) {
			event.setCancelled(true);
			return;
		}
	}

	private boolean expand(Region region) {
		try {
			region.expand(BlockVector3.at(0, 255, 0), BlockVector3.at(0, -255, 0));
			return true;
		} catch (RegionOperationException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean checkRegionCountAndSendMessage(Player player, World world, int maxCount) {
		long regionCount = WorldGuard.getInstance().getPlatform().getRegionContainer().get(world).getRegions().values()
				.stream().filter(region -> region.getOwners().contains(player.getUniqueId())).count();
        if (maxCount <= regionCount) {
			player.sendMessage(getMessage("too-many-regions").replaceAll("%max-count%", String.valueOf(maxCount)));
            return false;
        }
		return true;
	}

	private boolean checkVolumeAndSendMessage(Region region, int maxVolume, Player player) {
        if (region.getWidth() * region.getLength() * region.getHeight() > maxVolume) {
            player.sendMessage(getMessage("too-large-region").replaceAll("%max-volume%", String.valueOf(maxVolume)));
			return false;		
		}
		return true;
	}
	
    private FileConfiguration getDefaultConfig() {
        InputStream is = Objects.requireNonNull(getResource("config.yml"),
                "Jar do not have config.yml. what happened?");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(is));
    }

    private String getMessage(String key) {
        String fullKey = "messages." + key;
        return ChatColor.translateAlternateColorCodes('&',
                config.getString(fullKey, defaultConfig.getString(fullKey, fullKey)));
    }
}
