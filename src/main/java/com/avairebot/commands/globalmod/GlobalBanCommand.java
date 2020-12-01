package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.utilities.ComparatorUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GlobalBanCommand extends Command {

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
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Ban a member globally.");
    }

    @Override
    public List <String> getExampleUsage(@Nullable Message message) {
        return Collections.singletonList(
            "`:command` - Ban a member globally.");
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("global-ban");
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
        add("669672893730258964"); // PB Dev
    }};


    public final HashMap <Guild, Role> role = new HashMap <>();
    private final ArrayList <Guild> guild = new ArrayList <>();

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length < 1) {
            context.makeError("Sorry, but you didn't give any member id to globbaly ban!").queue();
            return true;
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


        final String reason = String.join(" ", Arrays.copyOfRange(args, soft ? 1 : 2, args.length));
        StringBuilder sb = new StringBuilder();
        for (Guild g : guild) {
            g.ban(args[0], time).reason("Global Ban, executed by " + context.member.getEffectiveName() + ". For: \n" + reason).queue();
            sb.append("``").append(g.getName()).append("`` - :white_check_mark:\n");
        }
        context.makeSuccess(args[0] + "has been banned from: \n\n" + sb.toString()).queue();

        return true;
    }
}

