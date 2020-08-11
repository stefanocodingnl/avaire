package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.transformers.GuildTransformer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GamenightCommand extends Command {

    public GamenightCommand(AvaIre avaire) {
        super(avaire);
    }


    @Override
    public String getName() {
        return "Gamenight Command";
    }

    @Override
    public String getDescription() {
        return "Toggle's the gamenight channel for the configured role";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Toggle's a channel's access for a role, used for a gamenight channels or something else.",
            "`:command <open/close>` - Open or close the channel you've executed the command in.",
            "`:command role <role-id>` - Set the role used for the command"
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Arrays.asList(
            "`:command` -  Open/Close the channel you execute the command in",
            "`:command open` - This open's the channel you execute the command in.",
            "`:command role <role-id>` - Change the role used for the command "
        );
    }


    @Override
    public List <String> getTriggers() {
        return Collections.singletonList("gamenight");
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.MISCELLANEOUS
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "requireOne:user,general.kick_members",
            "throttle:guild,1,5"
        );
    }


    @Override
    public boolean onCommand(CommandMessage context, String[] args) {



        GuildTransformer guildTransformer = context.getGuildTransformer();

        if (guildTransformer == null) {
            return sendErrorMessage(context, "errors.errorOccurredWhileLoading", "server settings");
        }

        if (args.length == 0) {
            toggleGamenight(context);
        } else if (args[0].equals("role")) {
            setPublicGameNightRole(context, args, guildTransformer);
        } else if (args[0].equals("open")) {
            if (guildTransformer.getGamenightRole() == null) {
                context.makeEmbeddedMessage(Color.RED)
                    .setDescription("You haven't set the role, please set-up the role with ``c!gamenight role <role-id>``.")
                    .queue();
                return false;
            }
            openGameNightChannel(context, guildTransformer);
        } else if (args[0].equals("close")) {
            if (guildTransformer.getGamenightRole() == null) {
                context.makeEmbeddedMessage(Color.RED)
                    .setDescription("You haven't set the role, please set-up the role with ``c!gamenight role <role-id>``.")
                    .queue();
                return false;
            }
            closeGameNightChannel(context, guildTransformer);
        }
        return false;
    }

    private void toggleGamenight(CommandMessage context) {
        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer.getGamenightRole() == null) {
            context.makeEmbeddedMessage(Color.RED)
                .setDescription("You haven't set the role, please set-up the role with ``c!gamenight role <role-id>``.")
                .queue();
            return;
        }
        //Pre-set variables
        Role role = context.getGuild().getRoleById(transformer.getGamenightRole());

        PermissionOverride permissionOverride = context.getGuild().getTextChannelById(context.channel.getId()).getPermissionOverride(role);

        //context.getGuild().modifyRolePositions(true).selectPosition(guild).moveUp(1);

        if (permissionCheck(permissionOverride, context, role)) return;

        if (permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
            context.channel.putPermissionOverride(role).setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue();

            context.channel.sendMessage(":unlock: Channel has been unlocked!").queue();

        } else {
            context.channel.putPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue();
            context.channel.sendMessage(":lock: Channel has been locked!").queue();
        }


    }

    private void setPublicGameNightRole(CommandMessage context, String[] args, GuildTransformer guildTransformer) {
        if (args[1].matches("^[0-9]{18}$")) {
            Role role = context.getGuild().getRoleById(args[1]);
            if (role == null) {
                context.makeEmbeddedMessage(new Color(255, 0, 0))
                    .setDescription(":no_entry_sign: I can't find a role with that ID. Are you sure it's the correct one?")
                    .queue();
                return;
            }
            try {
                updateGoodnightRole(context, role, guildTransformer);
            } catch (SQLException e) {
                context.makeEmbeddedMessage().setDescription("Something went wrong: \n" + e.getLocalizedMessage());
            }
        } else {
            context.makeEmbeddedMessage(new Color(255, 0, 0))
                .setDescription("Please use the correct format for this command: \n - ``c!gamenight role <role-id>")
                .queue();
        }
    }

    private void closeGameNightChannel(CommandMessage context, GuildTransformer guildTransformer) {
        //Pre-set variables
        Role role = context.guild.getRoleById(guildTransformer.getGamenightRole());
        PermissionOverride permissionOverride = context.getGuild().getTextChannelById(context.channel.getId()).
            getPermissionOverride(role);

        if (permissionCheck(permissionOverride, context, role)) return;

        if (permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
            context.channel.sendMessage(":x: The channel you're trying to lock is already locked!").queue();
            return;
        }
        context.channel.putPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue();
        context.channel.sendMessage(":lock: Channel has been locked!").queue();

    }

    private void openGameNightChannel(CommandMessage context, GuildTransformer guildTransformer) {
        //Pre-set variables
        Role role = context.guild.getRoleById(guildTransformer.getGamenightRole());


        //TODO: Change to database request.
        PermissionOverride permissionOverride = context.getGuild().getTextChannelById(context.channel.getId()).
            getPermissionOverride(role);

        if (permissionCheck(permissionOverride, context, role)) return;

        if (permissionOverride.getAllowed().contains(Permission.MESSAGE_WRITE)) {
            context.channel.sendMessage(":x: The channel you're trying to unlock is already unlocked!").queue();
            return;
        }

        context.channel.putPermissionOverride(role).setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue();
        context.channel.sendMessage(":unlock: Channel has been unlocked!").queue();
    }

    private boolean permissionCheck(PermissionOverride permissionOverride, CommandMessage context, Role role) {
        if (permissionOverride == null) {
            context.makeEmbeddedMessage().setDescription("Due to some security reasons, you have to manually set the permission override for the ``" + role.getName() + "`` role\n" +
                "Please look at the image for more information.\n" +
                "\n" +
                "Furthermore, the bot needs the permission ``MANAGE_PERMISSIONS`` on the channel, to work (Also a security thing).").setImage("https://i.imgur.com/NCcCaK4.png")
                .setColor(new Color(255, 0, 0)).queue();
            return true;
        }
        return false;
    }

    private void updateGoodnightRole(CommandMessage context, Role role, GuildTransformer guildTransformer) throws SQLException {
        avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
            .where("id", context.getGuild().getId())
            .update(statement -> statement.set("gamenight_role", role.getId()));
        context.makeEmbeddedMessage(new Color(0, 255, 0)).setDescription(":white_check_mark: I've set the role :role to the configurable role in the database!".replace(":role", role.getName())).queue();

        guildTransformer.setGamenightRole(role.getId());
    }


}
