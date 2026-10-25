package com.financemanager.service;

import com.financemanager.dto.InvestmentDto;
import com.financemanager.dto.MarketQuoteDto;
import com.financemanager.dto.MarketSearchResultDto;
import com.financemanager.dto.PortfolioSummaryDto;
import com.financemanager.entity.Investment;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private AuditService auditService;

    /**
     * Resolves the target student for the given user (active student if Parent, self if Student).
     */
    private User resolveTargetStudent(User currentUser) {
        if (currentUser.getRole() == Role.PARENT) {
            return familyLinkService.getActiveStudentForParent(currentUser);
        }
        return currentUser;
    }

    /**
     * Retrieves all investments for the active student profile.
     */
    public List<InvestmentDto> getInvestments(User currentUser) {
        User targetStudent = resolveTargetStudent(currentUser);
        return investmentRepository.findByStudentOrderByCreatedAtDesc(targetStudent)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Generates a comprehensive live portfolio valuation summary with asset breakdown.
     */
    public PortfolioSummaryDto getPortfolioSummary(User currentUser) {
        List<InvestmentDto> list = getInvestments(currentUser);

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setInvestments(list);
        summary.setAssetCount(list.size());
        summary.setLastRefreshedAt(LocalDateTime.now());

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal todayGainLoss = BigDecimal.ZERO;

        Map<String, BigDecimal> valueByType = new LinkedHashMap<>();

        for (InvestmentDto inv : list) {
            BigDecimal invested = inv.getInvestedAmount() != null ? inv.getInvestedAmount() : BigDecimal.ZERO;
            BigDecimal current = inv.getCurrentValue() != null ? inv.getCurrentValue() : invested;
            BigDecimal dayChange = inv.getTotalDayChange() != null ? inv.getTotalDayChange() : BigDecimal.ZERO;

            totalInvested = totalInvested.add(invested);
            totalCurrentValue = totalCurrentValue.add(current);
            todayGainLoss = todayGainLoss.add(dayChange);

            String type = inv.getAssetType() != null ? inv.getAssetType() : "OTHER";
            valueByType.put(type, valueByType.getOrDefault(type, BigDecimal.ZERO).add(current));
        }

        summary.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalCurrentValue(totalCurrentValue.setScale(2, RoundingMode.HALF_UP));
        summary.setTodayGainLoss(todayGainLoss.setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalPnl = totalCurrentValue.subtract(totalInvested);
        summary.setTotalProfitLoss(totalPnl.setScale(2, RoundingMode.HALF_UP));

        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPct = totalPnl.multiply(new BigDecimal("100"))
                    .divide(totalInvested, 2, RoundingMode.HALF_UP);
            summary.setTotalProfitLossPercentage(pnlPct);
        } else {
            summary.setTotalProfitLossPercentage(BigDecimal.ZERO);
        }

        // Calculate asset allocation percentages
        Map<String, BigDecimal> allocationByType = new LinkedHashMap<>();
        BigDecimal baseForAllocation = totalCurrentValue.compareTo(BigDecimal.ZERO) > 0 ? totalCurrentValue : totalInvested;

        for (Map.Entry<String, BigDecimal> entry : valueByType.entrySet()) {
            if (baseForAllocation.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = entry.getValue().multiply(new BigDecimal("100"))
                        .divide(baseForAllocation, 1, RoundingMode.HALF_UP);
                allocationByType.put(entry.getKey(), pct);
            } else {
                allocationByType.put(entry.getKey(), BigDecimal.ZERO);
            }
        }

        summary.setValueByType(valueByType);
        summary.setAllocationByType(allocationByType);

        return summary;
    }

    /**
     * Refreshes all market-linked investments using real-time financial market APIs.
     */
    public PortfolioSummaryDto refreshPortfolioPrices(User currentUser) {
        User targetStudent = resolveTargetStudent(currentUser);
        List<Investment> investments = investmentRepository.findByStudentOrderByCreatedAtDesc(targetStudent);

        int updatedCount = 0;
        for (Investment inv : investments) {
            if (inv.getSymbol() != null && !inv.getSymbol().trim().isEmpty()) {
                try {
                    MarketQuoteDto quote = marketDataService.getLiveQuote(inv.getSymbol().trim(), inv.getAssetType(), true);
                    if (quote != null && quote.getPrice() != null && quote.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        inv.setCurrentPrice(quote.getPrice());
                        inv.setDayChange(quote.getDayChange() != null ? quote.getDayChange() : BigDecimal.ZERO);
                        inv.setDayChangePercentage(quote.getDayChangePercentage() != null ? quote.getDayChangePercentage() : BigDecimal.ZERO);
                        inv.setPriceLastUpdated(quote.getTimestamp() != null ? quote.getTimestamp() : LocalDateTime.now());

                        if (inv.getQuantity() != null && inv.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal updatedValuation = inv.getQuantity().multiply(quote.getPrice()).setScale(2, RoundingMode.HALF_UP);
                            inv.setCurrentValue(updatedValuation);
                        }
                        inv.setLastUpdated(LocalDateTime.now());
                        investmentRepository.save(inv);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    // Log and continue to ensure resilient batch execution
                }
            }
        }

        auditService.log(currentUser, "PORTFOLIO_PRICES_REFRESHED", "INVESTMENT",
                targetStudent.getId().toString(), "Refreshed live prices for " + updatedCount + " assets");

        return getPortfolioSummary(currentUser);
    }

    /**
     * Creates a new investment, auto-fetching live market quotes if symbol is provided.
     */
    public InvestmentDto createInvestment(User currentUser, InvestmentDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        String assetType = dto.getAssetType() != null ? dto.getAssetType().trim().toUpperCase() : "INDIAN_STOCKS";
        String symbol = dto.getSymbol() != null ? dto.getSymbol().trim() : null;
        String assetName = dto.getAssetName() != null ? dto.getAssetName().trim() : "";

        BigDecimal quantity = dto.getQuantity() != null && dto.getQuantity().compareTo(BigDecimal.ZERO) > 0
                ? dto.getQuantity() : BigDecimal.ONE;
        BigDecimal purchasePrice = dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal brokerageCharges = dto.getBrokerageCharges() != null ? dto.getBrokerageCharges() : BigDecimal.ZERO;

        BigDecimal currentPrice = dto.getCurrentPrice();
        BigDecimal dayChange = BigDecimal.ZERO;
        BigDecimal dayChangePct = BigDecimal.ZERO;
        LocalDateTime quoteTime = null;
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";

        // Query live market API if symbol is provided
        if (symbol != null && !symbol.isEmpty()) {
            MarketQuoteDto quote = marketDataService.getLiveQuote(symbol, assetType, false);
            if (quote != null) {
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    currentPrice = quote.getPrice();
                }
                if (assetName.isEmpty() && quote.getName() != null && !quote.getName().isEmpty()) {
                    assetName = quote.getName();
                }
                dayChange = quote.getDayChange() != null ? quote.getDayChange() : BigDecimal.ZERO;
                dayChangePct = quote.getDayChangePercentage() != null ? quote.getDayChangePercentage() : BigDecimal.ZERO;
                quoteTime = quote.getTimestamp();
                if (quote.getCurrency() != null) {
                    currency = quote.getCurrency();
                }
            }
        }

        if (assetName.isEmpty()) {
            assetName = symbol != null ? symbol : "Asset";
        }

        // If purchasePrice was 0 and currentPrice is known, default purchasePrice to currentPrice
        if (purchasePrice.compareTo(BigDecimal.ZERO) <= 0 && currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            purchasePrice = currentPrice;
        }

        // Calculate invested amount
        BigDecimal investedAmount = dto.getInvestedAmount();
        if (investedAmount == null || investedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            investedAmount = quantity.multiply(purchasePrice).add(brokerageCharges).setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback for current price
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            currentPrice = purchasePrice;
        }

        // Calculate currentValue
        BigDecimal currentValue = dto.getCurrentValue();
        if (currentValue == null || currentValue.compareTo(BigDecimal.ZERO) <= 0) {
            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            } else {
                currentValue = investedAmount;
            }
        }

        Investment inv = new Investment(
                currentUser,
                assetType,
                assetName,
                symbol,
                dto.getInvestmentDate() != null ? dto.getInvestmentDate() : LocalDate.now(),
                quantity,
                purchasePrice,
                brokerageCharges,
                investedAmount,
                currentPrice,
                currentValue,
                currency,
                dto.getNotes()
        );
        inv.setDayChange(dayChange);
        inv.setDayChangePercentage(dayChangePct);
        inv.setPriceLastUpdated(quoteTime != null ? quoteTime : LocalDateTime.now());

        inv = investmentRepository.save(inv);
        auditService.log(currentUser, "INVESTMENT_CREATED", "INVESTMENT", inv.getId().toString(),
                "Asset: " + inv.getAssetName() + " (" + inv.getAssetType() + ")");
        return toDto(inv);
    }

    /**
     * Updates an existing investment.
     */
    public InvestmentDto updateInvestment(User currentUser, Long id, InvestmentDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found."));

        if (!inv.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only modify your own investments.");
        }

        String assetType = dto.getAssetType() != null ? dto.getAssetType().trim().toUpperCase() : inv.getAssetType();
        String symbol = dto.getSymbol() != null ? dto.getSymbol().trim() : inv.getSymbol();
        String assetName = dto.getAssetName() != null ? dto.getAssetName().trim() : inv.getAssetName();

        BigDecimal quantity = dto.getQuantity() != null && dto.getQuantity().compareTo(BigDecimal.ZERO) > 0
                ? dto.getQuantity() : inv.getQuantity();
        BigDecimal purchasePrice = dto.getPurchasePrice() != null ? dto.getPurchasePrice() : inv.getPurchasePrice();
        BigDecimal brokerageCharges = dto.getBrokerageCharges() != null ? dto.getBrokerageCharges() : inv.getBrokerageCharges();

        // Attempt quote update if symbol provided
        if (symbol != null && !symbol.isEmpty()) {
            MarketQuoteDto quote = marketDataService.getLiveQuote(symbol, assetType, false);
            if (quote != null) {
                inv.setCurrentPrice(quote.getPrice());
                inv.setDayChange(quote.getDayChange() != null ? quote.getDayChange() : BigDecimal.ZERO);
                inv.setDayChangePercentage(quote.getDayChangePercentage() != null ? quote.getDayChangePercentage() : BigDecimal.ZERO);
                inv.setPriceLastUpdated(quote.getTimestamp());
                if (quote.getCurrency() != null) {
                    inv.setCurrency(quote.getCurrency());
                }
            }
        }

        BigDecimal currentPrice = dto.getCurrentPrice() != null && dto.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0
                ? dto.getCurrentPrice() : inv.getCurrentPrice();

        BigDecimal investedAmount = dto.getInvestedAmount();
        if (investedAmount == null || investedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            investedAmount = quantity.multiply(purchasePrice).add(brokerageCharges).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal currentValue = dto.getCurrentValue();
        if (currentValue == null || currentValue.compareTo(BigDecimal.ZERO) <= 0) {
            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            } else {
                currentValue = investedAmount;
            }
        }

        inv.setAssetType(assetType);
        inv.setAssetName(assetName);
        inv.setSymbol(symbol);
        inv.setInvestmentDate(dto.getInvestmentDate() != null ? dto.getInvestmentDate() : inv.getInvestmentDate());
        inv.setQuantity(quantity);
        inv.setPurchasePrice(purchasePrice);
        inv.setBrokerageCharges(brokerageCharges);
        inv.setInvestedAmount(investedAmount);
        inv.setCurrentPrice(currentPrice);
        inv.setCurrentValue(currentValue);
        inv.setNotes(dto.getNotes());
        inv.setLastUpdated(LocalDateTime.now());

        inv = investmentRepository.save(inv);
        auditService.log(currentUser, "INVESTMENT_UPDATED", "INVESTMENT", inv.getId().toString(),
                "Current Value: " + inv.getCurrentValue());
        return toDto(inv);
    }

    /**
     * Deletes an investment.
     */
    public void deleteInvestment(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found."));

        if (!inv.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own investments.");
        }

        investmentRepository.delete(inv);
        auditService.log(currentUser, "INVESTMENT_DELETED", "INVESTMENT", id.toString(), "Deleted by student");
    }

    /**
     * Queries symbol suggestions for auto-complete.
     */
    public List<MarketSearchResultDto> searchSymbols(String query, String assetType) {
        return marketDataService.searchSymbols(query, assetType);
    }

    /**
     * Queries instant live market quote preview for an asset.
     */
    public MarketQuoteDto getLiveQuote(String symbol, String assetType) {
        return marketDataService.getLiveQuote(symbol, assetType, true);
    }

    /**
     * Maps Investment entity to DTO with calculated returns and day changes.
     */
    public InvestmentDto toDto(Investment inv) {
        InvestmentDto dto = new InvestmentDto();
        dto.setId(inv.getId());
        dto.setAssetType(inv.getAssetType());
        dto.setAssetName(inv.getAssetName());
        dto.setSymbol(inv.getSymbol());
        dto.setInvestmentDate(inv.getInvestmentDate());
        dto.setQuantity(inv.getQuantity() != null ? inv.getQuantity() : BigDecimal.ONE);
        dto.setPurchasePrice(inv.getPurchasePrice() != null ? inv.getPurchasePrice() : BigDecimal.ZERO);
        dto.setBrokerageCharges(inv.getBrokerageCharges() != null ? inv.getBrokerageCharges() : BigDecimal.ZERO);
        dto.setInvestedAmount(inv.getInvestedAmount());
        dto.setCurrentPrice(inv.getCurrentPrice() != null ? inv.getCurrentPrice() : inv.getPurchasePrice());
        dto.setCurrentValue(inv.getCurrentValue());
        dto.setDayChange(inv.getDayChange() != null ? inv.getDayChange() : BigDecimal.ZERO);
        dto.setDayChangePercentage(inv.getDayChangePercentage() != null ? inv.getDayChangePercentage() : BigDecimal.ZERO);

        BigDecimal totalDayChange = BigDecimal.ZERO;
        if (dto.getDayChange() != null && dto.getQuantity() != null) {
            totalDayChange = dto.getDayChange().multiply(dto.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        }
        dto.setTotalDayChange(totalDayChange);

        dto.setCurrency(inv.getCurrency() != null ? inv.getCurrency() : "INR");
        dto.setNotes(inv.getNotes());
        dto.setPriceLastUpdated(inv.getPriceLastUpdated());
        dto.setLastUpdated(inv.getLastUpdated());

        BigDecimal pnl = inv.getCurrentValue().subtract(inv.getInvestedAmount());
        dto.setProfitLoss(pnl);

        if (inv.getInvestedAmount() != null && inv.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPct = pnl.multiply(new BigDecimal("100"))
                    .divide(inv.getInvestedAmount(), 2, RoundingMode.HALF_UP);
            dto.setProfitLossPercentage(pnlPct);
        } else {
            dto.setProfitLossPercentage(BigDecimal.ZERO);
        }

        return dto;
    }
}
