package com.dangel.nightvision;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mod(NightVisionMod.MODID)
public class NightVisionMod {
    public static final String MODID = "dangel_nightvision";
    private static final Logger LOGGER = LogUtils.getLogger(); // 로깅을 위한 Logger 추가
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("nightvision_players.txt");
    private static List<String> targetPlayers = new ArrayList<>();

    public NightVisionMod() {
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("nv")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "player");
                                    if (!targetPlayers.contains(name)) {
                                        targetPlayers.add(name);
                                        saveConfig();
                                        context.getSource().sendSuccess(() -> Component.literal("§a[NV] " + name + " 추가 완료"), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "player");
                                    if (targetPlayers.remove(name)) {
                                        saveConfig();
                                        context.getSource().sendSuccess(() -> Component.literal("§c[NV] " + name + " 제거 완료"), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal("§e[NV] 대상: " + targetPlayers), true);
                            return 1;
                        }))
        );
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END && event.player != null) {
            Player player = event.player;

            if (targetPlayers.contains(player.getName().getString())) {
                // 10초(200틱) 주기 검사
                if (player.tickCount % 200 == 0) {
                    MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);

                    // Null 체크 및 지속시간 검사
                    if (effect == null || effect.getDuration() < 400) {
                        player.addEffect(new MobEffectInstance(
                                MobEffects.NIGHT_VISION, 32767, 0, false, false, true
                        ));
                    }
                }
            }
        }
    }

    private void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                targetPlayers = new ArrayList<>(Files.readAllLines(CONFIG_FILE));
            }
        } catch (IOException e) {
            LOGGER.error("야간 투시 설정 파일을 읽는 중 오류 발생: ", e); // printStackTrace 대신 로깅 사용
        }
    }

    private void saveConfig() {
        try {
            Files.write(CONFIG_FILE, targetPlayers);
        } catch (IOException e) {
            LOGGER.error("야간 투시 설정 파일을 저장하는 중 오류 발생: ", e); // 로깅 강화
        }
    }
}