package com.avairebot.middleware;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.factories.MessageFactory;
import com.avairebot.utilities.CheckPermissionUtil;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class IsModOrHigherMiddleware extends Middleware {

    public IsModOrHigherMiddleware(AvaIre avaire) {
        super(avaire);
    }

    String rankName = CheckPermissionUtil.GuildPermissionCheckType.MOD.getRankName();

    @Override
    public String buildHelpDescription(@Nonnull CommandMessage context, @Nonnull String[] arguments) {
        return "**This command can only be executed by a ``"+ rankName +"`` or higher (Pinewood)!**";
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        if (avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong(), true).isAdmin()) {
            return stack.next();
        }

        int permissionLevel = CheckPermissionUtil.getPermissionLevel(stack.getDatabaseEventHolder().getGuild(), message.getGuild(), message.getMember()).getLevel();
        if (permissionLevel >= CheckPermissionUtil.GuildPermissionCheckType.MOD.getLevel()) {
            return stack.next();
        }

        if (args.length == 0) {
            return sendMustBeModOrHigherMessage(message);
        }

        if (!avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong()).isAdmin()) {
            return sendMustBeModOrHigherMessage(message);
        }

        return stack.next();
    }

    private boolean sendMustBeModOrHigherMessage(@Nonnull Message message) {
        return runMessageCheck(message, () -> {
            MessageFactory.makeError(message, "<a:alerta:729735220319748117> This command is only allowed to be executed by a ``"+rankName+"`` or higher!")
                .queue(newMessage -> newMessage.delete().queueAfter(45, TimeUnit.SECONDS), RestActionUtil.ignore);

            return false;
        });
    }
}
