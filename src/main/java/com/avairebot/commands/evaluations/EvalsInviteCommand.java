package com.avairebot.commands.evaluations;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import net.dv8tion.jda.api.entities.Member;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EvalsInviteCommand extends Command {

    public EvalsInviteCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Eval Invite Command";
    }

    @Override
    public String getDescription() {
        return "Command to invite people to the evals discord (Stefano Only).";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command <@mention1 @mention2>` - Give the people who are tagged a invite to the evals discord."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList(
            "`:command <@mention1 @mention2>` - Give the people who are tagged a invite to the evals discord."

        );
    }


    @Override
    public List <String> getTriggers() {
        return Arrays.asList("evalsinvite", "evalsdiscord");
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.EVALUATIONS
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "throttle:user,1,10",
            "isOfficialPinewoodGuild"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!context.member.getUser().getId().equals("173839105615069184")) {
            context.makeError("This is a command limited to Stefano#7366.").queue();
            return true;
        }
        if (context.message.getMentionedMembers().size() < 1) {
            context.makeError("You didn't mention any members.").queue();
            return false;
        }

        context.makeEmbeddedMessage(new Color(111, 43, 185), "Inviting " + context.message.getMentionedMembers().size() + " members to the PBST Evals discord.").queue(o -> {
            try {
                ArrayList <String> members = new ArrayList <>();
                for (Member m : context.getMessage().getMentionedMembers()) {
                    TimeUnit.SECONDS.sleep(3);
                    if (!(m.getRoles().contains(context.guild.getRoleById("438136063077384202")) || m.getRoles().contains(context.guild.getRoleById("438136062859280394")))) {
                        context.makeError("The user " + m.getEffectiveName() + " doesn't have the T3 or T2 rank!").queue();
                        continue;
                    }

                    o.editMessage(context.makeEmbeddedMessage(new Color(111, 43, 185), "Inviting " + m.getEffectiveName() + " to PBST Evals.").buildEmbed()).queue();
                    m.getUser().openPrivateChannel().queue(p -> p.sendMessage(context.makeInfo("You have been invited to the PBST Evals Discord!\n" +
                        "[Please click this message to join!](https://discord.gg/7Qwp7zQ)").buildEmbed()).queue());
                    members.add(m.getEffectiveName());

                }

                o.editMessage(context.makeSuccess("The people: \n" + String.join("\n", members) + " \nhave been invited to the discord!").buildEmbed()).queue();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        });


        return false;
    }
}
