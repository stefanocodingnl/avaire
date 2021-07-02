package com.avairebot.requests.service.kronos.database;

public class GetAllPointsFromDatabaseService {

    public int userId;
    public double points;
    public ExtraData extraData;

    public int getUserId() {
        return userId;
    }

    public double getPoints() {
        return points;
    }

    public ExtraData getExtraData() {
        return extraData;
    }


    public class ExtraData {
        public String notes;
        public int ranklock;
        public int tierEval;

        public String getNotes() {
            return notes;
        }

        public int getRanklock() {
            return ranklock;
        }

        public int getTierEval() {
            return tierEval;
        }
    }

}
