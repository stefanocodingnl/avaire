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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GlobalBanCommand extends Command {

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
            context.makeError("Sorry, but you didn't give any member id to globbaly ban, or argument to use!").queue();
            return true;
        }

        if (args[0].equalsIgnoreCase("sync")) {
            return syncGlobalPermBansWithGuild(context);
        }

        if (args.length == 1) {
            context.makeError("Please supply a reason for the global ban!").queue();
            return true;
        }
        boolean soft = ComparatorUtil.isFuzzyFalse(args[1]);

        if (!soft && args.length < 3) {
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

        int time = soft ? 0 : 7;


        final String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (avaire.getShardManager().getUserById(args[0]) != null) {
            avaire.getShardManager().getUserById(args[0]).openPrivateChannel().queue(p -> {
                p.sendMessage(context.makeInfo("*You have been **global-banned** from all the Pinewood Builders discords by an PIA Agent. For the reason: *```" + reason + "```\n\n" +
                    "If you feel that your ban was unjustified please appeal at the Pinewood Builders Appeal Center; https://discord.gg/mWnQm25").setColor(Color.BLACK).buildEmbed()).queue();
            });
        }

        StringBuilder sb = new StringBuilder();
        for (Guild g : guild) {
            g.ban(args[0], time, "Banned by: " + context.member.getEffectiveName() + "\n" +
                "For: " + reason + "\n*THIS IS A PIA GLOBAL BAN, DO NOT REVOKE THIS BAN WITHOUT CONSULTING THE PIA MEMBER WHO INITIATED THE GLOBAL BAN, REVOKING THIS BAN WITHOUT PIA APPROVAL WILL RESULT IN DISCIPlINARY ACTION!*").reason("Global Ban, executed by " + context.member.getEffectiveName() + ". For: \n" + reason).queue();

            sb.append("``").append(g.getName()).append("`` - :white_check_mark:\n");
        }

        context.makeSuccess("<@" + args[0] + "> has been banned from: \n\n" + sb.toString()).queue();

        try {
            handleGlobalPermBan(context, args, reason);
        } catch (SQLException exception) {
            exception.printStackTrace();
            context.makeError("Something went adding this user to the global perm ban database.").queue();
        }

        return true;
    }

    private boolean syncGlobalPermBansWithGuild(CommandMessage context) {
        try {
            Collection c = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).get();
            for (DataRow dr : c) {
                context.guild.ban(dr.getString("userId"), 0, "THIS BAN MAY ONLY BE REVERTED BY A PIA MEMBER. ORIGINAL BAN REASON: " + dr.getString("reason")).reason("Ban sync").queue();
                context.makeInfo("Banned ``" + dr.getString("userId") + "`` (<@" + dr.getString("userId") + ">) from the guild.").setFooter("This message deletes after 10 seconds.").queue(v -> {
                    v.delete().queueAfter(10, TimeUnit.SECONDS);
                });
            }
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
}

