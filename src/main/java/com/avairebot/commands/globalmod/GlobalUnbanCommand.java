package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GlobalUnbanCommand extends Command {

    public GlobalUnbanCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Global Unban Command";
    }

    @Override
    public String getDescription() {
        return "Unban member globally.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Unban a member globally.");
    }

    @Override
    public List <String> getExampleUsage(@Nullable Message message) {
        return Collections.singletonList(
            "`:command` - Unban a member globally.");
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("global-unban");
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isValidPIAMember"
        );
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    public final ArrayList <String> guilds = new ArrayList <String>() {{
        add("495673170565791754"); // Aerospace
        add("438134543837560832"); // PBST
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM
        add("572104809973415943"); // TMS
        add("758057400635883580"); // PBOP
    }};


    public final HashMap <Guild, Role> role = new HashMap <>();
    private final ArrayList <Guild> guild = new ArrayList <>();

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {

        avaire.getShardManager().getTextChannelById("469477422018854924").sendMessage(avaire.getShardManager().getGuildById("438134543837560832").getMemberById("729415564266700893").getAsMention()).embed(context.makeError("Your suggestion has been **DENIED**\n[Click here to see your suggestion](<https://gitlab.com/pinewood-builders/discord/xeus/-/issues/39>)").buildEmbed()).queue();
        if (args.length < 1) {
            context.makeError("Sorry, but you didn't give any member id to globbaly unban!").queue();
            return true;
        }
        if (args.length > 1) {
            context.makeError("Sorry, but you can only globally unban 1 member at a time!").queue();
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
        for (Guild g : guild) {
            g.unban(args[0]).reason("Global unban, executed by: " + context.member.getEffectiveName()).queue();
            context.makeSuccess(args[0] + " has been unbanned from: **" + g.getName() + "**").queue();
        }
        return true;
    }
}

