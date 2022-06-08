package com.lx.bettertellraw.Cmds;

import com.lx.bettertellraw.Config.config;
import com.lx.bettertellraw.Data.Tellraws;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.Collection;

public class btellraw {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> tellrawNode = CommandManager
                .literal("btellraw")
                .requires(Permissions.require("btw.main", 2))
                .build();

        LiteralCommandNode<ServerCommandSource> reloadNode = CommandManager
                .literal("reload")
                .requires(Permissions.require("btw.reload", 2))
                .executes(btellraw::reloadConfig)
                .build();

        LiteralCommandNode<ServerCommandSource> sendNode = CommandManager
                .literal("send")
                .requires(Permissions.require("btw.send", 2))
                .build();

        LiteralCommandNode<ServerCommandSource> addNode = CommandManager
                .literal("add")
                .requires(Permissions.require("btw.add", 2))
                .build();

        LiteralCommandNode<ServerCommandSource> modifyNode = CommandManager
                .literal("modify")
                .requires(Permissions.require("btw.modify", 2))
                .build();

        LiteralCommandNode<ServerCommandSource> selectorNode = CommandManager
                .literal("entity")
                .build();

        LiteralCommandNode<ServerCommandSource> posNode = CommandManager
                .literal("pos")
                .build();

        ArgumentCommandNode<ServerCommandSource, EntitySelector> entitiesNode = CommandManager
                .argument("players", EntityArgumentType.players())
                .build();

