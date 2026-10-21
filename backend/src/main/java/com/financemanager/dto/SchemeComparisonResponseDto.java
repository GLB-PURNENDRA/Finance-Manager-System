package com.financemanager.dto;

import java.util.ArrayList;
import java.util.List;

public class SchemeComparisonResponseDto {

    private List<MutualFundSchemeDto> schemes = new ArrayList<>();
    private List<String> keyDifferences = new ArrayList<>();
    private String categoryVerdict;
    private String comparisonDate;
    private String disclaimer = "Past returns and historical volatility do not guarantee future performance. Compare expense ratios, benchmark deviation, and category objectives before making SIP decisions.";

    public SchemeComparisonResponseDto() {}

    public List<MutualFundSchemeDto> getSchemes() { return schemes; }
    public void setSchemes(List<MutualFundSchemeDto> schemes) { this.schemes = schemes; }

    public List<String> getKeyDifferences() { return keyDifferences; }
    public void setKeyDifferences(List<String> keyDifferences) { this.keyDifferences = keyDifferences; }

    public String getCategoryVerdict() { return categoryVerdict; }
    public void setCategoryVerdict(String categoryVerdict) { this.categoryVerdict = categoryVerdict; }

    public String getComparisonDate() { return comparisonDate; }
    public void setComparisonDate(String comparisonDate) { this.comparisonDate = comparisonDate; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
