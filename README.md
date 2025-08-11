# DCF Calculator

A web application for calculating the fair value of stocks using the Discounted Cash Flow (DCF) model.

## Project Structure

```
dcf-calculator/
├── frontend/                 # React TypeScript frontend
│   ├── src/
│   │   ├── types/           # TypeScript type definitions
│   │   ├── components/      # React components (to be created)
│   │   ├── services/        # API service layer (to be created)
│   │   └── utils/           # Utility functions (to be created)
│   ├── package.json
│   └── tsconfig.json
├── backend/                  # Spring Boot backend
│   ├── src/main/java/com/dcf/
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities (to be created)
│   │   ├── service/         # Business logic services (to be created)
│   │   ├── controller/      # REST controllers (to be created)
│   │   └── config/          # Configuration classes (to be created)
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
└── .kiro/specs/dcf-calculator/
    ├── requirements.md
    ├── design.md
    └── tasks.md
```

## Getting Started

### Frontend
```bash
cd frontend
npm install
npm start
```

### Backend
```bash
cd backend
mvn spring-boot:run
```

## Features

- User authentication with JWT
- Stock ticker search and financial data scraping
- DCF calculation with customizable parameters
- Interactive charts for financial visualization
- Personal watchlist management
- Comprehensive error handling

## Technology Stack

- **Frontend:** React, TypeScript, Recharts, Axios
- **Backend:** Spring Boot, Spring Security, JWT, JSoup
- **Database:** Firebase
- **Testing:** Jest, React Testing Library, JUnit