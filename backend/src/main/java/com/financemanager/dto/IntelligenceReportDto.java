package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class IntelligenceReportDto {

    // 1. Money Leaks
    private List<MoneyLeakItem> moneyLeaks;
    private BigDecimal totalLeakExposure = BigDecimal.ZERO;

    // 6. Anomalies
    private List<AnomalyItem> anomalies;

    // 8. Goal Coach Suggestions
    private List<GoalCoachItem> coachSuggestions;

    // 9. Month-End Predictor
    private MonthEndPrediction monthEndPrediction;

    public IntelligenceReportDto() {}

    public List<MoneyLeakItem> getMoneyLeaks() { return moneyLeaks; }
    public void setMoneyLeaks(List<MoneyLeakItem> moneyLeaks) { this.moneyLeaks = moneyLeaks; }

    public BigDecimal getTotalLeakExposure() { return totalLeakExposure; }
    public void setTotalLeakExposure(BigDecimal totalLeakExposure) { this.totalLeakExposure = totalLeakExposure; }

    public List<AnomalyItem> getAnomalies() { return anomalies; }
    public void setAnomalies(List<AnomalyItem> anomalies) { this.anomalies = anomalies; }

    public List<GoalCoachItem> getCoachSuggestions() { return coachSuggestions; }
    public void setCoachSuggestions(List<GoalCoachItem> coachSuggestions) { this.coachSuggestions = coachSuggestions; }

    public MonthEndPrediction getMonthEndPrediction() { return monthEndPrediction; }
    public void setMonthEndPrediction(MonthEndPrediction monthEndPrediction) { this.monthEndPrediction = monthEndPrediction; }

    public static class MoneyLeakItem {
        private String category;
        private String pattern;
        private int frequency;
        private BigDecimal totalSpent;
        private String explanation;
        private List<String> recordDetails;

        public MoneyLeakItem() {}
        public MoneyLeakItem(String category, String pattern, int frequency, BigDecimal totalSpent, String explanation, List<String> recordDetails) {
            this.category = category;
            this.pattern = pattern;
            this.frequency = frequency;
            this.totalSpent = totalSpent;
            this.explanation = explanation;
            this.recordDetails = recordDetails;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }

        public int getFrequency() { return frequency; }
        public void setFrequency(int frequency) { this.frequency = frequency; }

        public BigDecimal getTotalSpent() { return totalSpent; }
        public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        public List<String> getRecordDetails() { return recordDetails; }
        public void setRecordDetails(List<String> recordDetails) { this.recordDetails = recordDetails; }
    }

    public static class AnomalyItem {
        private String type; // "SPIKE", "OUTLIER", "SUSPECTED_DUPLICATE"
        private String title;
        private String explanation;
        private BigDecimal amount;
        private String category;
        private String transactionDate;

        public AnomalyItem() {}
        public AnomalyItem(String type, String title, String explanation, BigDecimal amount, String category, String transactionDate) {
            this.type = type;
            this.title = title;
            this.explanation = explanation;
            this.amount = amount;
            this.category = category;
            this.transactionDate = transactionDate;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getTransactionDate() { return transactionDate; }
        public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    }

    public static class GoalCoachItem {
        private String goalName;
        private BigDecimal targetAmount;
        private BigDecimal currentSaved;
        private BigDecimal recommendedMonthlySavings;
        private String reasoning;
        private String suggestedBudgetTradeOff;

        public GoalCoachItem() {}
        public GoalCoachItem(String goalName, BigDecimal targetAmount, BigDecimal currentSaved, BigDecimal recommendedMonthlySavings, String reasoning, String suggestedBudgetTradeOff) {
            this.goalName = goalName;
            this.targetAmount = targetAmount;
            this.currentSaved = currentSaved;
            this.recommendedMonthlySavings = recommendedMonthlySavings;
            this.reasoning = reasoning;
            this.suggestedBudgetTradeOff = suggestedBudgetTradeOff;
        }

        public String getGoalName() { return goalName; }
        public void setGoalName(String goalName) { this.goalName = goalName; }

        public BigDecimal getTargetAmount() { return targetAmount; }
        public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

        public BigDecimal getCurrentSaved() { return currentSaved; }
        public void setCurrentSaved(BigDecimal currentSaved) { this.currentSaved = currentSaved; }

        public BigDecimal getRecommendedMonthlySavings() { return recommendedMonthlySavings; }
        public void setRecommendedMonthlySavings(BigDecimal recommendedMonthlySavings) { this.recommendedMonthlySavings = recommendedMonthlySavings; }

        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }

        public String getSuggestedBudgetTradeOff() { return suggestedBudgetTradeOff; }
        public void setSuggestedBudgetTradeOff(String suggestedBudgetTradeOff) { this.suggestedBudgetTradeOff = suggestedBudgetTradeOff; }
    }

    public static class MonthEndPrediction {
        private int daysElapsed;
        private int totalDaysInMonth;
        private BigDecimal spentSoFar;
        private BigDecimal dailyBurnRate;
        private BigDecimal estimatedMonthEndSpend;
        private BigDecimal uncertaintyLow;
        private BigDecimal uncertaintyHigh;
        private String confidenceMessage;

        public MonthEndPrediction() {}

        public int getDaysElapsed() { return daysElapsed; }
        public void setDaysElapsed(int daysElapsed) { this.daysElapsed = daysElapsed; }

        public int getTotalDaysInMonth() { return totalDaysInMonth; }
        public void setTotalDaysInMonth(int totalDaysInMonth) { this.totalDaysInMonth = totalDaysInMonth; }

        public BigDecimal getSpentSoFar() { return spentSoFar; }
        public void setSpentSoFar(BigDecimal spentSoFar) { this.spentSoFar = spentSoFar; }

        public BigDecimal getDailyBurnRate() { return dailyBurnRate; }
        public void setDailyBurnRate(BigDecimal dailyBurnRate) { this.dailyBurnRate = dailyBurnRate; }

        public BigDecimal getEstimatedMonthEndSpend() { return estimatedMonthEndSpend; }
        public void setEstimatedMonthEndSpend(BigDecimal estimatedMonthEndSpend) { this.estimatedMonthEndSpend = estimatedMonthEndSpend; }

        public BigDecimal getUncertaintyLow() { return uncertaintyLow; }
        public void setUncertaintyLow(BigDecimal uncertaintyLow) { this.uncertaintyLow = uncertaintyLow; }

        public BigDecimal getUncertaintyHigh() { return uncertaintyHigh; }
        public void setUncertaintyHigh(BigDecimal uncertaintyHigh) { this.uncertaintyHigh = uncertaintyHigh; }

        public String getConfidenceMessage() { return confidenceMessage; }
        public void setConfidenceMessage(String confidenceMessage) { this.confidenceMessage = confidenceMessage; }
    }
}
