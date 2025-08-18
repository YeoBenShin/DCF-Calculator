package com.dcf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinancialDataDto {
    private String ticker;
    private List<BigDecimal> revenue;
    
    @JsonProperty("operating_expense")
    private List<BigDecimal> operatingExpense;
    
    @JsonProperty("operating_income")
    private List<BigDecimal> operatingIncome;
    
    @JsonProperty("operating_cash_flow")
    private List<BigDecimal> operatingCashFlow;
    
    @JsonProperty("net_profit")
    private List<BigDecimal> netProfit;
    
    @JsonProperty("capital_expenditure")
    private List<BigDecimal> capitalExpenditure;
    
    @JsonProperty("free_cash_flow")
    private List<BigDecimal> freeCashFlow;
    
    private List<BigDecimal> eps;
    
    @JsonProperty("total_debt")
    private List<BigDecimal> totalDebt;
    
    @JsonProperty("ordinary_shares_number")
    private List<BigDecimal> ordinarySharesNumber;
    
    @JsonProperty("date_fetched")
    private String dateFetched;

    public FinancialDataDto() {}

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public List<BigDecimal> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<BigDecimal> revenue) {
        this.revenue = revenue;
    }

    public List<BigDecimal> getOperatingExpense() {
        return operatingExpense;
    }

    public void setOperatingExpense(List<BigDecimal> operatingExpense) {
        this.operatingExpense = operatingExpense;
    }

    public List<BigDecimal> getOperatingIncome() {
        return operatingIncome;
    }

    public void setOperatingIncome(List<BigDecimal> operatingIncome) {
        this.operatingIncome = operatingIncome;
    }

    public List<BigDecimal> getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(List<BigDecimal> operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }

    public List<BigDecimal> getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(List<BigDecimal> netProfit) {
        this.netProfit = netProfit;
    }

    public List<BigDecimal> getCapitalExpenditure() {
        return capitalExpenditure;
    }

    public void setCapitalExpenditure(List<BigDecimal> capitalExpenditure) {
        this.capitalExpenditure = capitalExpenditure;
    }

    public List<BigDecimal> getFreeCashFlow() {
        return freeCashFlow;
    }

    public void setFreeCashFlow(List<BigDecimal> freeCashFlow) {
        this.freeCashFlow = freeCashFlow;
    }

    public List<BigDecimal> getEps() {
        return eps;
    }

    public void setEps(List<BigDecimal> eps) {
        this.eps = eps;
    }

    public List<BigDecimal> getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(List<BigDecimal> totalDebt) {
        this.totalDebt = totalDebt;
    }

    public List<BigDecimal> getOrdinarySharesNumber() {
        return ordinarySharesNumber;
    }

    public void setOrdinarySharesNumber(List<BigDecimal> ordinarySharesNumber) {
        this.ordinarySharesNumber = ordinarySharesNumber;
    }

    public String getDateFetched() {
        return dateFetched;
    }

    public void setDateFetched(String dateFetched) {
        this.dateFetched = dateFetched;
    }
    
    public void setDateFetched(LocalDate dateFetched) {
        this.dateFetched = dateFetched != null ? dateFetched.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }
}