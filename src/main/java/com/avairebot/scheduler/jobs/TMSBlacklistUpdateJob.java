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

package com.avairebot.scheduler.jobs;

import com.avairebot.AvaIre;
import com.avairebot.cache.CacheType;
import com.avairebot.contracts.scheduler.Job;
import com.avairebot.factories.RequestFactory;
import com.avairebot.requests.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TMSBlacklistUpdateJob extends Job {

    private static final Logger log = LoggerFactory.getLogger(TMSBlacklistUpdateJob.class);
    private final String cacheToken = "blacklist.tms.blacklists";

    public TMSBlacklistUpdateJob(AvaIre avaire) {
        super(avaire, 90, 90, TimeUnit.MINUTES);
        run();
    }

    @Override
    public void run() {
        handleTask(avaire -> {
            RequestFactory.makeGET("https://pb-kronos.dev/tms/blacklist")
                .addHeader("Access-Key", avaire.getConfig().getString("apiKeys.kronosApiKey"))
                .send((Consumer<Response>) response -> {
                    log.info("TMS Blacklist has been requested.");
                    ArrayList service = (ArrayList) response.toService(ArrayList.class);

                    avaire.getCache().getAdapter(CacheType.FILE).forever(cacheToken, service);
                });
        });
    }
}
