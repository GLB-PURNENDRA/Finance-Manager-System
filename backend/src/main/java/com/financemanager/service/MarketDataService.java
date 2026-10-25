package com.financemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financemanager.dto.MarketQuoteDto;
import com.financemanager.dto.MarketSearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 6000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache quotes for 3 minutes to balance real-time freshness and rate limits
    private final Map<String, CachedQuote> quoteCache = new ConcurrentHashMap<>();

    // Curated catalog for instant responsive symbol search
    private static final List<MarketSearchResultDto> CURATED_CATALOG = new ArrayList<>();

    static {
        // 1. Indian Bluechip Stocks (NSE)
        CURATED_CATALOG.add(new MarketSearchResultDto("RELIANCE.NS", "Reliance Industries Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("1190.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("TCS.NS", "Tata Consultancy Services Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("3950.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("INFY.NS", "Infosys Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("1780.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("HDFCBANK.NS", "HDFC Bank Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("1680.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("ICICIBANK.NS", "ICICI Bank Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("1260.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("TATAMOTORS.NS", "Tata Motors Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("890.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("SBIN.NS", "State Bank of India", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("795.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("ITC.NS", "ITC Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("475.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("BHARTIARTL.NS", "Bharti Airtel Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("1680.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("LT.NS", "Larsen & Toubro Ltd", "INDIAN_STOCKS", "NSE", "INR", new BigDecimal("3520.00")));

        // 2. US Tech & Growth Stocks (NASDAQ/NYSE)
        CURATED_CATALOG.add(new MarketSearchResultDto("AAPL", "Apple Inc.", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("235.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("MSFT", "Microsoft Corporation", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("425.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("NVDA", "NVIDIA Corporation", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("140.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("GOOGL", "Alphabet Inc. (Google)", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("165.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("AMZN", "Amazon.com Inc.", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("185.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("TSLA", "Tesla Inc.", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("220.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("META", "Meta Platforms Inc.", "US_STOCKS", "NASDAQ", "USD", new BigDecimal("585.00")));

        // 3. ETFs (Index & Commodities)
        CURATED_CATALOG.add(new MarketSearchResultDto("NIFTYBEES.NS", "Nippon India ETF Nifty BeES", "ETFS", "NSE", "INR", new BigDecimal("265.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("BANKBEES.NS", "Nippon India ETF Bank BeES", "ETFS", "NSE", "INR", new BigDecimal("540.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("GOLDBEES.NS", "Nippon India ETF Gold BeES (1g Gold)", "GOLD", "NSE", "INR", new BigDecimal("115.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("SILVERBEES.NS", "Nippon India ETF Silver BeES (1g Silver)", "GOLD", "NSE", "INR", new BigDecimal("92.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("SPY", "SPDR S&P 500 ETF Trust", "ETFS", "NYSE", "USD", new BigDecimal("580.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("QQQ", "Invesco QQQ Trust (Nasdaq 100)", "ETFS", "NASDAQ", "USD", new BigDecimal("490.00")));

        // 4. Mutual Funds (AMFI Codes)
        CURATED_CATALOG.add(new MarketSearchResultDto("120716", "UTI Nifty 50 Index Fund - Direct Plan - Growth", "MUTUAL_FUNDS", "AMFI", "INR", new BigDecimal("159.18")));
        CURATED_CATALOG.add(new MarketSearchResultDto("122639", "Parag Parikh Flexi Cap Fund - Direct Plan - Growth", "MUTUAL_FUNDS", "AMFI", "INR", new BigDecimal("82.40")));
        CURATED_CATALOG.add(new MarketSearchResultDto("119598", "HDFC Top 100 Fund - Direct Plan - Growth Option", "MUTUAL_FUNDS", "AMFI", "INR", new BigDecimal("1120.50")));
        CURATED_CATALOG.add(new MarketSearchResultDto("120684", "Nippon India Small Cap Fund - Direct Plan - Growth", "MUTUAL_FUNDS", "AMFI", "INR", new BigDecimal("168.20")));
        CURATED_CATALOG.add(new MarketSearchResultDto("120828", "ICICI Prudential Bluechip Fund - Direct Plan - Growth", "MUTUAL_FUNDS", "AMFI", "INR", new BigDecimal("105.80")));

        // 5. Cryptocurrencies
        CURATED_CATALOG.add(new MarketSearchResultDto("BTC", "Bitcoin (BTC)", "CRYPTO", "CRYPTO", "INR", new BigDecimal("7650000.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("ETH", "Ethereum (ETH)", "CRYPTO", "CRYPTO", "INR", new BigDecimal("225000.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("SOL", "Solana (SOL)", "CRYPTO", "CRYPTO", "INR", new BigDecimal("15800.00")));
        CURATED_CATALOG.add(new MarketSearchResultDto("BNB", "Binance Coin (BNB)", "CRYPTO", "CRYPTO", "INR", new BigDecimal("51000.00")));
    }

    private static class CachedQuote {
        MarketQuoteDto quote;
        LocalDateTime timestamp;

        CachedQuote(MarketQuoteDto quote) {
            this.quote = quote;
            this.timestamp = LocalDateTime.now();
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(timestamp.plusMinutes(3));
        }
    }

    /**
     * Resolves the live market quote for any supported symbol and asset type.
     */
    public MarketQuoteDto getLiveQuote(String rawSymbol, String assetType, boolean forceRefresh) {
        if (rawSymbol == null || rawSymbol.trim().isEmpty()) {
            return null;
        }

        String normalizedSymbol = normalizeSymbol(rawSymbol.trim(), assetType);
        String cacheKey = (assetType != null ? assetType : "AUTO") + ":" + normalizedSymbol.toUpperCase();

        if (!forceRefresh) {
            CachedQuote cached = quoteCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.quote;
            }
        }

        MarketQuoteDto quote = null;

        // Route to specialized provider based on asset type
        if ("MUTUAL_FUNDS".equalsIgnoreCase(assetType) || isNumeric(normalizedSymbol)) {
            quote = fetchMutualFundQuote(normalizedSymbol);
        } else if ("CRYPTO".equalsIgnoreCase(assetType)) {
            quote = fetchCryptoQuote(normalizedSymbol);
            if (quote == null) {
                // Fallback to Yahoo Crypto format (e.g. BTC-INR)
                quote = fetchYahooFinanceQuote(normalizedSymbol.contains("-") ? normalizedSymbol : normalizedSymbol + "-INR", "CRYPTO");
            }
        } else {
            // Stocks, ETFs, Gold
            quote = fetchYahooFinanceQuote(normalizedSymbol, assetType);
        }

        if (quote != null) {
            quoteCache.put(cacheKey, new CachedQuote(quote));
        }

        return quote;
    }

    /**
     * Searches curated and dynamic financial catalog for auto-complete.
     */
    public List<MarketSearchResultDto> searchSymbols(String query, String assetType) {
        if (query == null || query.trim().isEmpty()) {
            if (assetType != null && !assetType.isEmpty()) {
                return CURATED_CATALOG.stream()
                        .filter(item -> item.getAssetType().equalsIgnoreCase(assetType))
                        .limit(8)
                        .toList();
            }
            return CURATED_CATALOG.stream().limit(8).toList();
        }

        String cleanQuery = query.trim().toLowerCase();
        List<MarketSearchResultDto> results = new ArrayList<>();

        for (MarketSearchResultDto item : CURATED_CATALOG) {
            boolean matchesType = (assetType == null || assetType.isEmpty() || item.getAssetType().equalsIgnoreCase(assetType));
            boolean matchesQuery = item.getSymbol().toLowerCase().contains(cleanQuery) ||
                                  item.getName().toLowerCase().contains(cleanQuery);

            if (matchesType && matchesQuery) {
                results.add(item);
            }
        }

        return results;
    }

    /**
     * Fetches live market data from Yahoo Finance API.
     */
    private MarketQuoteDto fetchYahooFinanceQuote(String symbol, String assetType) {
        String urlString = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1d";
        try {
            String json = executeGet(urlString);
            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            JsonNode resultNode = root.path("chart").path("result");
            if (resultNode.isMissingNode() || !resultNode.isArray() || resultNode.isEmpty()) {
                return null;
            }

            JsonNode meta = resultNode.get(0).path("meta");
            double regularPrice = meta.path("regularMarketPrice").asDouble(0.0);
            if (regularPrice <= 0) return null;

            double previousClose = meta.path("chartPreviousClose").asDouble(meta.path("previousClose").asDouble(regularPrice));
            String currency = meta.path("currency").asText("INR");
            String instrumentType = meta.path("instrumentType").asText(assetType != null ? assetType : "EQUITY");

            BigDecimal price = BigDecimal.valueOf(regularPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal prev = BigDecimal.valueOf(previousClose).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dayChange = price.subtract(prev);
            BigDecimal dayChangePct = BigDecimal.ZERO;
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                dayChangePct = dayChange.multiply(BigDecimal.valueOf(100)).divide(prev, 2, RoundingMode.HALF_UP);
            }

            String name = meta.path("shortName").asText(meta.path("symbol").asText(symbol));

            return new MarketQuoteDto(symbol, name, instrumentType, price, prev, dayChange, dayChangePct, currency, "Yahoo Finance API");
        } catch (Exception e) {
            log.warn("Yahoo Finance quote failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches live Mutual Fund NAV from official AMFI/MFAPI service.
     */
    private MarketQuoteDto fetchMutualFundQuote(String schemeCode) {
        String urlString = "https://api.mfapi.in/mf/" + schemeCode;
        try {
            String json = executeGet(urlString);
            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            JsonNode meta = root.path("meta");
            JsonNode dataArray = root.path("data");

            if (dataArray.isMissingNode() || !dataArray.isArray() || dataArray.isEmpty()) {
                return null;
            }

            String schemeName = meta.path("scheme_name").asText("Mutual Fund (" + schemeCode + ")");
            double currentNavVal = dataArray.get(0).path("nav").asDouble(0.0);
            if (currentNavVal <= 0) return null;

            double prevNavVal = dataArray.size() > 1 ? dataArray.get(1).path("nav").asDouble(currentNavVal) : currentNavVal;

            BigDecimal price = BigDecimal.valueOf(currentNavVal).setScale(4, RoundingMode.HALF_UP);
            BigDecimal prev = BigDecimal.valueOf(prevNavVal).setScale(4, RoundingMode.HALF_UP);
            BigDecimal dayChange = price.subtract(prev);
            BigDecimal dayChangePct = BigDecimal.ZERO;
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                dayChangePct = dayChange.multiply(BigDecimal.valueOf(100)).divide(prev, 2, RoundingMode.HALF_UP);
            }

            return new MarketQuoteDto(schemeCode, schemeName, "MUTUAL_FUNDS", price, prev, dayChange, dayChangePct, "INR", "Official AMFI NAV Feeds");
        } catch (Exception e) {
            log.warn("MFAPI NAV quote failed for scheme {}: {}", schemeCode, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches live Cryptocurrency prices from CoinGecko Public API.
     */
    private MarketQuoteDto fetchCryptoQuote(String symbol) {
        String coinId = mapCryptoId(symbol);
        String urlString = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=inr,usd&include_24hr_change=true";

        try {
            String json = executeGet(urlString);
            if (json == null) return null;

            JsonNode root = objectMapper.readTree(json);
            JsonNode coinNode = root.path(coinId);
            if (coinNode.isMissingNode()) return null;

            double inrPrice = coinNode.path("inr").asDouble(0.0);
            double change24h = coinNode.path("inr_24h_change").asDouble(0.0);
            if (inrPrice <= 0) return null;

            BigDecimal price = BigDecimal.valueOf(inrPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal changePct = BigDecimal.valueOf(change24h).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dayChange = price.multiply(changePct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal prev = price.subtract(dayChange);

            return new MarketQuoteDto(symbol.toUpperCase(), symbol.toUpperCase() + " / INR", "CRYPTO",
                    price, prev, dayChange, changePct, "INR", "CoinGecko Market API");
        } catch (Exception e) {
            log.warn("CoinGecko crypto quote failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private String normalizeSymbol(String symbol, String assetType) {
        String s = symbol.trim().toUpperCase();
        if ("INDIAN_STOCKS".equalsIgnoreCase(assetType) || "ETFS".equalsIgnoreCase(assetType)) {
            if (!s.contains(".") && !s.contains("-")) {
                return s + ".NS";
            }
        } else if ("GOLD".equalsIgnoreCase(assetType)) {
            if (s.equalsIgnoreCase("GOLD") || s.equalsIgnoreCase("DIGITAL_GOLD") || s.equalsIgnoreCase("GOLDBEES")) {
                return "GOLDBEES.NS";
            }
        }
        return s;
    }

    private String mapCryptoId(String symbol) {
        String clean = symbol.trim().toUpperCase().replace("-INR", "").replace("-USD", "");
        return switch (clean) {
            case "BTC", "BITCOIN" -> "bitcoin";
            case "ETH", "ETHEREUM" -> "ethereum";
            case "SOL", "SOLANA" -> "solana";
            case "BNB" -> "binancecoin";
            case "DOGE", "DOGECOIN" -> "dogecoin";
            case "XRP", "RIPPLE" -> "ripple";
            case "ADA", "CARDANO" -> "cardano";
            default -> clean.toLowerCase();
        };
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private String executeGet(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlString).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int status = conn.getResponseCode();
            if (status != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
