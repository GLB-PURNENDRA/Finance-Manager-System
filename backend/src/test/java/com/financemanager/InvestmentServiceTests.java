package com.financemanager;

import com.financemanager.dto.InvestmentDto;
import com.financemanager.dto.MarketQuoteDto;
import com.financemanager.dto.MarketSearchResultDto;
import com.financemanager.dto.PortfolioSummaryDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.InvestmentRepository;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.InvestmentService;
import com.financemanager.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvestmentServiceTests {

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestSeedHelper testSeedHelper;

    private User student;
    private User parent;

    @BeforeEach
    public void setup() {
        testSeedHelper.ensureTestData();
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
        parent = userRepository.findByEmail("parent@family.com").orElseThrow();
        investmentRepository.deleteAll();
    }

    @Test
    public void testSymbolSearchAndLiveQuote() {
        List<MarketSearchResultDto> results = investmentService.searchSymbols("reliance", "INDIAN_STOCKS");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> "RELIANCE.NS".equalsIgnoreCase(r.getSymbol())));

        MarketQuoteDto quote = investmentService.getLiveQuote("RELIANCE.NS", "INDIAN_STOCKS");
        assertNotNull(quote);
        assertNotNull(quote.getPrice());
        assertTrue(quote.getPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testCreateInvestmentAutoPricingAndCalculation() {
        InvestmentDto dto = new InvestmentDto();
        dto.setAssetType("INDIAN_STOCKS");
        dto.setSymbol("RELIANCE.NS");
        dto.setQuantity(new BigDecimal("10"));
        dto.setPurchasePrice(new BigDecimal("1100.00"));
        dto.setBrokerageCharges(new BigDecimal("20.00"));
        dto.setInvestmentDate(LocalDate.now());

        InvestmentDto created = investmentService.createInvestment(student, dto);
        assertNotNull(created.getId());
        assertEquals("RELIANCE.NS", created.getSymbol());
        // Invested amount = (10 * 1100) + 20 = 11020
        assertEquals(new BigDecimal("11020.00"), created.getInvestedAmount());
        assertNotNull(created.getCurrentPrice());
        assertTrue(created.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);
        // Current value = 10 * currentPrice
        assertTrue(created.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(created.getProfitLoss());
    }

    @Test
    public void testPortfolioSummaryCalculation() {
        InvestmentDto inv1 = new InvestmentDto();
        inv1.setAssetType("INDIAN_STOCKS");
        inv1.setAssetName("Reliance Industries");
        inv1.setSymbol("RELIANCE.NS");
        inv1.setQuantity(new BigDecimal("5"));
        inv1.setPurchasePrice(new BigDecimal("1000.00"));
        inv1.setBrokerageCharges(BigDecimal.ZERO);
        inv1.setCurrentPrice(new BigDecimal("1200.00"));
        inv1.setInvestedAmount(new BigDecimal("5000.00"));
        inv1.setCurrentValue(new BigDecimal("6000.00"));
        investmentService.createInvestment(student, inv1);

        InvestmentDto inv2 = new InvestmentDto();
        inv2.setAssetType("GOLD");
        inv2.setAssetName("Nippon Gold ETF");
        inv2.setSymbol("GOLDBEES.NS");
        inv2.setQuantity(new BigDecimal("50"));
        inv2.setPurchasePrice(new BigDecimal("100.00"));
        inv2.setBrokerageCharges(BigDecimal.ZERO);
        inv2.setCurrentPrice(new BigDecimal("110.00"));
        inv2.setInvestedAmount(new BigDecimal("5000.00"));
        inv2.setCurrentValue(new BigDecimal("5500.00"));
        investmentService.createInvestment(student, inv2);

        PortfolioSummaryDto summary = investmentService.getPortfolioSummary(student);
        assertNotNull(summary);
        assertEquals(2, summary.getAssetCount());
        assertTrue(summary.getTotalInvested().compareTo(new BigDecimal("9000.00")) > 0);
        assertTrue(summary.getTotalCurrentValue().compareTo(new BigDecimal("9000.00")) > 0);
        assertNotNull(summary.getAllocationByType());
        assertTrue(summary.getAllocationByType().containsKey("INDIAN_STOCKS"));
        assertTrue(summary.getAllocationByType().containsKey("GOLD"));
    }

    @Test
    public void testParentReadOnlyAccessConstraint() {
        InvestmentDto dto = new InvestmentDto();
        dto.setAssetType("INDIAN_STOCKS");
        dto.setAssetName("TCS");
        dto.setInvestedAmount(new BigDecimal("5000.00"));
        dto.setCurrentValue(new BigDecimal("5000.00"));

        assertThrows(ForbiddenException.class, () -> {
            investmentService.createInvestment(parent, dto);
        }, "Parent must not be able to create investments");
    }

    @Test
    public void testParentCanViewStudentPortfolio() {
        InvestmentDto dto = new InvestmentDto();
        dto.setAssetType("MUTUAL_FUNDS");
        dto.setAssetName("UTI Nifty 50 Index Fund");
        dto.setSymbol("120716");
        dto.setQuantity(new BigDecimal("100"));
        dto.setPurchasePrice(new BigDecimal("150.00"));
        dto.setInvestedAmount(new BigDecimal("15000.00"));
        dto.setCurrentValue(new BigDecimal("16000.00"));
        investmentService.createInvestment(student, dto);

        PortfolioSummaryDto parentView = investmentService.getPortfolioSummary(parent);
        assertNotNull(parentView);
        assertEquals(1, parentView.getAssetCount());
        assertEquals("UTI Nifty 50 Index Fund", parentView.getInvestments().get(0).getAssetName());
    }

    @Test
    public void testRefreshPortfolioPrices() {
        InvestmentDto dto = new InvestmentDto();
        dto.setAssetType("INDIAN_STOCKS");
        dto.setSymbol("RELIANCE.NS");
        dto.setQuantity(new BigDecimal("2"));
        dto.setPurchasePrice(new BigDecimal("500.00"));
        dto.setInvestedAmount(new BigDecimal("1000.00"));
        dto.setCurrentValue(new BigDecimal("1000.00"));
        investmentService.createInvestment(student, dto);

        PortfolioSummaryDto refreshed = investmentService.refreshPortfolioPrices(student);
        assertNotNull(refreshed);
        assertEquals(1, refreshed.getAssetCount());
        InvestmentDto updated = refreshed.getInvestments().get(0);
        // Current price must have updated from market quote (> 1000)
        assertTrue(updated.getCurrentPrice().compareTo(new BigDecimal("800.00")) > 0);
        assertTrue(updated.getCurrentValue().compareTo(new BigDecimal("1600.00")) > 0);
    }
}
