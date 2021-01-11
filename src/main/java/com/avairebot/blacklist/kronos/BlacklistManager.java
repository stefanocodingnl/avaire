package com.avairebot.blacklist.kronos;

import com.avairebot.AvaIre;
import com.avairebot.cache.CacheType;
import com.avairebot.chat.PlaceholderMessage;
import com.avairebot.commands.CommandMessage;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlacklistManager {

    private final AvaIre avaire;

    private static final Logger log = LoggerFactory.getLogger(BlacklistManager.class);

    public BlacklistManager(AvaIre avaire) {
        this.avaire = avaire;
    }

    private final String TMSCacheToken = "blacklist.tms.blacklists";
    private final String PBSTCacheToken = "blacklist.pbst.blacklists";

    @SuppressWarnings("unchecked")
    public ArrayList<Long> getTMSBlacklist() {
        if (avaire.getCache().getAdapter(CacheType.FILE).has(TMSCacheToken)) {
            ArrayList<LinkedTreeMap<String, Double>> items = (ArrayList<LinkedTreeMap<String, Double>>) avaire.getCache()
                .getAdapter(CacheType.FILE).get(TMSCacheToken);

            ArrayList<Long> list = new ArrayList<>();

            for (LinkedTreeMap<String, Double> item : items) {
                list.add(item.get("id").longValue());
            }

            return list;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Long> getPBSTBlacklist() {
        if (avaire.getCache().getAdapter(CacheType.FILE).has(PBSTCacheToken)) {
            List<LinkedTreeMap<String, Double>> items = (List<LinkedTreeMap<String, Double>>) avaire.getCache()
                .getAdapter(CacheType.FILE).get(PBSTCacheToken);


            ArrayList<Long> list = new ArrayList<>();

            for (LinkedTreeMap<String, Double> item : items) {
                list.add(item.get("id").longValue());
            }

            return list;
        }
        return null;
    }



}
