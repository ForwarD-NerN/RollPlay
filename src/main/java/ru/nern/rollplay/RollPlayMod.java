package ru.nern.rollplay;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RollPlayMod implements ModInitializer {
	public static ConfigurationManager.Config config = new ConfigurationManager.Config();
	public static final Logger LOGGER = LoggerFactory.getLogger("rollplay");

	@Override
	public void onInitialize() {
		ConfigurationManager.onInit();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("rollplay").then(literal("reload").requires(Permissions.require("rollplay.reload", 2)).executes(ctx -> reloadConfig(ctx.getSource()))));
			dispatcher.register(literal("roll").requires(Permissions.require("rollplay.roll", 0))
					.executes(ctx -> roll(ctx.getSource(), 12))
					.then(argument("max", IntegerArgumentType.integer(1, 12))
							.executes(ctx -> roll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "max")))));
			dispatcher.register(literal("coin").requires(Permissions.require("rollplay.coin", 0))
					.executes(ctx -> coin(ctx.getSource())));
			dispatcher.register(literal("try").requires(Permissions.require("rollplay.try", 0))
					.then(argument("action", StringArgumentType.string())
							.executes(ctx -> tryCmd(ctx.getSource(), StringArgumentType.getString(ctx, "action")))));
		});
	}

	private static int tryCmd(ServerCommandSource source, String action) {
		ConfigurationManager.Config.TryCommand config = RollPlayMod.config.trycmd;
		List<ServerPlayerEntity> players = getPlayersInRange(source, config.messageRange, false);

		boolean success = source.getWorld().random.nextBoolean();
		String result = success ? config.success : config.fail;

		for(ServerPlayerEntity player : players) {
			player.sendMessage(Text.literal(config.message.replace("%player", source.getName()).replace("%action", action).replace("%result", result)), false);
		}

		if(config.playSound) {
			Vec3d pos = source.getPosition();
			source.getWorld().playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
		}


		return 1;
	}

	private static int coin(ServerCommandSource source) {
		ConfigurationManager.Config.CoinCommand config = RollPlayMod.config.coin;
		List<ServerPlayerEntity> players = getPlayersInRange(source, config.messageRange, true);

		boolean orel = source.getWorld().random.nextBoolean();
		String res = orel ? config.orel : config.reshka;
		String selfMessage = String.format(orel ?
				config.selfMessageOrel : config.selfMessageReshka, res);

		source.sendFeedback(() -> Text.literal(selfMessage), false);

		for(ServerPlayerEntity player : players) {
			player.sendMessage(Text.literal(config.message.replace("%p", source.getName()).replace("%r", StringUtils.capitalize(res))), false);
		}

		if(config.playSound) {
			Vec3d pos = source.getPosition();
			source.getWorld().playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
		}

		return 1;
	}

	private static int roll(ServerCommandSource source, int max) {
		ConfigurationManager.Config.RollCommand config = RollPlayMod.config.roll;
		List<ServerPlayerEntity> players = getPlayersInRange(source, config.messageRange, true);

		int f = Math.min(max, 6);
		int s = max-f;

		int firstRoll = source.getWorld().random.nextBetween(1, f);
		int secondRoll = s > 0 ? source.getWorld().random.nextInt(s) : 0;

		String cubes = numberToCube(firstRoll)+numberToCube(secondRoll);
		source.sendFeedback(() -> Text.literal(String.format(config.selfMessage, cubes)), false);


		for(ServerPlayerEntity player : players) {
			player.sendMessage(Text.literal(config.message.replace("%p", source.getName()).replace("%r", cubes)), false);
		}

		if(config.playSound) {
			Vec3d pos = source.getPosition();
			source.getWorld().playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
		}

		return 1;
	}

	private static List<ServerPlayerEntity> getPlayersInRange(ServerCommandSource source, int range, boolean removeSender) {
		List<ServerPlayerEntity> players = range == -1 ? source.getServer().getPlayerManager().getPlayerList() : source.getWorld().getPlayers(player -> source.getPosition().distanceTo(player.getPos()) < range);
		//Удаляем отправителя из списка рассылки. Ему должно отправляться selfMessage
		if(removeSender && source.isExecutedByPlayer()) players.remove(source.getPlayer());
		return players;
	}

	private static String numberToCube(int number) {
        return switch (number) {
			case 0 -> "";
            case 1 -> "⚀";
            case 2 -> "⚁";
            case 3 -> "⚂";
            case 4 -> "⚃";
            case 5 -> "⚄";
            case 6 -> "⚅";
            default -> "TOO_BIG";
        };
	}

	private static int reloadConfig(ServerCommandSource source) {
		ConfigurationManager.onInit();
		source.sendFeedback(() -> Text.literal(RollPlayMod.config.reloadConfigMessage), true);
		return 1;
	}
}
