package liege.counter;

public class LeaderboardEntry {
    private String name;
    private int pushups;
    private int level;
    private int totalXp;
    private int xpForNextLevel;
    private int currentXp;

    private int totalQuestsCompleted;
    private int quest1Completions;
    private int quest2Completions;
    private int quest3Completions;
    private int bonusXpCollected;

    private int dailyPushups;
    private int weeklyPushups;
    private int monthlyPushups;
    private int yearlyPushups;

    private double avgPushupsPerDay;
    private double avgPushupsPerWeek;
    private double avgPushupsPerMonth;


    public LeaderboardEntry() {
        this.name = name;
        this.pushups = pushups;
        this.level = level;
    }


    // Getters and Setters for all fields
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPushups() {
        return pushups;
    }

    public void setPushups(int pushups) {
        this.pushups = pushups;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getXpForNextLevel() {
        return xpForNextLevel;
    }

    public void setXpForNextLevel(int xpForNextLevel) {
        this.xpForNextLevel = xpForNextLevel;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(int currentXp) {
        this.currentXp = currentXp;
    }

    public int getTotalQuestsCompleted() {
        return totalQuestsCompleted;
    }

    public void setTotalQuestsCompleted(int totalQuestsCompleted) {
        this.totalQuestsCompleted = totalQuestsCompleted;
    }

    public int getQuest1Completions() {
        return quest1Completions;
    }

    public void setQuest1Completions(int quest1Completions) {
        this.quest1Completions = quest1Completions;
    }

    public int getQuest2Completions() {
        return quest2Completions;
    }

    public void setQuest2Completions(int quest2Completions) {
        this.quest2Completions = quest2Completions;
    }

    public int getQuest3Completions() {
        return quest3Completions;
    }

    public void setQuest3Completions(int quest3Completions) {
        this.quest3Completions = quest3Completions;
    }

    public int getBonusXpCollected() {
        return bonusXpCollected;
    }

    public void setBonusXpCollected(int bonusXpCollected) {
        this.bonusXpCollected = bonusXpCollected;
    }

    public int getDailyPushups() {
        return dailyPushups;
    }

    public void setDailyPushups(int dailyPushups) {
        this.dailyPushups = dailyPushups;
    }

    public int getWeeklyPushups() {
        return weeklyPushups;
    }

    public void setWeeklyPushups(int weeklyPushups) {
        this.weeklyPushups = weeklyPushups;
    }

    public int getMonthlyPushups() {
        return monthlyPushups;
    }

    public void setMonthlyPushups(int monthlyPushups) {
        this.monthlyPushups = monthlyPushups;
    }

    public int getYearlyPushups() {
        return yearlyPushups;
    }

    public void setYearlyPushups(int yearlyPushups) {
        this.yearlyPushups = yearlyPushups;
    }

    public double getAvgPushupsPerDay() {
        return avgPushupsPerDay;
    }

    public void setAvgPushupsPerDay(double avgPushupsPerDay) {
        this.avgPushupsPerDay = avgPushupsPerDay;
    }

    public double getAvgPushupsPerWeek() {
        return avgPushupsPerWeek;
    }

    public void setAvgPushupsPerWeek(double avgPushupsPerWeek) {
        this.avgPushupsPerWeek = avgPushupsPerWeek;
    }

    public double getAvgPushupsPerMonth() {
        return avgPushupsPerMonth;
    }

    public void setAvgPushupsPerMonth(double avgPushupsPerMonth) {
        this.avgPushupsPerMonth = avgPushupsPerMonth;
    }
}


