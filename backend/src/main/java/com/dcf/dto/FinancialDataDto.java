package com.dcf.dto;

import java.time.LocalDate;
import java.util.List;

public class FinancialDataDto {
    private String ticker;
    private List<Double> revenue;
    private List<Double> operatingExpense;
    private List<Double> operatingIncome;
    private List<Double> operatingCashFlow;
    private List<Double> netProfit;
    private List<Double> capitalExpenditure;
    private List<Double> freeCashFlow;
    private List<Double> eps;
    private List<Double> totalDebt;
    private List<Double> ordinarySharesNumber;
    private LocalDate dateFetched;

    public FinancialDataDto() {}

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public List<Double> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<Double> revenue) {
        this.revenue = revenue;
    }

    public List<Double> getOperatingExpense() {
        return operatingExpense;
    }

    public void setOperatingExpense(List<Double> operatingExpense) {
        this.operatingExpense = operatingExpense;
    }

    public List<Double> getOperatingIncome() {
        return operatingIncome;
    }

    public void setOperatingIncome(List<Double> operatingIncome) {
        this.operatingIncome = operatingIncome;
    }

    public List<Double> getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(List<Double> operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }

    public List<Double> getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(List<Double> netProfit) {
        this.netProfit = netProfit;
    }

    public List<Double> getCapitalExpenditure() {
        return capitalExpenditure;
    }

    public void setCapitalExpenditure(List<Double> capitalExpenditure) {
        this.capitalExpenditure = capitalExpenditure;
    }

    public List<Double> getFreeCashFlow() {
        return freeCashFlow;
    }

    public void setFreeCashFlow(List<Double> freeCashFlow) {
        this.freeCashFlow = freeCashFlow;
    }

    public List<Double> getEps() {
        return eps;
    }

    public void setEps(List<Double> eps) {
        this.eps = eps;
    }

    public List<Double> getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(List<Double> totalDebt) {
        this.totalDebt = totalDebt;
    }

    public List<Double> getOrdinarySharesNumber() {
        return ordinarySharesNumber;
    }

    public void setOrdinarySharesNumber(List<Double> ordinarySharesNumber) {
        this.ordinarySharesNumber = ordinarySharesNumber;
    }

    public LocalDate getDateFetched() {
        return dateFetched;
    }

    public void setDateFetched(LocalDate dateFetched) {
        this.dateFetched = dateFetched;
    }
}