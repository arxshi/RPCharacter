package me.arahis.rpcharacter.commands;

import me.arahis.rpcharacter.RPCharacterPlugin;
import me.arahis.rpcharacter.database.IDataHandler;
import me.arahis.rpcharacter.database.SavingType;
import me.arahis.rpcharacter.models.Character;
import me.arahis.rpcharacter.models.RPPlayer;
import me.arahis.rpcharacter.utils.Refactor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class CharSetSkinCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            Refactor.sendMessageFromConfig(sender, "only-for-players");
            return true;
        }

        RPCharacterPlugin plugin = RPCharacterPlugin.getPlugin();

        if(args.length < 3) {
            Refactor.sendMessage(sender, String.format(plugin.getConfig().getString("wrong-usage"), command.getUsage()));
            return true;
        }

        IDataHandler handler = plugin.getDataHandler();
        int limit = plugin.getConfig().getInt("limit");

        Player player = (Player) sender;

        if(Refactor.isNotANumber(args[1])) {
            Refactor.sendMessageFromConfig(player, "id-should-be-number");
            return true;
        }

        int id = Integer.parseInt(args[1]);

        // /setskin <format[nick/url]> <id> <nick/url> <classic/slim>

        if(args[0].equals("nick")) {

            IProperty property = plugin.getSkinsRestorerAPI().getSkinData(args[2]);

            if(property == null) {
                // Скин игрока %s не найден!
                Refactor.sendMessage(player, String.format(plugin.getConfig().getString("skin-not-found"), args[2]));
                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                Character character = handler.getCharacter(player, id);

                if(character == null) {
                    // Персонаж с номером %d не найден!
                    Refactor.sendMessage(player, String.format(plugin.getConfig().getString("char-not-found"), id));
                    return;
                }

                character.setPropertyName(property.getName());
                character.setPropertyValue(property.getValue());
                character.setPropertySignature(property.getSignature());

                try {
                    handler.saveCharacter(character, SavingType.UPDATE);
                } catch (SQLException e) {
                    Refactor.sendFormattedWarn("%s's character #%d %s wasn't successfully updated", character.getOwnerName(), character.getCharId(), character.getOwnerName());
                    Refactor.sendMessageFromConfig(player, "db-con-error");
                    return;
                }
                Refactor.sendFormattedInfo("Skin updated by nickname to %s's skin", args[2]);

                // Скин персонажа #%d %s был изменен по нику %s
                Refactor.sendMessage(player, String.format(plugin.getConfig().getString("skin-updated-by-nick"), id, character.getCharName(), args[2]));

                RPPlayer rpPlayer = handler.getRPPlayer(player);
                if(rpPlayer.getSelectedChar() == id) {
                    plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), property);
                }

            });

        }

        if (args[0].equals("url") && (args[3].equals("classic") || args[3].equals("slim"))) {

            try {
                IProperty property = plugin.getSkinsRestorerAPI().genSkinUrl(args[2], SkinVariant.valueOf(args[3].toUpperCase()));

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                    Character character = handler.getCharacter(player, id);

                    character.setPropertyName(property.getName());
                    character.setPropertyValue(property.getValue());
                    character.setPropertySignature(property.getSignature());

                    try {
                        handler.saveCharacter(character, SavingType.UPDATE);
                    } catch (SQLException e) {
                        Refactor.sendFormattedWarn("%s's character #%d %s wasn't successfully updated", character.getOwnerName(), character.getCharId(), character.getOwnerName());
                        Refactor.sendMessageFromConfig(player, "db-con-error");
                        return;
                    }
                    Refactor.sendInfo("Skin updated by url: " + args[2]);
                    Refactor.sendInfo("Skin variant: " + args[3].toUpperCase());

                    // Скин персонажа #%d %s был изменен по ссылке
                    // url
                    Refactor.sendMessage(player, String.format(plugin.getConfig().getString("skin-updated-by-url"), id, character.getCharName()));
                    player.spigot().sendMessage(new ComponentBuilder(args[2])
                            .event(new ClickEvent(ClickEvent.Action.OPEN_URL, args[2]))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder("Нажмите, чтобы открыть ссылку").create()))
                            .underlined(true)
                            .create()
                    );

                    RPPlayer rpPlayer = handler.getRPPlayer(player);
                    if(rpPlayer.getSelectedChar() == id) {
                        plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), property);
                    }

                });

            } catch (SkinRequestException e) {
                Refactor.sendMessage(player, "Ошибка SkinsRestorerAPI! Попробуйте снова позже.");
                Refactor.sendMessage(player, "Если ошибка осталась, создайте тикет в поддержке!");
                e.printStackTrace();
                return true;
            }

        }

        return true;
    }
}
