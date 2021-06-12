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

package com.avairebot.commands.system;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.SystemCommand;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Collections;
import java.util.List;

public class ListServersPBCommand extends SystemCommand {

    public ListServersPBCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "List Official Servers Command";
    }

    @Override
    public String getDescription() {
        return "Just a change so I can list all guilds that are in the bot and the bot is inside.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <on|off>` - Toggles debug mode on or off");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command on` - Enables debug mode.");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("removeallnonpbserversfromthebotyesiamcompletlysureaboutthischangeandiknowwhatthiswilldoandiamalsoawarethisCANNOTberevertedbecausethebothastobereinvited");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!context.getMember().getId().equals("173839105615069184")) {
            context.makeError("YOU ARE NOT STEFANO").queue();
            context.channel.sendMessage("<@173839105615069184>").queue();
            context.channel.sendMessage("<@173839105615069184>").queue();
            context.channel.sendMessage("<@173839105615069184>").queue();
            return false;
        }


        /*for (Guild g : avaire.getShardManager().getGuilds()) {
            if (isValidPBGuild(g.getId())) {
                context.makeWarning("``" + g.getName()+ "`` has been ignored. Since this is an OFFICIAL PB Guild").queue();
            } else {
                g.leave().queue();
                context.makeWarning("Left ``" + g.getName() + "``").queue();
            }
        }*/
        return true;
    }

    private boolean isValidPBGuild(String id) {
        return Constants.guilds.contains(id);
    }
}
