package com.financemanager.dto;

import jakarta.validation.constraints.NotBlank;

public class AiQuestionRequestDto {

    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;

    public AiQuestionRequestDto() {}
    public AiQuestionRequestDto(String prompt) { this.prompt = prompt; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
