package com.pvpbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class CommandPvPBot extends CommandBase {

    private static final int MAX_BOTS = 5;

    @Override
    public String getCommandName() {
        return "pvpbot";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pvpbot <spawn [dificuldade] | remove | difficulty <dificuldade> | armor <tipo> | sword <tipo> | pause | list>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.isSinglePlayer()) return true; // funciona sem "cheats" no singleplayer
        return super.canCommandSenderUseCommand(sender);
    }

    private static void msg(ICommandSender s, EnumChatFormatting color, String text) {
        ChatComponentText c = new ChatComponentText(text);
        c.getChatStyle().setColor(color);
        s.addChatMessage(c);
    }

    private static String material(String raw) {
        if (raw == null) return null;
        String s = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        switch (s) {
            case "none": case "nenhuma": case "nenhum": case "sem": return "none";
            case "leather": case "couro": return "leather";
            case "gold": case "golden": case "ouro": return "gold";
            case "chain": case "chainmail": case "malha": return "chain";
            case "iron": case "ferro": return "iron";
            case "diamond": case "diamante": return "diamond";
            case "wood": case "wooden": case "madeira": return "wood";
            case "stone": case "pedra": return "stone";
            default: return null;
        }
    }

    private static List<EntityPvPBot> bots(World w) {
        List<EntityPvPBot> r = new ArrayList<EntityPvPBot>();
        for (Entity e : new ArrayList<Entity>(w.loadedEntityList)) {
            if (e instanceof EntityPvPBot) r.add((EntityPvPBot) e);
        }
        return r;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            msg(sender, EnumChatFormatting.YELLOW, "Uso: " + getCommandUsage(sender));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World w = player.worldObj;

        switch (sub) {
            case "spawn": {
                Difficulty d = PvPBotMod.defaultDifficulty;
                if (args.length > 1) {
                    d = Difficulty.parse(args[1]);
                    if (d == null) {
                        msg(sender, EnumChatFormatting.RED, "Dificuldade invalida. Use: iniciante, intermediario, avancado, mestre ou insano.");
                        return;
                    }
                }
                if (bots(w).size() >= MAX_BOTS) {
                    msg(sender, EnumChatFormatting.RED, "Limite de " + MAX_BOTS + " bots. Use /pvpbot remove.");
                    return;
                }
                Vec3 look = player.getLookVec();
                double hx = look.xCoord, hz = look.zCoord;
                double len = Math.sqrt(hx * hx + hz * hz);
                if (len < 1.0E-3D) { hx = 0.0D; hz = 1.0D; len = 1.0D; }
                double sx = player.posX + hx / len * 4.0D;
                double sz = player.posZ + hz / len * 4.0D;

                EntityPvPBot bot = new EntityPvPBot(w);
                bot.setDifficulty(d);
                bot.setArmorType(PvPBotMod.defaultArmor);
                bot.setSwordType(PvPBotMod.defaultSword);
                boolean placed = false;
                for (int dy = 0; dy <= 6 && !placed; dy++) {
                    bot.setLocationAndAngles(sx, player.posY + dy, sz, player.rotationYaw + 180.0F, 0.0F);
                    if (w.getCollidingBoundingBoxes(bot, bot.getEntityBoundingBox()).isEmpty()) placed = true;
                }
                if (!placed) {
                    bot.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw + 180.0F, 0.0F);
                }
                bot.rotationYawHead = bot.rotationYaw;
                w.spawnEntityInWorld(bot);
                msg(sender, EnumChatFormatting.GREEN, "Bot invocado na dificuldade " + d.display + ".");
                return;
            }
            case "remove": {
                int n = 0;
                for (EntityPvPBot b : bots(w)) { b.setDead(); n++; }
                msg(sender, EnumChatFormatting.GREEN, n + " bot(s) removido(s).");
                return;
            }
            case "difficulty": {
                Difficulty d = args.length > 1 ? Difficulty.parse(args[1]) : null;
                if (d == null) {
                    msg(sender, EnumChatFormatting.RED, "Uso: /pvpbot difficulty <iniciante|intermediario|avancado|mestre|insano>");
                    return;
                }
                PvPBotMod.defaultDifficulty = d;
                for (EntityPvPBot b : bots(w)) b.setDifficulty(d);
                msg(sender, EnumChatFormatting.GREEN, "Dificuldade: " + d.display + ".");
                return;
            }
            case "armor": {
                String m = args.length > 1 ? material(args[1]) : null;
                if (m == null || "wood".equals(m) || "stone".equals(m)) {
                    msg(sender, EnumChatFormatting.RED, "Uso: /pvpbot armor <none|leather|gold|chain|iron|diamond>");
                    return;
                }
                PvPBotMod.defaultArmor = m;
                for (EntityPvPBot b : bots(w)) b.setArmorType(m);
                msg(sender, EnumChatFormatting.GREEN, "Armadura: " + m + ".");
                return;
            }
            case "sword": {
                String m = args.length > 1 ? material(args[1]) : null;
                if (m == null || "none".equals(m) || "leather".equals(m) || "chain".equals(m)) {
                    msg(sender, EnumChatFormatting.RED, "Uso: /pvpbot sword <wood|stone|gold|iron|diamond>");
                    return;
                }
                PvPBotMod.defaultSword = m;
                for (EntityPvPBot b : bots(w)) b.setSwordType(m);
                msg(sender, EnumChatFormatting.GREEN, "Espada: " + m + ".");
                return;
            }
            case "pause": {
                List<EntityPvPBot> list = bots(w);
                if (list.isEmpty()) {
                    msg(sender, EnumChatFormatting.RED, "Nenhum bot no mundo.");
                    return;
                }
                boolean newState = !list.get(0).isPaused();
                for (EntityPvPBot b : list) b.setPaused(newState);
                msg(sender, EnumChatFormatting.GREEN, newState ? "Bots pausados." : "Bots retomados.");
                return;
            }
            case "list": {
                List<EntityPvPBot> list = bots(w);
                msg(sender, EnumChatFormatting.YELLOW, list.size() + " bot(s) no mundo.");
                for (EntityPvPBot b : list) {
                    msg(sender, EnumChatFormatting.GRAY, "- " + b.getDifficulty().display + (b.isPaused() ? " (pausado)" : ""));
                }
                return;
            }
            default:
                msg(sender, EnumChatFormatting.YELLOW, "Uso: " + getCommandUsage(sender));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "spawn", "remove", "difficulty", "armor", "sword", "pause", "list");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("spawn".equals(sub) || "difficulty".equals(sub)) {
                return getListOfStringsMatchingLastWord(args, "iniciante", "intermediario", "avancado", "mestre", "insano");
            }
            if ("armor".equals(sub)) {
                return getListOfStringsMatchingLastWord(args, "none", "leather", "gold", "chain", "iron", "diamond");
            }
            if ("sword".equals(sub)) {
                return getListOfStringsMatchingLastWord(args, "wood", "stone", "gold", "iron", "diamond");
            }
        }
        return null;
    }
}
