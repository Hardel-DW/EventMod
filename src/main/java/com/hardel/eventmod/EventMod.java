package com.hardel.eventmod;

import com.hardel.eventmod.command.CommandEvent;
import com.hardel.eventmod.event.type.Finder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventMod implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing HeadFinder Mod");

        UseBlockCallback.EVENT.register(Finder::onBlockUse);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandEvent.registerCommands(dispatcher, registryAccess));
    }
}
