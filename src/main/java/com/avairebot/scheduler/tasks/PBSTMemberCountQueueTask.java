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

package com.avairebot.scheduler.tasks;

import com.avairebot.AvaIre;
import com.avairebot.contracts.scheduler.Task;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

import static com.avairebot.utils.JsonReader.readArrayJsonFromUrl;
import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class PBSTMemberCountQueueTask implements Task {


    @Override
    public void handle(AvaIre avaire) {
        if (avaire.areWeReadyYet()) {
            Guild g = avaire.getShardManager().getGuildById("669672893730258964");
            if (g != null) {
                TextChannel tc = g.getTextChannelById("742658223466872853");
                if (tc != null) {
                    tc.sendMessage("<a:loading:742658561414266890> Retrieving member count").submit()
                        .thenAcceptAsync((d) -> {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            d.editMessage("PBST Currently has " + getMemberCount() + " members in the group!").submit();}).whenComplete((a, b) -> {
                        if (b != null) {
                            b.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    public int getMemberCount() {
        try {
            JSONObject json = readJsonFromUrl("https://groups.roblox.com/v1/groups/645836");
            return json.getInt("memberCount");
        } catch (Exception e) {
            return 0;
        }
    }

}
