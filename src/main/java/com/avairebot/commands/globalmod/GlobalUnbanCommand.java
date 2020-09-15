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
    }};


    public final HashMap <Guild, Role> role = new HashMap <>();
    private final ArrayList <Guild> guild = new ArrayList <>();

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length < 1) {
            context.makeError("Sorry, but you didn't give any member id to globbaly ban!").queue();
            return true;
        }
        if (args.length > 1) {
            context.makeError("Sorry, but you can only globally ban 1 member at a time!").queue();
            return true;
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

