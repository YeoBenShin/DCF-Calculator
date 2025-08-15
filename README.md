# 📊 DCF Calculator

A **web application** for calculating the fair value of stocks using the **Discounted Cash Flow (DCF)** model.  
Includes interactive charts, financial data scraping, customizable parameters, and a personal watchlist.

---

## ✨ Features

- 🔐 **User Authentication** – Secure login with JWT and Spring Security
- 🔍 **Stock Search** – Retrieve stock data with web scraping via JSoup
- 📈 **DCF Valuation** – Fully customizable DCF parameters
- 📊 **Interactive Charts** – Visualize trends with Recharts
- 📋 **Personal Watchlist** – Save and manage tracked companies
- ⚡ **Robust Error Handling** – Clear feedback and validation

---

## 🛠 Tech Stack

**Frontend**
- React (TypeScript)
- Recharts
- Axios

**Backend**
- Spring Boot
- Spring Security + JWT
- JSoup
- Firebase (Database)

**Testing**
- Jest, React Testing Library (Frontend)
- JUnit (Backend)

---

## 🚀 Getting Started

### 1️⃣ Clone the repository

git clone https://github.com/YeoBenShin/DCF-Calculator.git
cd DCF-Calculator

2️⃣ Frontend Setup

cd frontend
npm install
npm run build
npm start

3️⃣ Backend Setup

cd backend
mvn clean compile
mvn spring-boot:run
