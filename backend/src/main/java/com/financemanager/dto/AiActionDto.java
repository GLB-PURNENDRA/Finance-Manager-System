package com.financemanager.dto;

import java.util.Map;

public class AiActionDto {

    private String actionId;
    private String actionType; // "CREATE_BUDGET", "ADJUST_SAVINGS_GOAL", "SET_CATEGORY_RULE"
    private String title;
    private String description;
    private Map<String, Object> payload;
    private boolean requiresConfirmation = true;

    public AiActionDto() {}

    public AiActionDto(String actionId, String actionType, String title, String description, Map<String, Object> payload) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.title = title;
        this.description = description;
        this.payload = payload;
        this.requiresConfirmation = true;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
}
