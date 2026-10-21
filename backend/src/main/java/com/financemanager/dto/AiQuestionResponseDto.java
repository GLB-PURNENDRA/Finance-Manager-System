package com.financemanager.dto;

import java.util.List;

public class AiQuestionResponseDto {

    private String answer;
    private String reportingPeriod;
    private List<String> evidenceRecords;
    private String modelUsed;
    private boolean isRealLlm;
    private String note = "Insights are computed from verified backend financial records. Calculations are deterministic.";

    public AiQuestionResponseDto() {}

    public AiQuestionResponseDto(String answer, String reportingPeriod, List<String> evidenceRecords, String modelUsed, boolean isRealLlm) {
        this.answer = answer;
        this.reportingPeriod = reportingPeriod;
        this.evidenceRecords = evidenceRecords;
        this.modelUsed = modelUsed;
        this.isRealLlm = isRealLlm;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getReportingPeriod() { return reportingPeriod; }
    public void setReportingPeriod(String reportingPeriod) { this.reportingPeriod = reportingPeriod; }

    public List<String> getEvidenceRecords() { return evidenceRecords; }
    public void setEvidenceRecords(List<String> evidenceRecords) { this.evidenceRecords = evidenceRecords; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public boolean isRealLlm() { return isRealLlm; }
    public void setRealLlm(boolean realLlm) { isRealLlm = realLlm; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
