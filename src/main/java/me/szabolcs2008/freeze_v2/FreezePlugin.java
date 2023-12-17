package me.szabolcs2008.freeze_v2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FreezePlugin extends JavaPlugin implements Listener {

    ArrayList<String> frozen;
    String prefix;
    List<String> chatMessage;
    List<String> enabledCommands;
    String punishCommand;



    public FreezePlugin() {
        this.frozen = new ArrayList<String>();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefix = this.getConfig().getString("prefix");
        chatMessage = getConfig().getStringList("chatMessage");
        enabledCommands = getConfig().getStringList("enabledCommands");
        getLogger().info("Freeze betöltve!");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("freeze").setExecutor(this);
        punishCommand = getConfig().getString("punishCommand");
    }

    @Override
    public void onDisable() {
        getLogger().info("Freeze kikapcsolva!");
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("freeze").setExecutor(this);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        if (this.frozen.contains(player.getName()) && event.getMessage().startsWith("/") && !player.hasPermission("cfreeze.bypass")) {
            for (String item : enabledCommands){
                if (event.getMessage().toLowerCase().startsWith(item)) {
                    return;
                }
            }
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&7Nem használhatsz parancsokat lefagyasztva!"));
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        if (this.frozen.contains(player.getName())) {
            event.setTo(event.getFrom());
            event.setCancelled(true);
            for (final String message : this.chatMessage) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            final Player d = (Player)event.getDamager();
            if (this.frozen.contains(d.getName())) {
                event.setCancelled(true);
                d.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "Nem üthetsz másokat lefagyasztva!"));
            }
            else if (event.getEntity() instanceof Player) {
                final Player v = (Player)event.getEntity();
                if (this.frozen.contains(v.getName())) {
                    event.setCancelled(true);
                    d.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&7Ez a játékos le van fagyasztva!"));
                }
            }
        }
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent event) {
        final Player p = event.getPlayer();
        if (this.frozen.contains(p.getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&7Le vagy fagyasztva!"));
        }
    }

    @EventHandler
    public void onPlace(final BlockPlaceEvent event) {
        final Player p = event.getPlayer();
        if (this.frozen.contains(p.getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&7Le vagy fagyasztva!"));
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player p = event.getPlayer();
        if (this.frozen.contains(p.getName())) {
            this.frozen.remove(p.getName());
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), punishCommand.replace("{player}", p.getName()));
            for (final Player players : Bukkit.getOnlinePlayers()) {
                if (players.hasPermission("cfreeze.freeze")) {
                    players.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&9" + p.getName() + " &7Kilépett lefagyasztva és ki lett tiltva"));
                }
            }
        }
    }


    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("freeze")) {
            if (sender.hasPermission("cfreeze.freeze")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&7/freeze <név>"));
                    return true;
                }
                else {
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (sender.hasPermission("cfreeze.admin")) {
                            reloadConfig();
                            prefix = getConfig().getString("prefix");
                            chatMessage = getConfig().getStringList("chatMessage");
                            enabledCommands = getConfig().getStringList("enabledCommands");
                            punishCommand = getConfig().getString("punishCommand");
                            getLogger().info("Freeze újratöltve");
                            sender.sendMessage(prefix + "&7config.yml újratöltve!");
                        }
                        else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&cEhhez nincs jogod!"));
                        }
                    }
                    else {
                        final Player target = Bukkit.getServer().getPlayer(args[0]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&7Ez a játékos nem létezik!"));
                            return true;
                        }
                        if (!target.hasPermission("cfreeze.bypass")) {
                            if (this.frozen.contains(target.getName())) {
                                this.frozen.remove(target.getName());
                                target.removePotionEffect(PotionEffectType.BLINDNESS);
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + target.getName() + " &7feloldva!"));
                                return true;
                            }
                            this.frozen.add(target.getName());
                            final Player t = target.getPlayer();
                            t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 9999999, 255));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + target.getName() + " &7lefagyasztva!"));
                            return true;
                        }
                        else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&cEzt a játékost nem fagyaszthatod le!"));
                        }
                    }
                }
            }
            else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.prefix + "&cEhhez nincs jogod!"));
            }
        }
        return true;
    }
}
