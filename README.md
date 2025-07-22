# Orbin Bank - Enterprise Banking Platform (Backend)

Welcome to the backend repository for Project Orbin Bank, a comprehensive, enterprise-grade digital banking platform built on the Jakarta EE 10 framework. This project demonstrates a secure, scalable, and feature-rich system for handling core banking operations, designed to be consumed by a modern frontend application.

**Developed by:** R. Ashan Himantha Rathnayaka
**GitHub Profile:** [github.com/AshanHimantha](https://github.com/AshanHimantha)

---

### **Companion Repositories**
*   **Backend (This Repo):** [[https://github.com/AshanHimantha/enterprise-banking-platform](https://github.com/AshanHimantha/enterprise-banking-platform)](https://github.com/AshanHimantha/enterprise-banking-platform)
*   **Frontend (React UI):** [https://github.com/AshanHimantha/baking-webapp](https://github.com/AshanHimantha/baking-webapp)

---

## 🚀 Features

This backend powers a full suite of modern banking features, including:

*   **Secure User Lifecycle & Management:**
    *   JWT-based Authentication with a custom `HttpAuthenticationMechanism`.
    *   Role-Based Access Control (`CUSTOMER`, `EMPLOYEE`, `ADMIN`, `NONE`).
    *   Full KYC submission (data and images) and a secure admin approval workflow.
    *   Asynchronous email verification via Zoho Mail (SMTP).
    *   Secure APIs for users to manage profiles, passwords, and PINs.
    *   Comprehensive admin panel for managing users, employees, and roles.
*   **Core Banking & Transactions:**
    *   Multi-account management (`SAVING`, `CURRENT`) with creation limits.
    *   Secure, atomic fund transfers and bill payments using transactional EJBs with pessimistic locking to prevent race conditions.
    *   Detailed, filterable, and paginated transaction history.
    *   Employee-assisted cash deposit system with a full audit trail.
*   **Automated & Asynchronous Processes:**
    *   **EJB Timers** for recurring payments and interest calculation.
    *   Dynamic, level-based daily interest accrual and monthly payout, accurately handling leap years.
    *   **JMS Queue & MDBs** for resilient, asynchronous generation of monthly PDF statements.
*   **Virtual Card Management:**
    *   Full lifecycle management: create, freeze, terminate, and manage virtual debit cards.
    *   Securely set/change PINs and spending limits.
    *   Password-protected endpoint to reveal full card details.
*   **Document & Report Generation:**
    *   Dynamic generation of secure, password-protected PDF statements and receipts from HTML/CSS templates using iText 7.
    *   Publicly accessible, secure endpoints for serving user avatars and biller logos.
*   **Administrative & Monitoring Tools:**
    *   An analytics dashboard for admins with system-wide KPIs and chart data.
    *   A powerful transaction monitoring API with advanced search and filtering.
    *   **EJB Interceptors** for global logging (`ejb-jar.xml`) and selective, annotation-based auditing (`@Auditable`).

---

## 🛠️ Technology Stack

*   **Framework:** Jakarta EE 10
    *   **Business Logic:** Enterprise JavaBeans (EJB) 4.0 (Stateless, Singleton, MDB)
    *   **API Layer:** JAX-RS 3.1 (RESTful APIs)
    *   **Persistence:** Jakarta Persistence (JPA) 3.1 with Hibernate
    *   **Security:** Jakarta Security 3.0
    *   **Messaging:** Jakarta Messaging Service (JMS) 3.1
*   **Application Server:** Payara Server 6 Community Edition
*   **Database:** MySQL 8
*   **PDF Generation:** iText 7 (pdfHTML)
*   **Build Tool:** Apache Maven
*   **Version Control:** Git

---

## ⚙️ Setup and Deployment Guide

Follow these steps to set up the environment and deploy the application.

### Prerequisites

*   **Java Development Kit (JDK):** Version 17 or higher.
*   **Apache Maven:** Version 3.8 or higher.
*   **MySQL Server:** Version 8 or higher.
*   **Payara Server:** Version 6 Community Edition.
*   **Git:** For cloning the repository.
*   **Postman:** For API testing.

### Step 1: Clone the Repository

```bash
git clone https://github.com/AshanHimantha/enterprise-banking-platform.git
cd enterprise-banking-platform

### Step 2: Database Setup
Log in to your MySQL server command line or client and run the following SQL commands. Remember to replace 'YourStrongPassword' with your own secure password.

CREATE DATABASE banking_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'bankinguser'@'localhost' IDENTIFIED BY 'YourStrongPassword';

GRANT ALL PRIVILEGES ON banking_app_db.* TO 'bankinguser'@'localhost';

FLUSH PRIVILEGES;

EXIT;


## Step 3: Payara Server Configuration

These steps are performed in the Payara Admin Console: [http://localhost:4848](http://localhost:4848)

### 3.1. JDBC Connection Pool

1. Navigate to: `Resources -> JDBC -> JDBC Connection Pools`
2. Click **New**
3. Fill in:
   - **Pool Name**: `BankingAppPool`
   - **Resource Type**: `javax.sql.DataSource`
   - **Database Driver Vendor**: `MySql`
4. Click **Next**
5. Scroll to the bottom and set the following Additional Properties:
   - `user`: `bankinguser`
   - `password`: `YourStrongPassword`
   - `databaseName`: `banking_app_db`
   - `serverName`: `localhost`
   - `portNumber`: `3306`
6. Click **Finish**

### 3.2. JDBC Resource

1. Navigate to: `Resources -> JDBC -> JDBC Resources`
2. Click **New**
3. Fill in:
   - **JNDI Name**: `jdbc/bankingDB`
   - **Pool Name**: Select `BankingAppPool` from the dropdown
4. Click **OK**
5. Go back to the JDBC Connection Pools page, select `BankingAppPool`, and click **Ping**
6. You should see a **"Ping Succeeded"** message

### 3.3. JavaMail Session (Zoho Mail)

1. Navigate to: `Resources -> JavaMail Sessions`
2. Click **New**
3. Fill in:
   - **JNDI Name**: `mail/zoho`
   - **Mail Host**: `smtp.zoho.com`
   - **Default User**: `you@yourdomain.com`
   - **Default Sender Address**: `you@yourdomain.com`
4. Scroll to Additional Properties and add:

| Property Name              | Value                              |
|---------------------------|------------------------------------|
| `mail.smtp.auth`          | `true`                             |
| `mail.smtp.port`          | `587`                              |
| `mail.smtp.starttls.enable` | `true`                         |
| `mail.smtp.ssl.enable`    | `false`                            |
| `mail.smtp.password`      | `YourAppSpecific16CharPassword`    |

5. Ensure **Status** is set to **Enabled** and click **OK**

### 3.4. JMS Destination Resource (Statement Queue)

1. Navigate to: `Resources -> JMS Resources -> Destination Resources`
2. Click **New**
3. Fill in:
   - **JNDI Name**: `jms/statementQueue`
   - **Physical Destination Name**: `StatementQueue`
   - **Resource Type**: `jakarta.jms.Queue`
4. Click **OK**

---

## Step 4: Build and Deploy

### Build the Project

```bash
mvn clean install

## Deploy via asadmin (Command Line)
# Navigate to Payara bin directory if not in PATH
asadmin deploy /path/to/your/project/banking-ear/target/enterprise-banking-platform-1.0.ear


