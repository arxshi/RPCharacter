package me.arahis.rpcharacter.listeners;

import me.arahis.rpcharacter.RPCharacterPlugin;
import me.arahis.rpcharacter.database.IDataHandler;
import me.arahis.rpcharacter.database.SavingType;
import me.arahis.rpcharacter.models.Character;
import me.arahis.rpcharacter.models.RPPlayer;
import me.arahis.rpcharacter.utils.Refactor;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class JoinListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        RPCharacterPlugin plugin = RPCharacterPlugin.getPlugin();
        IDataHandler handler = plugin.getDataHandler();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            if (handler.getRPPlayer(player) == null) {
                try {
                    handler.saveRPPlayer(new RPPlayer(player.getUniqueId().toString(), player.getName(), 1, 1), SavingType.SAVE);
                } catch (SQLException e) {
                    Refactor.sendFormattedWarn("RPPlayer %s wasn't saved successfully", player.getName());
                    e.printStackTrace();
                }
            } else {
                RPPlayer rpPlayer = handler.getRPPlayer(player);
                Character character = handler.getCharacter(player, rpPlayer.getSelectedChar());
                Refactor.setCharacterToPlayer(player, character);
            }

            IProperty property = plugin.getSkinsRestorerAPI().getSkinData(player.getName());

            if(property == null) {
                try {
                    property = plugin.getSkinsRestorerAPI().genSkinUrl("https://ic.wampi.ru/2023/06/17/Original_Steve_with_Beard.png", SkinVariant.CLASSIC);
                } catch (SkinRequestException e) {
                    Refactor.sendMessage(player, "Ошибка SkinsRestorerAPI! Попробуйте снова позже!");
                    Refactor.sendMessage(player, "Если ошибка осталась, создайте тикет в поддержке!");
                    e.printStackTrace();
                    return;
                }
            }

            if(handler.getCharacter(player, 1) == null) {
                try {
                    handler.saveCharacter(new Character(
                            (long) handler.getLastCharId() + 1,
                            player.getName(),
                            player.getUniqueId().toString(),
                            "Нон-РП",
                            player.getName(),
                            property.getName(),
                            property.getValue(),
                            property.getSignature(),
                            1
                    ), SavingType.SAVE);
                } catch (SQLException e) {
                    Refactor.sendFormattedWarn("%s's character #%d %s wasn't successfully saved", player.getName(), 1, player.getName());
                    e.printStackTrace();
                }
            }
        });
    }
}
