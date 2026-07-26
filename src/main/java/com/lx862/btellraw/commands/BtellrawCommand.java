package com.lx862.btellraw.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.TagParser;
import io.netty.handler.codec.DecoderException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

public final class BtellrawCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        LiteralCommandNode<CommandSourceStack> tellrawNode = Commands
                .literal("btellraw")
                .requires(Permissions.require("btw.main", PermissionLevel.ALL))
                .build();

        LiteralCommandNode<CommandSourceStack> reloadNode = Commands
                .literal("reload")
                .requires(Permissions.require("btw.reload", PermissionLevel.GAMEMASTERS))
                .executes(BtellrawCommand::reloadConfig)
                .build();

        LiteralCommandNode<CommandSourceStack> sendNode = Commands
                .literal("send")
                .requires(Permissions.require("btw.send", PermissionLevel.GAMEMASTERS))
                .build();

        LiteralCommandNode<CommandSourceStack> addNode = Commands
                .literal("add")
                .requires(Permissions.require("btw.add", PermissionLevel.GAMEMASTERS))
                .build();

        LiteralCommandNode<CommandSourceStack> modifyNode = Commands
                .literal("modify")
                .requires(Permissions.require("btw.modify", PermissionLevel.GAMEMASTERS))
                .build();

        LiteralCommandNode<CommandSourceStack> previewNode = Commands
                .literal("preview")
                .requires(Permissions.require("btw.preview", PermissionLevel.GAMEMASTERS))
                .build();

        LiteralCommandNode<CommandSourceStack> listNode = Commands
                .literal("list")
                .requires(Permissions.require("btw.list", PermissionLevel.GAMEMASTERS))
                .executes(BtellrawCommand::listTellraws)
                .build();

        LiteralCommandNode<CommandSourceStack> aboutNode = Commands
                .literal("about")
                .requires(Permissions.require("btw.about", PermissionLevel.ALL))
                .executes(BtellrawCommand::about)
                .build();

        LiteralCommandNode<CommandSourceStack> selectorNode = Commands
                .literal("entity")
                .build();

        LiteralCommandNode<CommandSourceStack> posNode = Commands
                .literal("pos")
                .build();

        ArgumentCommandNode<CommandSourceStack, EntitySelector> entitiesNode = Commands
                .argument("players", EntityArgument.players())
                .build();

        ArgumentCommandNode<CommandSourceStack, String> addTellrawNode = Commands
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> SharedSuggestionProvider.suggest(Config.tellraws.values().stream().map(TellrawEntry::fileName).toList(), SuggestionBuilder))
                .then(Commands.argument("id", StringArgumentType.string())
                        .then(Commands.argument("text", StringArgumentType.string())
                        .executes(ctx -> addTellraw(ctx, StringArgumentType.string()))))
                .build();

        ArgumentCommandNode<CommandSourceStack, String> modifyTellrawNode = Commands
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> SharedSuggestionProvider.suggest(Config.tellraws.values().stream().map(TellrawEntry::fullID).toList(), SuggestionBuilder))
                        .then(Commands.argument("text", StringArgumentType.string())
                                .executes(ctx -> modifyTellraw(ctx, StringArgumentType.string())))
                .build();

        ArgumentCommandNode<CommandSourceStack, String> modifyTellrawTextNode = Commands
                .argument("id", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> SharedSuggestionProvider.suggest(Config.tellraws.values().stream().map(TellrawEntry::fullID).toList(), SuggestionBuilder))
                        .then(Commands.argument("JSONText", ComponentArgument.textComponent(registryAccess))
                                .executes(ctx -> modifyTellraw(ctx, ComponentArgument.textComponent(registryAccess))))
                .build();

        ArgumentCommandNode<CommandSourceStack, Integer> pageNode = Commands
                .argument("page", IntegerArgumentType.integer(1))
                .executes(BtellrawCommand::listTellraws)
                .build();

        ArgumentCommandNode<CommandSourceStack, String> addTellrawTextNode = Commands
                .argument("fileName", StringArgumentType.string()).suggests((commandContext, SuggestionBuilder) -> SharedSuggestionProvider.suggest(Config.tellraws.values().stream().map(TellrawEntry::fileName).toList(), SuggestionBuilder))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("JSONText", ComponentArgument.textComponent(registryAccess))
                                .executes(ctx -> addTellraw(ctx, ComponentArgument.textComponent(registryAccess)))))
                .build();

        ArgumentCommandNode<CommandSourceStack, Coordinates> pos1Node = Commands
                .argument("pos1", BlockPosArgument.blockPos())
                .build();

        ArgumentCommandNode<CommandSourceStack, Coordinates> pos2Node = Commands
                .argument("pos2", BlockPosArgument.blockPos())
                .build();

        ArgumentCommandNode<CommandSourceStack, String> tellrawID = Commands
                .argument("tellrawID", StringArgumentType.string())
                .executes(context -> sendTellraw(StringArgumentType.getString(context, "tellrawID"), context, new String[]{}))
                .suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(Config.tellraws.keySet(), suggestionsBuilder))
                .build();

        ArgumentCommandNode<CommandSourceStack, Component> JSONTextNode = Commands
                .argument("Text", ComponentArgument.textComponent(registryAccess))
                .executes(context -> sendTellraw(ComponentArgument.getRawComponent(context, "Text"), context))
                .build();

        ArgumentCommandNode<CommandSourceStack, String> placeholderNode = Commands
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

    public static int reloadConfig(CommandContext<CommandSourceStack> context) {
        int tellrawLoaded = Config.load();
        context.getSource().sendSuccess(() -> Component.literal("Config reloaded. " + tellrawLoaded + " tellraws loaded.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    public static int about(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Better Tellraw - Enhanced tellraw command and managed tellraw storage").withStyle(ChatFormatting.GOLD), false);
        context.getSource().sendSuccess(() -> Component.literal("https://modrinth.com/mod/bettertellraw").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.UNDERLINE).withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/bettertellraw")))), false);
        return 1;
    }

    public static int listTellraws(CommandContext<CommandSourceStack> context) {
        int tellrawPerPage = 8;
        int pages = (int)Math.ceil(Config.tellraws.size() / (double)tellrawPerPage);
        int page = 0;
        int offset = 0;

        try {
            int selectedPage = IntegerArgumentType.getInteger(context, "page")-1;

            if(selectedPage > pages-1) {
                context.getSource().sendSuccess(() -> Component.literal("Page " + (selectedPage+1) + " does not exists.").withStyle(ChatFormatting.RED), false);
                return 1;
            }
            page = selectedPage;
            offset = tellrawPerPage * selectedPage;
        } catch (Exception e) {
            // Page argument not supplied
        }

        // Separator
        context.getSource().sendSuccess(() -> Component.literal("There are " + Config.tellraws.size() + " tellraws loaded.").withStyle(ChatFormatting.GREEN), false);

        int i = 0;
        for(String id : Config.tellraws.keySet().stream().sorted().toList()) {
            if(i < offset) {
                i++;
                continue;
            }

            if(i > offset + tellrawPerPage-1) break;

            String order = (i+1) + ". ";

            MutableComponent finalText = Component.literal(order + id);
            finalText.withStyle(ChatFormatting.YELLOW);
            finalText.withStyle(style -> {
                style = style.withClickEvent(new ClickEvent.RunCommand("/btellraw preview \"" + id + "\""));
                style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to preview " + id).withStyle(ChatFormatting.GOLD)));
                return style;
            });

            context.getSource().sendSuccess(() -> finalText, false);
            i++;
        }

        int ordinalPage = page + 1;

        MutableComponent leftArrow = Component.literal("←").withStyle(ChatFormatting.GOLD);
        MutableComponent rightArrow = Component.literal("→").withStyle(ChatFormatting.GOLD);
        MutableComponent pageText = Component.literal(" [ Page " + (ordinalPage) + "/" + pages + " ] ").withStyle(ChatFormatting.GOLD);

        leftArrow.withStyle(style -> {
            style = style.withClickEvent(new ClickEvent.RunCommand("/btellraw list " + (ordinalPage - 1)));
            style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal("Previous page").withStyle(ChatFormatting.YELLOW)));
            return style;
        });
        rightArrow.withStyle(style -> {
            style = style.withClickEvent(new ClickEvent.RunCommand("/btellraw list " + (ordinalPage + 1)));
            style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal("Next page").withStyle(ChatFormatting.YELLOW)));
            return style;
        });

        MutableComponent finalText = Component.literal("");
        if(page > 0) finalText.append(leftArrow);
        finalText.append(pageText);
        if(ordinalPage < pages) finalText.append(rightArrow);

        context.getSource().sendSuccess(() -> finalText, false);
        return 1;
    }

    public static int sendTellraw(Collection<ServerPlayer> players, Component msg, CommandContext<CommandSourceStack> context) {
        Component finalText = Placeholders.COMMON_PLACEHOLDER_PARSER.parseComponent(TextNode.convert(msg), ServerPlaceholderContext.of(context.getSource().getServer()).asParserContext());

        for (ServerPlayer player : players) {
            player.sendSystemMessage(finalText);
        }
        return 1;
    }

    public static int sendTellraw(String msg, CommandContext<CommandSourceStack> context, String[] placeholder) {
        Collection<ServerPlayer> playerList;
        try {
            AABB area = new AABB(new Vec3(BlockPosArgument.getBlockPos(context, "pos1")), new Vec3(BlockPosArgument.getBlockPos(context, "pos2")));
            playerList = context.getSource().getLevel().getEntitiesOfClass(ServerPlayer.class, area, e -> true);
        } catch (Exception e) {
            try {
                playerList = EntityArgument.getPlayers(context, "players");
            } catch (Exception f) {
                playerList = Collections.singletonList(context.getSource().getPlayer());
            }
        }

        TellrawEntry tellraw = Config.tellraws.get(msg);
        String tellrawMsg;
        if(tellraw == null) {
            tellrawMsg = msg;
        } else {
            tellrawMsg = Config.tellraws.get(msg).content();
        }

        String formattedString;
        try {
            formattedString = String.format(tellrawMsg, (Object[]) placeholder);
        } catch (Exception e) {
            formattedString = tellrawMsg;
        }

        Component tellrawText;
        try {
            JsonElement jsonElement = JsonParser.parseString(formattedString);
            tellrawText = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).resultOrPartial().orElseThrow();
        } catch (JsonParseException | DecoderException ignored) {
            tellrawText = TagParser.QUICK_TEXT_WITH_STF.parseComponent(formattedString, ParserContext.of());
        }

        return sendTellraw(playerList, tellrawText, context);
    }

    public static int sendTellraw(Component msg, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> playerList;
        try {
            AABB area = new AABB(new Vec3(BlockPosArgument.getBlockPos(context, "pos1")), new Vec3(BlockPosArgument.getBlockPos(context, "pos2")));
            playerList = context.getSource().getLevel().getEntitiesOfClass(ServerPlayer.class, area, e -> true);
        } catch (Exception e) {
            playerList = EntityArgument.getPlayers(context, "players");
        }

        return sendTellraw(playerList, msg, context);
    }

    public static int addTellraw(CommandContext<CommandSourceStack> context, ArgumentType<?> type) {
        String ID = StringArgumentType.getString(context, "id");
        String fullID = StringArgumentType.getString(context, "fileName") + "." + StringArgumentType.getString(context, "id");
        if(Config.tellraws.get(fullID) != null) {
            context.getSource().sendSuccess(() -> Component.literal("Tellraw " + fullID + " already exists.").withStyle(ChatFormatting.RED), false);
            return 1;
        }

        if(type instanceof StringArgumentType) {
            TellrawEntry tellrawObj = new TellrawEntry(StringArgumentType.getString(context, "fileName"), StringArgumentType.getString(context, "text"), fullID, ID);
            Config.tellraws.put(fullID, tellrawObj);
            Config.saveConfig();
        } else {
            TellrawEntry tellrawObj = new TellrawEntry(StringArgumentType.getString(context, "fileName"), ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, ComponentArgument.getRawComponent(context, "JSONText")).getOrThrow().toString(), fullID, ID);
            Config.tellraws.put(fullID, tellrawObj);
            Config.saveConfig();
        }
        context.getSource().sendSuccess(() -> Component.literal("Tellraws added. Full id is: " + fullID).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    public static int modifyTellraw(CommandContext<CommandSourceStack> context, ArgumentType<?> type) {
        String ID = StringArgumentType.getString(context, "id");
        TellrawEntry oldEntry = Config.tellraws.get(ID);

        if(oldEntry == null) {
            context.getSource().sendSuccess(() -> Component.literal("Cannot find tellraw with id " + ID).withStyle(ChatFormatting.RED), false);
            return 1;
        }

        final String content;
        if(type instanceof StringArgumentType) {
            content = StringArgumentType.getString(context, "text");
        } else {
            content = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, ComponentArgument.getRawComponent(context, "JSONText")).getOrThrow().toString();
        }

        Config.tellraws.put(ID, new TellrawEntry(oldEntry.fileName(), content, oldEntry.fullID(), oldEntry.id()));
        Config.saveConfig();

        context.getSource().sendSuccess(() -> Component.literal("Tellraw " + ID + " modified.").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}