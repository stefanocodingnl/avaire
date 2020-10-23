package com.avairebot.middleware;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.factories.MessageFactory;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class IsOfficialPinewoodGuildMiddleware extends Middleware {

    public IsOfficialPinewoodGuildMiddleware(AvaIre avaire) {
        super(avaire);
    }
    ArrayList<String> guilds = Constants.guilds;

    @Override
    public String buildHelpDescription(@Nonnull CommandMessage context, @Nonnull String[] arguments) {
        return "**This command can only be executed in official Pinewood servers!**";
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        if (avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong(), true).isAdmin()) {
            return stack.next();
        }

        if (isPinewoodGuild(message.getGuild().getId())) {
            return stack.next();
        }

        if (args.length == 0) {
            return sendMustBePinewoodDiscordMessage(message);
        }

        if (!avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong()).isAdmin()) {
            return sendMustBePinewoodDiscordMessage(message);
        }

        return stack.next();
    }

    private boolean isPinewoodGuild(String id) {
        return guilds.contains(id);
    }

    private boolean sendMustBePinewoodDiscordMessage(@Nonnull Message message) {
        return runMessageCheck(message, () -> {
            MessageFactory.makeError(message, "<a:alerta:729735220319748117> This command is only usable in official PB discord's, due to the fact it can modify something important!")
                .queue(newMessage -> newMessage.delete().queueAfter(45, TimeUnit.SECONDS), RestActionUtil.ignore);

            return false;
        });
    }
}
