package com.avairebot.commands.votes;

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
        return "";
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
                "throttle:user,1,4"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
    }
}
