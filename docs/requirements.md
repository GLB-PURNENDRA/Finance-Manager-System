# Requirements Specification: Finance System Manager

## 1. Project Overview
**Finance System Manager — Parent–Student Family Finance System** is a dynamic web application engineered to bridge personal financial autonomy for university students with transparency and visibility for parents. 

The system provides:
- Complete personal finance management for the Student (income, expenses, budgets, savings goals, investments, SIPs).
- Secure Parent linking with strict read-only access to permitted student spending reports.
- Nine intelligent analytical features to detect money leaks, forecast month-end spending, detect anomalies, simulate what-if scenarios, provide conversational insights, and import statements with duplicate protection.
- Zero mockups or fake client storage: 100% persistent backend with relational database storage and token-based security.

---

## 2. User Roles & Capabilities

### 2.1 Student Account (Full Control)
- **Authentication**: Register with email, full name, secure password; login to receive JWT session token; logout.
- **Transactions**: Add, edit, view, and delete income and expense records with category, merchant/description, amount, date, and payment method.
- **Budgets**: Set monthly category-specific spending limits and warning thresholds (e.g., alert at 80% limit).
- **Savings Goals**: Define target amounts, current saved funds, target dates, and track visual progress percentage.
- **Investment Portfolio**: Track assets (Stocks, Mutual Funds, Gold, Bonds, Fixed Deposits) with invested capital, current valuation, and calculated profit/loss.
- **SIP Tracker**: Manage Systematic Investment Plans (SIPs) with monthly debit amounts, due dates, payment status, duration, assumed return rate, and compound future value projections.
- **Family Link Management**: View parent link requests, approve link requests, revoke active links instantly, or generate unique link invitation codes.
- **Statement Import**: Upload CSV financial statements, view parsed rows in preview table, inspect duplicate flags, and confirm import.
- **Intelligent Features**: Access Money Leak Detector, Financial Anomaly Radar, Month-End Predictor, What-If Simulator, Goal Coach, and Ask Your Money AI.

### 2.2 Parent Account (Read-Only)
- **Authentication**: Register as Parent; login and receive JWT token.
- **Link Management**: Send a link request to a student using the student's email or invitation code.
- **Permitted Visibility**: Once approved (status = `ACTIVE`), view the student's permitted expense history, category breakdowns, and spending analytics.
- **Strict Read-Only Enforcement**:
  - The UI clearly indicates `READ-ONLY PARENT VIEW`.
  - Mutation endpoints (creating/editing/deleting transactions, altering budgets, touching investments) reject Parent requests with HTTP `403 Forbidden`.
  - If a link is `PENDING` or `REVOKED`, zero student records are accessible.
- **Prohibitions**: Cannot view unrelated student accounts, modify categories, or trigger automated changes.

---

## 3. Nine Intelligent Features

1. **Money Leak Detector**: Analyzes transaction frequency for recurring micro-expenses (e.g. daily tea/snacks, repeat food orders) and category spending velocity. Returns actionable explanations linked to specific records.
2. **What-If Simulator**: Enables hypothetical adjustments (e.g. "What if I reduce dining out by ₹1,500 and increase SIP by ₹1,000?") calculating simulated month-end balance without altering actual database records.
3. **Ask Your Money AI**: Natural language query engine over the user's authorized financial data with deterministic financial summaries. Features dual-mode execution: intelligent server-side rules analyzer and optional external Gemini API integration.
4. **Action-Taking AI Agent**: Suggests proposed actions (e.g., creating a budget category limit, setting a savings reminder) that require explicit user confirmation before saving. Never executes funds transfers.
5. **Smart Statement Intelligence**: CSV statement parser with column detection, date/amount normalization, category suggestion, duplicate identification against existing database records, and preview prior to commit.
6. **Financial Anomaly Radar**: Detects statistical outliers (e.g. single transaction > 3x category average, sudden category jump > 50% vs previous period, duplicate transaction within 24 hours). Explains rationale without false claims of fraud.
7. **Student Finance Mode**: Specialized category taxonomy optimized for university students (Hostel/Rent, Mess/Food, College Tuition/Fees, Books & Stationery, Commute/Transport, Digital Subscriptions, Personal Spending).
8. **Goal-Based Money Coach**: Evaluates current monthly savings velocity against goal deadlines and calculates recommended monthly allocation and trade-offs.
9. **Month-End Predictor**: Computes daily spend rate for current month, projects total monthly burn, and displays an uncertainty band based on historical variance.

---

## 4. Technical Non-Functional Requirements
- **Security**: BCrypt hashing, JWT Bearer authentication, server-side principal verification on every query.
- **Data Integrity**: Relational foreign keys with cascading where appropriate, `DECIMAL(12,2)` for financial amounts to avoid floating-point errors.
- **Database Support**: Native MySQL schema with auto-fallback to embedded H2 (MySQL compatibility mode) for standalone local testing.
- **Accessibility & Design**: Responsive CSS design with contrast-tested color palette, clear status banners, and confirmation modals for destructive operations.
