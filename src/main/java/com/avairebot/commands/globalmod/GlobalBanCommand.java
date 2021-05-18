package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.utilities.ComparatorUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

public class GlobalBanCommand extends Command {

    private final static HashSet<String> fuzzyTrue = new HashSet<>(Arrays.asList("yes", "y", "on", "enable", "true", "confirm", "1"));
    private final static HashSet<String> fuzzyFalse = new HashSet<>(Arrays.asList("no", "n", "off", "disable", "false", "reset", "0"));

    public final ArrayList<String> guilds = new ArrayList<String>() {{
        add("495673170565791754"); // Aerospace
        add("438134543837560832"); // PBST
        add("791168471093870622"); // Kronos Dev
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM
        add("572104809973415943"); // TMS
        add("758057400635883580"); // PBOP
        add("669672893730258964"); // PB Dev
    }};
    public final HashMap<Guild, Role> role = new HashMap<>();
    private final ArrayList<Guild> guild = new ArrayList<>();

    public GlobalBanCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Global Ban Command";
    }

    @Override
    public String getDescription() {
        return "Ban member globally.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Ban a member globally.");
    }

    @Override
    public List<String> getExampleUsage(@Nullable Message message) {
        return Collections.singletonList(
            "`:command` - Ban a member globally.");
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("global-ban");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isValidPIAMember"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {

        if (args.length < 1) {
            context.makeError("Sorry, but you didn't give any member id to globbaly ban, or argument to use!\n\n" +
                "``Valid arguments``: \n" +
                " - **sync/s** - Sync all global-bans with a server.\n" +
                " - **v/view/see** - View the reason why someone is global-banned.\n\n*In the **RARE** cases someone has to be banned from the PBAC, you can ban them with ``--pbac-ban``*").queue();
            return false;
        }

        switch (args[0]) {
            case "s":
            case "sync": {
                return syncGlobalPermBansWithGuild(context);
            }
            case "view":
            case "see":
            case "v": {
                return showGlobalBanWithReason(context, args);
            }
            case "mod-bans":
            case "ban-logs":
            case "bl":
            case "mb": {
                return showWhoBannedWho(context, args);
            }
        }

        if (args.length == 1) {
            context.makeError("Please supply an true or false argument!").queue();
            return true;
        }

        if (!(fuzzyFalse.contains(args[1]) || fuzzyTrue.contains(args[1]))) {
            context.makeError("Please use either ``true`` or ``false`` as the second argument. (And yes, watch the capitalisation)").queue();
            return false;
        }

        ComparatorUtil.ComparatorType soft = ComparatorUtil.getFuzzyType(args[1]);

        if (args.length < 3) {
            context.makeError("Please supply a reason for the global ban!").queue();
            return true;
        }

        if (guild.size() > 0) {
            guild.clear();
        }
        for (String s : guilds) {
            Guild g = avaire.getShardManager().getGuildById(s);
            if (g != null) {
                guild.add(g);
            }
        }

        int time = soft.getValue() ? 0 : 7;
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        boolean pbacBan = false;
        if (reason.endsWith("--pbac-ban")) {
            reason = reason.replace("--pbac-ban", "");
            guild.add(avaire.getShardManager().getGuildById("750471488095780966"));
            pbacBan = true;
        }

        if (avaire.getShardManager().getUserById(args[0]) != null) {
            String finalReason = reason;
            boolean finalPbacBan = pbacBan;
            avaire.getShardManager().getUserById(args[0]).openPrivateChannel().queue(p -> {
                p.sendMessage(context.makeInfo(
                    "*You have been **global-banned** from all the Pinewood Builders discords by an PIA Agent. " +
                        "For the reason: *```" + finalReason + "```\n\n" +
                        (finalPbacBan ? "This ban has also banned you from the [PBAC (Pinewood Builders Appeal Center)](https://discord.gg/mWnQm25). " +
                            "If you feel like this was still unjustified, contact ``Stefano#7366``" : "If you feel that your ban was unjustified please appeal at the Pinewood Builders Appeal Center; " +
                            "https://discord.gg/mWnQm25")).setColor(Color.BLACK).buildEmbed()).queue();
            });
        }

        TextChannel tc = avaire.getShardManager().getTextChannelById(Constants.PIA_LOG_CHANNEL);
        if (tc != null) {
            tc.sendMessage(context.makeInfo("[``:global-unbanned-id`` was global-banned from all discords by :user for](:link):\n" +
                "```:reason```").set("global-unbanned-id", args[0]).set("reason", reason).set("user", context.getMember().getAsMention()).set("link", context.getMessage().getJumpUrl()).buildEmbed()).queue();
        }

        StringBuilder sb = new StringBuilder();
        for (Guild g : guild) {
            g.ban(args[0], time, "Banned by: " + context.member.getEffectiveName() + "\n" +
                "For: " + reason + "\n*THIS IS A PIA GLOBAL BAN, DO NOT REVOKE THIS BAN WITHOUT CONSULTING THE PIA MEMBER WHO INITIATED THE GLOBAL BAN, REVOKING THIS BAN WITHOUT PIA APPROVAL WILL RESULT IN DISCIPlINARY ACTION!*").reason("Global Ban, executed by " + context.member.getEffectiveName() + ". For: \n" + reason).queue();

            sb.append("``").append(g.getName()).append("`` - :white_check_mark:\n");
        }

        context.makeSuccess("<@" + args[0] + "> has been banned from: \n\n" + sb).queue();

        try {
            handleGlobalPermBan(context, args, reason);
        } catch (SQLException exception) {
            exception.printStackTrace();
            context.makeError("Something went wrong adding this user to the global perm ban database.").queue();
        }
        return true;
    }

    private boolean showWhoBannedWho(CommandMessage context, String[] args) {
        if (args.length < 2) {
            context.makeError("I don't know what moderator's id you'd like to view.").queue();
            return false;
        }

        String id = args[1];
        try {
            Collection c = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).where("punisherId", id).get();
            int i = c.size();
            if (i < 1) {
                context.makeInfo("This person has not banned anyone.").queue();
                return false;
            }
            StringBuilder sb = new StringBuilder();
            for (DataRow d : c) {
                sb.append("``userId`` was banned by <@punisherId> **(``punisherId``)** for:\n```reason```\n\n"
                    .replace("userId", d.getString("userId"))
                        .replaceAll("punisherId", d.getString("punisherId"))
                        .replace("reason", d.getString("reason")));
            }

            return buildAndSendEmbed(sb, context);
        } catch (SQLException throwables) {
            context.makeError("The database coudn't return anything, please check with the developer").queue();
            return false;
        }
    }
    private boolean showGlobalBanWithReason(CommandMessage context, String[] args) {
        if (args.length < 2) {
            context.makeError("I don't know what user id you'd like to view.").queue();
            return false;
        }

        String id = args[1];
        try {
            Collection c = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).where("userId", id).get();
            if (c.size() < 1) {
                context.makeInfo("``:userId`` was not found/is not banned.").set("userId", id).requestedBy(context).queue();
                return false;
            } else if (c.size() == 1) {
                context.makeSuccess("``:userId`` was banned by <@:punisherId> **(``:punisherId``)** for:\n```:reason```")
                    .set("userId", c.get(0).getString("userId")).set("punisherId", c.get(0).getString("punisherId")).set("reason", c.get(0).getString("reason")).queue();
                return true;
            } else {
                context.makeError("Something went wrong, there are more then 1 of the same punishment in the database. Ask Stefano to check this.").queue();
                return false;
            }
        } catch (SQLException throwables) {
            context.makeError("The database coudn't return anything, please check with the developer").queue();
            return false;
        }
    }
    private boolean syncGlobalPermBansWithGuild(CommandMessage context) {
        try {
            Collection c = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).get();
            context.makeInfo("Syncing **``" + c.size() + "``** global bans to this guild...").queue();
            for (DataRow dr : c) {
                context.guild.ban(dr.getString("userId"), 0, "THIS BAN MAY ONLY BE REVERTED BY A PIA MEMBER. ORIGINAL BAN REASON: " + dr.getString("reason")).reason("Ban sync").queue();
            }
            context.makeSuccess("**``" + c.size() + "``** global bans where synced to this guild...").queue();

        } catch (SQLException exception) {
            exception.printStackTrace();
            context.makeError("Something went wrong when syncing.").queue();
            return false;
        }
        return true;
    }
    private void handleGlobalPermBan(CommandMessage context, String[] args, String reason) throws SQLException {
        Collection c = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).where("userId", args[0]).get();
        if (c.size() < 1) {
            avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).insert(o -> {
                o.set("userId", args[0]);
                o.set("punisherId", context.getAuthor().getId());
                o.set("reason", reason, true);
            });
            context.makeSuccess("Permbanned ``" + args[0] + "`` in the database.").queue();
        } else {
            context.makeError("This user already has a permban in the database!").queue();
        }
    }
    private boolean buildAndSendEmbed(StringBuilder sb, CommandMessage context) {
        if (sb.length() > 1900) {
            for (String s : splitStringEvery(sb.toString(), 2000)) {
                context.makeWarning(s).queue();
            }
        } else {
            context.makeSuccess(sb.toString()).queue();
        }
        return true;
    }
    public String[] splitStringEvery(String s, int interval) {
        int arrayLength = (int) Math.ceil(((s.length() / (double) interval)));
        String[] result = new String[arrayLength];

        int j = 0;
        int lastIndex = result.length - 1;
        for (int i = 0; i < lastIndex; i++) {
            result[i] = s.substring(j, j + interval);
            j += interval;
        } //Add the last bit
        result[lastIndex] = s.substring(j);

        return result;
    }

}

