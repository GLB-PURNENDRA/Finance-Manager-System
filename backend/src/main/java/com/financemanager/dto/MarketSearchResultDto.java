package com.financemanager.dto;

import java.math.BigDecimal;

public class MarketSearchResultDto {

    private String symbol;
    private String name;
    private String assetType;
    private String exchange;
    private String currency;
    private BigDecimal suggestedPrice;

    public MarketSearchResultDto() {}

    public MarketSearchResultDto(String symbol, String name, String assetType, String exchange, String currency, BigDecimal suggestedPrice) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.currency = currency;
        this.suggestedPrice = suggestedPrice;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getSuggestedPrice() { return suggestedPrice; }
    public void setSuggestedPrice(BigDecimal suggestedPrice) { this.suggestedPrice = suggestedPrice; }
}
