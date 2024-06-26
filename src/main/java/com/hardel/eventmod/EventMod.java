package com.hardel.eventmod;

import com.hardel.eventmod.command.CommandEvent;
import com.hardel.eventmod.event.finder.FinderAction;
import com.hardel.eventmod.event.parkour.ParkourAction;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String finderKey = "finder";
    public static final String ParkourKey = "parkour";

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing HeadFinder Mod");

        UseBlockCallback.EVENT.register(FinderAction::onBlockUse);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandEvent.registerCommands(dispatcher, registryAccess));
        ServerTickEvents.START_SERVER_TICK.register(ParkourAction::onTick);
    }
}
