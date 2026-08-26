package com.icyapotheosis;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;


@EventBusSubscriber(modid = IcyApotheosis.MOD_ID)
public class ModCommands {

    
    private static final int PERMISSION_LEVEL = 2;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            LiteralArgumentBuilder.<CommandSourceStack>literal("icyapotheosis")
                .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                
                .executes(context -> {
                    context.getSource().sendSuccess(() ->
                        Component.translatable("command.icy_apotheosis.help"), false);
                    return 1;
                })

                
                .then(Commands.literal("reload")
                    .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                    .executes(context -> {
                        IcyApotheosisConfig.forceReloadFromDisk();
                        int total = IcyApotheosisConfig.getEntityRarityMap().size();
                        context.getSource().sendSuccess(() ->
                            Component.translatable("command.icy_apotheosis.reload.success"), false);
                        IcyApotheosis.LOGGER.info("[icy_apotheosis] /reload OK. {} boss entities registered.", total);
                        return 1;
                    })
                )

                
                .then(Commands.literal("list")
                    .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                    .executes(context -> {
                        Map<ResourceLocation, Map<String, Float>> map = IcyApotheosisConfig.getEntityRarityMap();
                        StringBuilder sb = new StringBuilder();
                        sb.append(Component.translatable("command.icy_apotheosis.list.header").getString()).append("\n");
                        if (map.isEmpty()) {
                            sb.append("  ").append(Component.translatable("command.icy_apotheosis.list.empty").getString());
                        } else {
                            map.forEach((entityKey, rarityWeights) -> {
                                sb.append("  \u00A7e").append(entityKey).append("\u00A7f -> ");
                                rarityWeights.forEach((rarity, weight) ->
                                    sb.append("\u00A7c").append(rarity).append("(w:").append(weight.floatValue()).append(") "));
                                sb.append("\n");
                            });
                        }
                        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                        return 1;
                    })
                )

                
                .then(Commands.literal("help")
                    .requires(src -> src.hasPermission(PERMISSION_LEVEL))
                    .executes(context -> {
                        context.getSource().sendSuccess(() ->
                            Component.translatable("command.icy_apotheosis.help"), false);
                        return 1;
                    })
                )
        );
    }
}
