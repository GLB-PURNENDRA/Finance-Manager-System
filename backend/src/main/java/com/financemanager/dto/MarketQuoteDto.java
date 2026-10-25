package com.financemanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketQuoteDto {

    private String symbol;
    private String name;
    private String assetType;
    private BigDecimal price;
    private BigDecimal previousClose;
    private BigDecimal dayChange;
    private BigDecimal dayChangePercentage;
    private String currency;
    private String source;
    private LocalDateTime timestamp = LocalDateTime.now();

    public MarketQuoteDto() {}

    public MarketQuoteDto(String symbol, String name, String assetType, BigDecimal price,
                          BigDecimal previousClose, BigDecimal dayChange, BigDecimal dayChangePercentage,
                          String currency, String source) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.price = price;
        this.previousClose = previousClose;
        this.dayChange = dayChange;
        this.dayChangePercentage = dayChangePercentage;
        this.currency = currency;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getDayChange() { return dayChange; }
    public void setDayChange(BigDecimal dayChange) { this.dayChange = dayChange; }

    public BigDecimal getDayChangePercentage() { return dayChangePercentage; }
    public void setDayChangePercentage(BigDecimal dayChangePercentage) { this.dayChangePercentage = dayChangePercentage; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
