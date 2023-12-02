package com.lx862.btellraw.commands;

import com.lx862.btellraw.config.Config;
import com.lx862.btellraw.data.TellrawEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class BtellrawCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> tellrawNode = CommandManager
                .literal("btellraw")
                .requires(Permissions.require("btw.main", 0))
                .build();

        LiteralCommandNode<ServerCommandSource> reloadNode = CommandManager
                .literal("reload")
                .requires(Permissions.require("btw.reload", 2))
                .executes(BtellrawCommand::reloadConfig)
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

        LiteralCommandNode<ServerCommandSource> previewNode = CommandManager
                .literal("preview")
                .requires(Permissions.require("btw.preview", 2))
                .build();

        LiteralCommandNode<ServerCommandSource> listNode = CommandManager
                .literal("list")
                .requires(Permissions.require("btw.list", 2))
                .executes(BtellrawCommand::listTellraws)
                .build();

        LiteralCommandNode<ServerCommandSource> aboutNode = CommandManager
                .literal("about")
                .requires(Permissions.require("btw.about", 0))
                .executes(BtellrawCommand::about)
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
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(Config.tellrawList.values().stream().map(t -> t.fileName).toList(), SuggestionBuilder))
                .then(CommandManager.argument("id", StringArgumentType.string())
                        .then(CommandManager.argument("text", StringArgumentType.string())
                        .executes(ctx -> addTellraw(ctx, StringArgumentType.string()))))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> modifyTellrawNode = CommandManager
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(Config.tellrawList.values().stream().map(t -> t.fullID).toList(), SuggestionBuilder))
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .executes(ctx -> modifyTellraw(ctx, StringArgumentType.string())))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> modifyTellrawTextNode = CommandManager
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(Config.tellrawList.values().stream().map(t -> t.fullID).toList(), SuggestionBuilder))
                        .then(CommandManager.argument("JSONText", TextArgumentType.text())
                                .executes(ctx -> modifyTellraw(ctx, TextArgumentType.text())))
                .build();

        ArgumentCommandNode<ServerCommandSource, Integer> pageNode = CommandManager
                .argument("page", IntegerArgumentType.integer(1))
                .executes(BtellrawCommand::listTellraws)
                .build();

        ArgumentCommandNode<ServerCommandSource, String> addTellrawTextNode = CommandManager
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> CommandSource.suggestMatching(Config.tellrawList.values().stream().map(t -> t.fileName).toList(), SuggestionBuilder))
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
                .executes(context -> sendTellraw(StringArgumentType.getString(context, "tellrawID"), context, new String[]{}))
                .suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(Config.tellrawList.keySet(), suggestionsBuilder))
                .build();

        ArgumentCommandNode<ServerCommandSource, Text> JSONTextNode = CommandManager
                .argument("Text", TextArgumentType.text())
                .executes(context -> sendTellraw(TextArgumentType.getTextArgument(context, "Text"), context))
                .build();

        ArgumentCommandNode<ServerCommandSource, String> placeholderNode = CommandManager
                .argument("placeholders", StringArgumentType.string())
                .executes(context -> sendTellraw(StringArgumentType.getString(context, "tellrawID"), context, StringArgumentType.getString(context, "placeholders").split(",")))
                .build();

        dispatcher.getRoot().addChild(tellrawNode);

        tellrawNode.addChild(reloadNode);
        tellrawNode.addChild(aboutNode);
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
        tellrawNode.addChild(previewNode);
            previewNode.addChild(tellrawID);
        tellrawNode.addChild(listNode);
            listNode.addChild(pageNode);
    }

    public static int reloadConfig(CommandContext<ServerCommandSource> context) {
        int tellrawLoaded = Config.load();
        context.getSource().sendFeedback(() -> Text.literal("Config reloaded. " + tellrawLoaded + " tellraws loaded.").formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Better Tellraw - Enhanced tellraw command and managed tellraw storage").formatted(Formatting.GOLD), false);
        context.getSource().sendFeedback(() -> Text.literal("https://modrinth.com/mod/bettertellraw").formatted(Formatting.GREEN).formatted(Formatting.UNDERLINE).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/bettertellraw"))), false);
        return 1;
    }

    public static int listTellraws(CommandContext<ServerCommandSource> context) {
        int tellrawPerPage = 8;
        int pages = (int)Math.ceil(Config.tellrawList.size() / (double)tellrawPerPage);
        int page = 0;
        int offset = 0;

        try {
            int selectedPage = IntegerArgumentType.getInteger(context, "page")-1;

            if(selectedPage > pages-1) {
                context.getSource().sendFeedback(() -> Text.literal("Page " + (selectedPage+1) + " does not exists.").formatted(Formatting.RED), false);
                return 1;
            }
            page = selectedPage;
            offset = tellrawPerPage * selectedPage;
        } catch (Exception e) {
            // Page argument not supplied
        }

        // Separator
        context.getSource().sendFeedback(() -> Text.literal("There are " + Config.tellrawList.size() + " tellraws loaded.").formatted(Formatting.GREEN), false);

        int i = 0;
        for(String id : Config.tellrawList.keySet().stream().sorted().toList()) {
            if(i < offset) {
                i++;
                continue;
            }

            if(i > offset + tellrawPerPage-1) break;

            String order = (i+1) + ". ";

            MutableText finalText = Text.literal(order + id);
            finalText.formatted(Formatting.YELLOW);
            finalText.styled(style -> {
                style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/btellraw preview \"" + id + "\""));
                style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to preview " + id).formatted(Formatting.GOLD)));
                return style;
            });

            context.getSource().sendFeedback(() -> finalText, false);
            i++;
        }

        int ordinalPage = page + 1;

        MutableText leftArrow = Text.literal("←").formatted(Formatting.GOLD);
        MutableText rightArrow = Text.literal("→").formatted(Formatting.GOLD);
        MutableText pageText = Text.literal(" [ Page " + (ordinalPage) + "/" + pages + " ] ").formatted(Formatting.GOLD);

        leftArrow.styled(style -> {
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/btellraw list " + (ordinalPage - 1)));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Previous page").formatted(Formatting.YELLOW)));
            return style;
        });
        rightArrow.styled(style -> {
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/btellraw list " + (ordinalPage + 1)));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Next page").formatted(Formatting.YELLOW)));
            return style;
        });

        MutableText finalText = Text.literal("");
        if(page > 0) finalText.append(leftArrow);
        finalText.append(pageText);
        if(ordinalPage < pages) finalText.append(rightArrow);

        context.getSource().sendFeedback(() -> finalText, false);
        return 1;
    }

    public static int sendTellraw(Collection<ServerPlayerEntity> players, Text msg, CommandContext<ServerCommandSource> context) {
        Text finalText = Placeholders.parseText(msg, PlaceholderContext.of(context.getSource().getServer()));

        for (ServerPlayerEntity player : players) {
            player.sendMessage(finalText);
        }
        return 1;
    }

    public static int sendTellraw(String msg, CommandContext<ServerCommandSource> context, String[] placeholder) {
        Collection<ServerPlayerEntity> playerList;
        try {
            Box area = new Box(BlockPosArgumentType.getBlockPos(context, "pos1").toCenterPos(), BlockPosArgumentType.getBlockPos(context, "pos2").toCenterPos());
            playerList = context.getSource().getWorld().getEntitiesByClass(ServerPlayerEntity.class, area, e -> true);
        } catch (Exception e) {
            try {
                playerList = EntityArgumentType.getPlayers(context, "players");
            } catch (Exception f) {
                playerList = Collections.singletonList(context.getSource().getPlayer());
            }
        }

        TellrawEntry tellraw = Config.tellrawList.get(msg);
        String tellrawMsg;
        if(tellraw == null) {
            tellrawMsg = msg;
        } else {
            tellrawMsg = Config.tellrawList.get(msg).content;
        }

        String formattedString;
        try {
            formattedString = String.format(tellrawMsg, (Object[]) placeholder);
        } catch (Exception e) {
            formattedString = tellrawMsg;
        }

        Text tellrawComponent;
        try {
            tellrawComponent = Text.Serialization.fromJson(formattedString);
        } catch (Exception ignored) {
            tellrawComponent = TextParserUtils.formatTextSafe(formattedString);
        }

        return sendTellraw(playerList, tellrawComponent, context);
    }

    public static int sendTellraw(Text msg, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> playerList;
        try {
            Box area = new Box(BlockPosArgumentType.getBlockPos(context, "pos1").toCenterPos(), BlockPosArgumentType.getBlockPos(context, "pos2").toCenterPos());
            playerList = context.getSource().getWorld().getEntitiesByClass(ServerPlayerEntity.class, area, e -> true);
        } catch (Exception e) {
            playerList = EntityArgumentType.getPlayers(context, "players");
        }

        return sendTellraw(playerList, msg, context);
    }

    public static int addTellraw(CommandContext<ServerCommandSource> context, ArgumentType type) {
        String ID = StringArgumentType.getString(context, "id");
        String fullID = StringArgumentType.getString(context, "fileName") + "." + StringArgumentType.getString(context, "id");
        if(Config.tellrawList.get(fullID) != null) {
            context.getSource().sendFeedback(() -> Text.literal("Tellraw " + fullID + " already exists.").formatted(Formatting.RED), false);
            return 1;
        }

        if(type instanceof StringArgumentType) {
            TellrawEntry tellrawObj = new TellrawEntry(StringArgumentType.getString(context, "fileName"), StringArgumentType.getString(context, "text"), fullID, ID);
            Config.tellrawList.put(fullID, tellrawObj);
            Config.saveConfig();
        } else {
            TellrawEntry tellrawObj = new TellrawEntry(StringArgumentType.getString(context, "fileName"), Text.Serialization.toJsonString(TextArgumentType.getTextArgument(context, "JSONText")), fullID, ID);
            Config.tellrawList.put(fullID, tellrawObj);
            Config.saveConfig();
        }
        context.getSource().sendFeedback(() -> Text.literal("Tellraws added. Full ID is: " + fullID).formatted(Formatting.GREEN), false);
        return 1;
    }

    public static int modifyTellraw(CommandContext<ServerCommandSource> context, ArgumentType type) {
        String ID = StringArgumentType.getString(context, "id");
        TellrawEntry tellrawObj = Config.tellrawList.get(ID);

        if(tellrawObj == null) {
            context.getSource().sendFeedback(() -> Text.literal("Cannot find tellraw with ID " + ID).formatted(Formatting.RED), false);
            return 1;
        }

        if(type instanceof StringArgumentType) {
            tellrawObj.content = StringArgumentType.getString(context, "text");
        } else {
            tellrawObj.content = Text.Serialization.toJsonString(TextArgumentType.getTextArgument(context, "JSONText"));
        }
        Config.tellrawList.put(ID, tellrawObj);
        Config.saveConfig();

        context.getSource().sendFeedback(() -> Text.literal("Tellraw " + ID + " modified.").formatted(Formatting.GOLD), false);
        return 1;
    }
}