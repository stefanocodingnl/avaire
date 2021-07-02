package com.avairebot.requests.service.group;

import java.util.List;

public class GuildRobloxRanksService {

    public List<GroupRankBinding> groupRankBindings;

    public List<GroupRankBinding> getGroupRankBindings() {
        return groupRankBindings;
    }

    public class Group {
        private Long id;
        private List<Integer> ranks;

        public Long getId() {
            return id;
        }

        public List<Integer> getRanks() {
            return ranks;
        }

    }

    public class GroupRankBinding {
        private String role;
        private List<Group> groups;

        public String getRole() {
            return role;
        }

        public List<Group> getGroups() {
            return groups;
        }

    }

}
