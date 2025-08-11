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

## 📂 Project Structure

dcf-calculator/
├── frontend/                 # React TypeScript frontend
│   ├── src/
│   │   ├── types/            # TypeScript type definitions
│   │   ├── components/       # React components
│   │   ├── services/         # API service layer
│   │   └── utils/            # Utility functions
│   ├── package.json
│   └── tsconfig.json
├── backend/                  # Spring Boot backend
│   ├── src/main/java/com/dcf/
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── entity/           # JPA entities
│   │   ├── service/          # Business logic services
│   │   ├── controller/       # REST controllers
│   │   └── config/           # Configuration classes
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
└── .kiro/specs/dcf-calculator/
├── requirements.md
├── design.md
└── tasks.md

---

## 🚀 Getting Started

### 1️⃣ Clone the repository

git clone https://github.com/YeoBenShin/DCF-Calculator.git
cd DCF-Calculator

2️⃣ Frontend Setup

cd frontend
npm install
npm start

3️⃣ Backend Setup

cd backend
mvn spring-boot:run


⸻

📚 How It Works
	1.	Login/Register → Authenticate with JWT
	2.	Search Stock → Fetch financial data from external sources
	3.	Calculate DCF → Apply model with user-defined assumptions
	4.	View Charts → Interactive trend visualizations
	5.	Manage Watchlist → Track saved companies

⸻

📅 Roadmap
	•	Multi-currency support
	•	Export results to PDF/Excel
	•	Advanced chart analytics
	•	Mobile-responsive UI

⸻

🤝 Contributing

We welcome contributions!
Please fork this repository, create a new branch for your feature, and submit a pull request.

⸻
