# Finance System Manager — Parent–Student Family Finance System

> **A professional, full-stack personal finance and family transparency web application built with Java Spring Boot, MySQL/H2 relational database, and responsive Vanilla JavaScript.**

---

## 1. Project Overview & Architecture

Finance System Manager is engineered around a core family finance problem: **Students need financial autonomy to track day-to-day spending, while parents require verified, read-only transparency without micromanaging or modifying student records.**

```
finance-manager-system/
├── backend/                   # Spring Boot 3.3.4 (Java 21+) REST API
│   ├── src/main/java/         # Controllers, Services, Repositories, Entities, Security
│   ├── src/main/resources/    # Configuration (application.properties, application-mysql.properties)
│   ├── src/test/java/         # Automated unit & integration tests
│   └── pom.xml                # Maven project definition
├── frontend/                  # Modern Single Page Application (HTML5, CSS3, Vanilla ES6 JS)
│   ├── index.html             # Responsive dashboard layout
│   ├── css/style.css          # Design system, accessible palette & typography
│   └── js/                    # API client, Auth, Dashboard, Transactions, Budgets, Goals,
│                              # Investments, SIP, Family Link, Statement Import, Intelligence
├── database/
│   ├── schema.sql             # Production MySQL 8.0+ relational schema
│   └── seed.sql               # Safe sample demo seed data
├── docs/                      # Engineering documentation
│   ├── requirements.md        # Functional & non-functional requirements
│   ├── architecture.md        # Layered architecture and sequence flows
│   ├── roles-and-permissions.md# Role permission matrix & security guardrails
│   ├── database-schema.md     # Relational ER diagrams & constraints
│   └── roadmap.md             # Phased implementation status
├── mvnw / mvnw.cmd            # Cross-platform Maven wrappers
└── README.md                  # This setup and operating guide
```

---

## 2. Core Product: Two User Roles & Security Boundaries

### 🎓 Student Account (`STUDENT`)
- **Full Autonomy & Ownership**:
  - Add, edit, view, and delete income and expense transactions.
  - Set category spending limits and threshold alerts.
  - Track savings goals with visual completion percentages.
  - Maintain investment portfolios (Stocks, Mutual Funds, Gold, Bonds, FDs) with recorded P&L.
  - Track Systematic Investment Plans (SIP) with compound growth projections.
  - Import bank/UPI CSV statements with duplicate detection.
  - Generate a secure **Student Invitation Code** (e.g. `STU-ALEX-8842`) and explicitly **Approve** or **Revoke** Parent link requests.

### 🛡️ Parent Account (`PARENT`)
- **Strict Read-Only Visibility**:
  - Request a link using the Student's email or invitation code.
  - **Zero data visible while request is `PENDING`**.
  - Once approved (`ACTIVE`), view permitted student expense transactions and category analytics.
  - **Student Income records are private** and never disclosed to the parent.
- **Server-Side Enforcement**:
  - Parent mutation requests (`POST`, `PUT`, `DELETE` on transactions, budgets, goals, investments, SIPs) are rejected at the API controller layer with HTTP **`403 Forbidden`**.

---

## 3. Nine Intelligent Features

1. **Money Leak Detector**: Scans recent transactions to identify recurring micro-expenses (e.g. daily campus canteen tea/snacks) and velocity spikes. Explains the exact evidence records.
2. **What-If Simulator**: Allows students to simulate hypothetical adjustments (e.g. "+₹2,000 monthly stipend, -₹500 dining, +₹1,000 SIP") and see projected cash flow and annual savings impacts without altering actual database records.
3. **Ask Your Money AI**: Natural-language financial assistant that answers queries regarding authorized spending, top categories, and SIP schedules directly computed from verified backend ledger records.
4. **Action-Taking AI Agent**: Inspects uncapped spending categories and proposes actionable draft budgets (e.g. "Cap Food & Mess at ₹4,000"). Requires **explicit student confirmation** before applying. Never executes monetary transfers.
5. **Smart Statement Intelligence**: CSV statement parser that performs automatic column detection, date normalization, keyword-based category suggestions, and duplicate detection against existing ledger records.
6. **Financial Anomaly Radar**: Detects statistical outliers (expenses > 2.5x category average) and suspected duplicate charges within 48 hours without asserting fraud.
7. **Student Finance Mode**: Specialized category taxonomy optimized for student life (Hostel & Rent, Food & Mess, Books & Supplies, College Fees, Commute/Transport, Digital Subscriptions, Personal Spending).
8. **Goal-Based Money Coach**: Recommends feasible monthly contributions for active savings goals and highlights budget trade-offs.
9. **Month-End Predictor**: Computes daily spend pace for the current calendar month and projects month-end burn with an uncertainty interval ($\pm 12\%$).

---

## 4. Setup and Running Instructions

### Prerequisites
- **Java**: JDK 21 or newer (Java 26 supported). Verify by running `java -version`.
- **Operating System**: Windows, macOS, or Linux.

---

### Step 1: Run the Backend & Frontend (Out-of-the-Box)

The application includes an embedded persistent database (H2 in MySQL-compatibility mode) with automatic schema generation and safe seed data. Frontend static assets are served directly by Spring Boot.

Open your terminal or PowerShell in the project root:

```powershell
# Using the Maven wrapper in the project:
.\mvnw.cmd spring-boot:run -f backend\pom.xml
```

*Or on macOS / Linux:*
```bash
./mvnw spring-boot:run -f backend/pom.xml
```

Once started:
1. Open your web browser and navigate to: **`http://localhost:8080/`**
2. The Finance System Manager interface will open immediately.

---

### Step 2: Pre-Seeded Demo Accounts

Click the **"Quick Demo Accounts"** buttons on the Sign In modal to test both roles:

| Role | Email Address | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **Student** | `student@university.edu` | `Password123!` | Full control, transaction ledger, budgets, goals, investments, SIPs, intelligence suite |
| **Parent** | `parent@family.com` | `Password123!` | Read-only view of Alex's permitted expenses, link status banner, mutation restricted |

---

### Step 3 (Optional): Connecting to a Production MySQL Database

If you have a local or remote MySQL 8.0+ server running:

1. Create the database in MySQL:
   ```sql
   CREATE DATABASE finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. (Optional) Run `database/schema.sql` and `database/seed.sql` in MySQL Workbench or the `mysql` CLI.
3. Start the application with the `mysql` profile and your environment variables:
   ```powershell
   $env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/finance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:SPRING_DATASOURCE_USERNAME="root"
   $env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"

   .\mvnw.cmd spring-boot:run -f backend\pom.xml -Dspring-boot.run.profiles=mysql
   ```

---

## 5. Running Automated Tests

To run the complete automated test suite (verifying authentication, student CRUD, mathematical calculations, parent read-only 403 enforcement, and CSV duplicate detection):

```powershell
.\mvnw.cmd test -f backend\pom.xml
```

### Verified Test Results:
```
[INFO] Running com.financemanager.AuthAndSecurityTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.financemanager.TransactionAndBudgetTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.financemanager.ParentLinkingAndAccessTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.financemanager.StatementImportAndIntelligenceTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 6. Environment Variables & Secrets Reference

| Variable | Description | Default (Local Dev) |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC connection string | `jdbc:h2:file:./data/financedb;MODE=MySQL...` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` (H2) / `root` (MySQL) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Empty (H2) / configured in env |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 tokens | Built-in development key |
| `GEMINI_API_KEY` | Optional Google Gemini API key for external LLM queries | Unset (uses deterministic ledger NLP fallback) |
