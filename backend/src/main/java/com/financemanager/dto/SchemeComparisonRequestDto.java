package com.financemanager.dto;

import java.util.List;

public class SchemeComparisonRequestDto {

    private List<String> schemeCodes;

    public SchemeComparisonRequestDto() {}

    public SchemeComparisonRequestDto(List<String> schemeCodes) {
        this.schemeCodes = schemeCodes;
    }

    public List<String> getSchemeCodes() { return schemeCodes; }
    public void setSchemeCodes(List<String> schemeCodes) { this.schemeCodes = schemeCodes; }
}
