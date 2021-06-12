package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GlobalPruneCommand extends Command {

    public GlobalPruneCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Global Prune Command";
    }

    @Override
    public String getDescription() {
        return "Prune members globally.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Prune members from all guilds globally.");
    }

    @Override
    public List <String> getExampleUsage(@Nullable Message message) {
        return Collections.singletonList(
            "`:command` - Prune members from all guilds globally.");
    }

    @Override
    public List <Class <? extends Command>> getRelations() {
        return Collections.singletonList(AddPIAModWildcardCommand.class);
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("global-prune");
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
        add("791168471093870622"); // Kronos Dev
        add("371062894315569173"); // Official PB Server
        add("514595433176236078"); // PBQA
        add("436670173777362944"); // PET/
        add("505828893576527892"); // MMFA
        add("498476405160673286"); // PBM/
        add("572104809973415943"); // TMS
        add("758057400635883580"); // PBOP
    }};

    private final ArrayList <String> roles = new ArrayList <String>() {{
        add("536743894008987653");
        add("523960390758170642");
        add("522292764231073794");
        add("659929588134182912");
        add("438153651433897984");
        add("571737591989403648");
        add("547621009902272543");
        add("758065350322684015");
    }};

    public final HashMap <Guild, Role> role = new HashMap <>();
    private final ArrayList <Guild> guild = new ArrayList <>();

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (guild.size()>0) {
            guild.clear();
        }
        for (String s : guilds) {
            Guild g = avaire.getShardManager().getGuildById(s);
            if (g != null) {
                guild.add(g);
            }
        }
        if (role.size()>0) {
            role.clear();
        }
        for (Guild g : guild) {
            for (String r : roles) {
                if (g.getRoleById(r) != null) {
                    role.put(g, g.getRoleById(r));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        if (role.size() > 0) {
            Iterator <Map.Entry <Guild, Role>> it = role.entrySet().iterator();
            while (it.hasNext()) {
                it.forEachRemaining(p -> {
                    sb.append("Pruned members from ``").append(p.getKey().getName()).append("``");
                    p.getKey().prune(7, true, p.getValue()).queue();
                    guild.remove(p.getKey());
                });
                //it.remove(); // avoids a ConcurrentModificationException
            }
        }
        for (Guild g : guild) {
            sb.append("Pruned ").append(" members from ``").append(g.getName()).append("``\n");
            g.prune(7, true).reason("Global prune, executed by: " + context.getMember().getEffectiveName()).queue();
        }
        context.makeSuccess(sb.toString()).queue();

        TextChannel tc = avaire.getShardManager().getTextChannelById(Constants.PIA_LOG_CHANNEL);
        if (tc != null) {
            tc.sendMessage(context.makeInfo("[``:guildName`` where pruned by :mention](:link)")
                .set("guildName", "ALL GUILDS")
                .set("mention", context.getMember().getAsMention())
                .set("link", context.getMessage().getJumpUrl()).buildEmbed()).queue();
        }
        return true;
    }

}
