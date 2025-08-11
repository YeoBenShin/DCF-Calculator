package com.dcf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "financial_data")
public class FinancialData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Ticker symbol is required")
    @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
    private String ticker;

    @ElementCollection
    @CollectionTable(name = "financial_revenue", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> revenue = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_operating_expense", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> operatingExpense = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_operating_income", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> operatingIncome = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_operating_cash_flow", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> operatingCashFlow = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_net_profit", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> netProfit = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_capital_expenditure", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> capitalExpenditure = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_free_cash_flow", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> freeCashFlow = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_eps", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> eps = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_total_debt", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> totalDebt = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "financial_ordinary_shares", joinColumns = @JoinColumn(name = "financial_data_id"))
    @Column(name = "value")
    @OrderColumn(name = "year_index")
    private List<Double> ordinarySharesNumber = new ArrayList<>();

    @Column(name = "date_fetched", nullable = false)
    @NotNull(message = "Date fetched is required")
    private LocalDate dateFetched;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    public FinancialData() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.dateFetched = LocalDate.now();
    }

    public FinancialData(String ticker) {
        this();
        this.ticker = ticker.toUpperCase();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker != null ? ticker.toUpperCase() : null;
    }

    public List<Double> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<Double> revenue) {
        this.revenue = revenue != null ? revenue : new ArrayList<>();
    }

    public List<Double> getOperatingExpense() {
        return operatingExpense;
    }

    public void setOperatingExpense(List<Double> operatingExpense) {
        this.operatingExpense = operatingExpense != null ? operatingExpense : new ArrayList<>();
    }

    public List<Double> getOperatingIncome() {
        return operatingIncome;
    }

    public void setOperatingIncome(List<Double> operatingIncome) {
        this.operatingIncome = operatingIncome != null ? operatingIncome : new ArrayList<>();
    }

    public List<Double> getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(List<Double> operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow != null ? operatingCashFlow : new ArrayList<>();
    }

    public List<Double> getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(List<Double> netProfit) {
        this.netProfit = netProfit != null ? netProfit : new ArrayList<>();
    }

    public List<Double> getCapitalExpenditure() {
        return capitalExpenditure;
    }

    public void setCapitalExpenditure(List<Double> capitalExpenditure) {
        this.capitalExpenditure = capitalExpenditure != null ? capitalExpenditure : new ArrayList<>();
    }

    public List<Double> getFreeCashFlow() {
        return freeCashFlow;
    }

    public void setFreeCashFlow(List<Double> freeCashFlow) {
        this.freeCashFlow = freeCashFlow != null ? freeCashFlow : new ArrayList<>();
    }

    public List<Double> getEps() {
        return eps;
    }

    public void setEps(List<Double> eps) {
        this.eps = eps != null ? eps : new ArrayList<>();
    }

    public List<Double> getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(List<Double> totalDebt) {
        this.totalDebt = totalDebt != null ? totalDebt : new ArrayList<>();
    }

    public List<Double> getOrdinarySharesNumber() {
        return ordinarySharesNumber;
    }

    public void setOrdinarySharesNumber(List<Double> ordinarySharesNumber) {
        this.ordinarySharesNumber = ordinarySharesNumber != null ? ordinarySharesNumber : new ArrayList<>();
    }

    public LocalDate getDateFetched() {
        return dateFetched;
    }

    public void setDateFetched(LocalDate dateFetched) {
        this.dateFetched = dateFetched;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean hasValidData() {
        return !revenue.isEmpty() && !freeCashFlow.isEmpty() && !eps.isEmpty();
    }

    public boolean isDataStale(int daysThreshold) {
        return dateFetched.isBefore(LocalDate.now().minusDays(daysThreshold));
    }

    public Double getLatestRevenue() {
        return revenue.isEmpty() ? null : revenue.get(revenue.size() - 1);
    }

    public Double getLatestFreeCashFlow() {
        return freeCashFlow.isEmpty() ? null : freeCashFlow.get(freeCashFlow.size() - 1);
    }

    public Double getLatestEps() {
        return eps.isEmpty() ? null : eps.get(eps.size() - 1);
    }

    public int getDataYears() {
        return Math.max(Math.max(revenue.size(), freeCashFlow.size()), eps.size());
    }

    public Double getLatestDebt() {
        return totalDebt.isEmpty() ? null : totalDebt.get(totalDebt.size() - 1);
    }

    public Double getLatestSharesOutstanding() {
        return ordinarySharesNumber.isEmpty() ? null : ordinarySharesNumber.get(ordinarySharesNumber.size() - 1);
    }
}