/*
 * Copyright (c) 2018.
 *
 * This file is part of AvaIre.
 *
 * AvaIre is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AvaIre is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AvaIre.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.avairebot.middleware.global;

import com.avairebot.AvaIre;
import com.avairebot.commands.Category;
import com.avairebot.commands.CommandHandler;
import com.avairebot.commands.administration.ToggleCategoryCommand;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.database.transformers.ChannelTransformer;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.factories.MessageFactory;
import com.avairebot.middleware.MiddlewareStack;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IsCategoryEnabled extends Middleware {

    private static final HashMap<String, String> disabledCategories;

    static {
        disabledCategories = new HashMap<>();
    }

    public IsCategoryEnabled(AvaIre avaire) {
        super(avaire);
    }

    public static void enableCategory(Category category) {
        disabledCategories.remove(category.getName());
    }

    public static void disableCategory(Category category, @Nullable String reason) {
        disabledCategories.put(category.getName(), reason);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (disabledCategories.containsKey(stack.getCommandContainer().getCategory().getName())) {
            if (avaire.getBotAdmins().getUserById(message.getAuthor().getIdLong()).isAdmin()) {
                return stack.next();
            }

            String disabledMessage = disabledCategories.get(stack.getCommandContainer().getCategory().getName());
            if (disabledMessage == null) {
                disabledMessage = "The :category command category is currently disable globally by a bot administrator.";
            }

            String finalDisabledMessage = disabledMessage;
            return runMessageCheck(message, () -> {
                MessageFactory.makeError(message, finalDisabledMessage)
                    .set("category", stack.getCommandContainer().getCategory().getName())
                    .queue(success -> success.delete().queueAfter(15, TimeUnit.SECONDS, null, RestActionUtil.ignore));

                return false;
            });
        }

        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        if (isCategoryCommands(stack) || stack.getCommandContainer().getCategory().isGlobalOrSystem()) {
            return stack.next();
        }

        GuildTransformer transformer = stack.getDatabaseEventHolder().getGuild();
        if (transformer == null) {
            return stack.next();
        }

        ChannelTransformer channel = transformer.getChannel(message.getChannel().getId());
        if (channel == null) {
            return stack.next();
        }

        if (!channel.isCategoryEnabled(stack.getCommandContainer().getCategory()) && !isModOrHigher(stack, message)) {
            // TODO: Make the mod+ check for channel command bypass so commands will be limited to the bot commands channel except for mods.
            if (isHelpCommand(stack) && stack.isMentionableCommand()) {
                MessageFactory.makeError(message, "The help command is disabled in this channel, you can enable it by using the `:category` command.")
                    .set("category", CommandHandler.getCommand(ToggleCategoryCommand.class).getCommand().generateCommandTrigger(message))
                    .queue(success -> success.delete().queueAfter(15, TimeUnit.SECONDS, null, RestActionUtil.ignore));
            }

            return false;
        }

        return stack.next();
    }

    private boolean isModOrHigher(MiddlewareStack stack, Message message) {
        Set <Long> moderatorRoles = stack.getDatabaseEventHolder().getGuild().getModeratorRoles();
        Set <Long> adminRoles = stack.getDatabaseEventHolder().getGuild().getAdministratorRoles();
        Set <Long> managerRoles = stack.getDatabaseEventHolder().getGuild().getManagerRoles();

        List <Role> roles = new ArrayList <>();

        for (Long i : moderatorRoles) {
            Role r = message.getGuild().getRoleById(i);
            if (r != null) {
                roles.add(r);
            }
        }

        for (Long i : managerRoles) {
            Role r = message.getGuild().getRoleById(i);
            if (r != null) {
                roles.add(r);
            }
        }

        for (Long i : adminRoles) {
            Role r = message.getGuild().getRoleById(i);
            if (r != null) {
                roles.add(r);
            }
        }

        return roles.stream().anyMatch(message.getMember().getRoles()::contains);
    }

    private boolean isCategoryCommands(MiddlewareStack stack) {
        return stack.getCommand().getClass().getTypeName().equals("com.avairebot.commands.administration.ToggleCategoryCommand") ||
            stack.getCommand().getClass().getTypeName().equals("com.avairebot.commands.administration.CategoriesCommand");
    }

    private boolean isHelpCommand(MiddlewareStack stack) {
        return stack.getCommand().getClass().getTypeName().equals("com.avairebot.commands.help.HelpCommand");
    }
}
