package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.administration.SoftBanCommand;
import com.avairebot.commands.administration.UnbanCommand;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.jagrosh.jdautilities.command.CommandEvent;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VoteCommand extends Command {

    public VoteCommand(AvaIre avaire) {
        super(avaire, true);
    }

    @Override
    public String getName() {
        return "Vote Command";
    }

    @Override
    public String getDescription() {
        return "Appel";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
                "`:command` - Vote for a vote."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command `");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("vote");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
                "throttle:user,1,25"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MISCELLANEOUS);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        

        if (!context.isGuildMessage()) {
            context.makeInfo(" TODO: Check if there is a vote on the first argument, if not. Continue down.").queue();
            return false; // TODO: Check if there is a vote on the first argument, if not. Continue down.
        }

        if (args.length < 1){
            context.makeInfo("Please tell a valid option to work with the vote system:\n" +
                    "- ``create``\n");
        return false;
        }

        if (args[0].equalsIgnoreCase("create")) {
            context.makeInfo("TODO: Make the vote creation system.").queue();
            if (args.length == 1) {
                context.makeError("Please enter the items you'd like to enter into the vote").setTitle("Need items!").queue();
            }
            if (args.length == 2) {
                String[] strings = args[1].split(",");
                context.makeError("You have given me the following options: ").setTitle("Missing question!")
            }
            return false; // TODO: Make the vote creation system.
        }

        if (args[0].equalsIgnoreCase("delete")) {
            context.makeInfo("TODO: Make the vote deletion system.").queue();
            return false; // TODO: Make the vote deletion system.
        }


        return false;
    }
}