        ArgumentCommandNode<ServerCommandSource, String> addTellrawNode = CommandManager
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(config.TellrawList.values().stream().map(t -> t.fileName).toList(), SuggestionBuilder))
                .then(CommandManager.argument("id", StringArgumentType.string())
                        .then(CommandManager.argument("text", StringArgumentType.string())
                        .executes(ctx -> addTellraw(ctx, StringArgumentType.string()))))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> modifyTellrawNode = CommandManager
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(config.TellrawList.values().stream().map(t -> t.fullID).toList(), SuggestionBuilder))
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .executes(ctx -> modifyTellraw(ctx, StringArgumentType.string())))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> modifyTellrawTextNode = CommandManager
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(config.TellrawList.values().stream().map(t -> t.fullID).toList(), SuggestionBuilder))
                        .then(CommandManager.argument("JSONText", TextArgumentType.text())
                                .executes(ctx -> modifyTellraw(ctx, TextArgumentType.text())))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> addTellrawTextNode = CommandManager
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(config.TellrawList.values().stream().map(t -> t.fileName).toList(), SuggestionBuilder))
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .then(CommandManager.argument("JSONText", TextArgumentType.text())
                                .executes(ctx -> addTellraw(ctx, TextArgumentType.text()))))
                .build();

        ArgumentCommandNode<ServerCommandSource, PosArgument> pos1Node = CommandManager
                .argument("pos1", BlockPosArgumentType.blockPos())
                .build();

        ArgumentCommandNode<ServerCommandSource, PosArgument> pos2Node = CommandManager
                .argument("pos2", BlockPosArgumentType.blockPos())
                .build();

        ArgumentCommandNode<ServerCommandSource, String> tellrawID = CommandManager
                .argument("tellrawID", StringArgumentType.string())
                .executes(context -> run(StringArgumentType.getString(context, "tellrawID"), context, new String[]{}))
                .suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(config.TellrawList.keySet(), suggestionsBuilder))
                .build();

        ArgumentCommandNode<ServerCommandSource, Text> JSONTextNode = CommandManager
                .argument("Text", TextArgumentType.text())
                .executes(context -> run(TextArgumentType.getTextArgument(context, "Text"), context))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> placeholderNode = CommandManager
                .argument("placeholders", StringArgumentType.string())
                .executes(context -> run(StringArgumentType.getString(context, "tellrawID"), context, StringArgumentType.getString(context, "placeholders").split(",")))
                .build();

        dispatcher.getRoot().addChild(tellrawNode);

        tellrawNode.addChild(reloadNode);
        tellrawNode.addChild(addNode);
            addNode.addChild(addTellrawNode);
            addNode.addChild(addTellrawTextNode);
        tellrawNode.addChild(modifyNode);
            modifyNode.addChild(modifyTellrawNode);
            modifyNode.addChild(modifyTellrawTextNode);
        tellrawNode.addChild(sendNode);
            sendNode.addChild(selectorNode);
                selectorNode.addChild(entitiesNode);
                    entitiesNode.addChild(tellrawID);
                    entitiesNode.addChild(JSONTextNode);
                        tellrawID.addChild(placeholderNode);

            sendNode.addChild(posNode);
                posNode.addChild(pos1Node);
                    pos1Node.addChild(pos2Node);
                        pos2Node.addChild(tellrawID);
                        pos2Node.addChild(JSONTextNode);
                            tellrawID.addChild(placeholderNode);
    }

    public static int reloadConfig(CommandContext<ServerCommandSource> context) {
        int tellrawLoaded = config.loadConfig();
        context.getSource().sendFeedback(Text.literal("Config reloaded. " + tellrawLoaded + " tellraws loaded.").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int run(Collection<ServerPlayerEntity> players, Text msg, CommandContext<ServerCommandSource> context) {
        Text finalText = Placeholders.parseText(msg, PlaceholderContext.of(context.getSource().getServer()));

        for (ServerPlayerEntity player : players) {
            player.sendMessage(finalText, false);
        }
        return 1;
    }

    public static int run(String msg, CommandContext<ServerCommandSource> context, String[] placeholder) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> playerList;
        try {
            Box area = new Box(BlockPosArgumentType.getBlockPos(context, "pos1"), BlockPosArgumentType.getBlockPos(context, "pos2"));
            playerList = context.getSource().getWorld().getEntitiesByClass(ServerPlayerEntity.class, area, e -> true);
        } catch (Exception e) {
            playerList = EntityArgumentType.getPlayers(context, "players");
        }

        Tellraws tellraw = config.TellrawList.get(msg);
        String tellrawMsg;
        if(tellraw == null) {
            tellrawMsg = msg;
        } else {
            tellrawMsg = config.TellrawList.get(msg).content;
        }

        String formattedString;
        try {
            formattedString = String.format(tellrawMsg, (Object[]) placeholder);
        } catch (Exception e) {
            formattedString = tellrawMsg;
        }

        Text tellrawComponent;
        try {
            tellrawComponent = Text.Serializer.fromJson(formattedString);
        } catch (Exception ignored) {
            tellrawComponent = TextParserUtils.formatTextSafe(formattedString);
        }

        return run(playerList, tellrawComponent, context);
    }

    public static int run(Text msg, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> playerList;
        try {
            Box area = new Box(BlockPosArgumentType.getBlockPos(context, "pos1"), BlockPosArgumentType.getBlockPos(context, "pos2"));
            playerList = context.getSource().getWorld().getEntitiesByClass(ServerPlayerEntity.class, area, e -> true);
        } catch (Exception e) {
            playerList = EntityArgumentType.getPlayers(context, "players");
        }

        return run(playerList, msg, context);
    }

    public static int addTellraw(CommandContext<ServerCommandSource> context, ArgumentType type) {
        String ID = StringArgumentType.getString(context, "id");
        String fullID = StringArgumentType.getString(context, "fileName") + "." + StringArgumentType.getString(context, "id");
        if(config.TellrawList.get(fullID) != null) {
            context.getSource().sendFeedback(Text.literal("Tellraw " + fullID + " already exists.").formatted(Formatting.RED), false);
            return 1;
        }

        if(type instanceof StringArgumentType) {
            Tellraws tellrawObj = new Tellraws(StringArgumentType.getString(context, "fileName"), StringArgumentType.getString(context, "text"), fullID, ID);
            config.TellrawList.put(fullID, tellrawObj);
            config.saveConfig();
        } else {
            Tellraws tellrawObj = new Tellraws(StringArgumentType.getString(context, "fileName"), Text.Serializer.toJson(TextArgumentType.getTextArgument(context, "JSONText")), fullID, ID);
            config.TellrawList.put(fullID, tellrawObj);
            config.saveConfig();
        }
        context.getSource().sendFeedback(Text.literal("Tellraws added. Full ID is: " + fullID).formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int modifyTellraw(CommandContext<ServerCommandSource> context, ArgumentType type) {
        String ID = StringArgumentType.getString(context, "id");
        Tellraws tellrawObj = config.TellrawList.get(ID);

        if(tellrawObj == null) {
            context.getSource().sendFeedback(Text.literal("Cannot find tellraw with ID " + ID).formatted(Formatting.RED), false);
            return 1;
        }

        if(type instanceof StringArgumentType) {
            tellrawObj.content = StringArgumentType.getString(context, "text");
        } else {
            tellrawObj.content = Text.Serializer.toJson(TextArgumentType.getTextArgument(context, "JSONText"));
        }
        config.TellrawList.put(ID, tellrawObj);
        config.saveConfig();

        context.getSource().sendFeedback(Text.literal("Tellraw " + ID + " modified.").formatted(Formatting.GOLD), false);
        return 1;
    }
}