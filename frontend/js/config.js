// API Base Configuration
// Uses relative /api when served from backend or absolute http://localhost:8080/api when served from standalone live server
const API_BASE_URL = window.location.port === "8080" ? "/api" : "http://localhost:8080/api";
