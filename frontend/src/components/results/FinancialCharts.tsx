import React, { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FinancialData, ChartDataPoint } from '../../types';
import './FinancialCharts.css';

interface FinancialChartsProps {
  financialData: FinancialData;
}

type ChartType = 'revenue' | 'net_income' | 'free_cash_flow' | 'eps';

const FinancialCharts: React.FC<FinancialChartsProps> = ({ financialData }) => {
  const [activeChart, setActiveChart] = useState<ChartType>('revenue');
  const [isLoading, setIsLoading] = useState(false);

  // Transform financial data into chart format
  const transformDataToChart = (data: number[], label: string): ChartDataPoint[] => {
    return data.map((value, index) => ({
      year: `Year ${index + 1}`,
      value: value
    }));
  };

  const getChartData = (type: ChartType): ChartDataPoint[] => {
    switch (type) {
      case 'revenue':
        return transformDataToChart(financialData.revenue, 'Revenue');
      case 'net_income':
        return transformDataToChart(financialData.net_profit, 'Net Income');
      case 'free_cash_flow':
        return transformDataToChart(financialData.free_cash_flow, 'Free Cash Flow');
      case 'eps':
        return transformDataToChart(financialData.eps, 'EPS');
      default:
        return [];
    }
  };

  const getChartTitle = (type: ChartType): string => {
    switch (type) {
      case 'revenue':
        return 'Revenue Trend';
      case 'net_income':
        return 'Net Income Trend';
      case 'free_cash_flow':
        return 'Free Cash Flow Trend';
      case 'eps':
        return 'Earnings Per Share (EPS) Trend';
      default:
        return '';
    }
  };

  const getChartColor = (type: ChartType): string => {
    switch (type) {
      case 'revenue':
        return '#8884d8';
      case 'net_income':
        return '#82ca9d';
      case 'free_cash_flow':
        return '#ffc658';
      case 'eps':
        return '#ff7c7c';
      default:
        return '#8884d8';
    }
  };

  const formatValue = (value: number, type: ChartType): string => {
    if (type === 'eps') {
      return `$${value.toFixed(2)}`;
    }
    
    // Format large numbers (in billions/millions)
    if (Math.abs(value) >= 1e9) {
      return `$${(value / 1e9).toFixed(1)}B`;
    } else if (Math.abs(value) >= 1e6) {
      return `$${(value / 1e6).toFixed(1)}M`;
    } else if (Math.abs(value) >= 1e3) {
      return `$${(value / 1e3).toFixed(1)}K`;
    }
    return `$${value.toFixed(0)}`;
  };

  const handleChartChange = (type: ChartType) => {
    setIsLoading(true);
    setActiveChart(type);
    // Simulate loading for smooth transition
    setTimeout(() => setIsLoading(false), 300);
  };

  const chartData = getChartData(activeChart);
  const hasData = chartData.length > 0 && chartData.some(point => point.value !== 0);

  return (
    <div className="financial-charts">
      <div className="charts-header">
        <h2>Financial Performance</h2>
        <p className="data-source">Data for {financialData.ticker} • Last updated: {new Date(financialData.date_fetched).toLocaleDateString()}</p>
      </div>

      {/* Chart Navigation */}
      <div className="chart-navigation">
        <button
          className={`nav-button ${activeChart === 'revenue' ? 'active' : ''}`}
          onClick={() => handleChartChange('revenue')}
        >
          Revenue
        </button>
        <button
          className={`nav-button ${activeChart === 'net_income' ? 'active' : ''}`}
          onClick={() => handleChartChange('net_income')}
        >
          Net Income
        </button>
        <button
          className={`nav-button ${activeChart === 'free_cash_flow' ? 'active' : ''}`}
          onClick={() => handleChartChange('free_cash_flow')}
        >
          Free Cash Flow
        </button>
        <button
          className={`nav-button ${activeChart === 'eps' ? 'active' : ''}`}
          onClick={() => handleChartChange('eps')}
        >
          EPS
        </button>
      </div>

      {/* Chart Display */}
      <div className="chart-container">
        {isLoading ? (
          <div className="chart-loading">
            <div className="loading-spinner"></div>
            <p>Loading chart...</p>
          </div>
        ) : !hasData ? (
          <div className="chart-error">
            <p>No data available for {getChartTitle(activeChart)}</p>
          </div>
        ) : (
          <div className="chart-wrapper">
            <h3 className="chart-title">{getChartTitle(activeChart)}</h3>
            <ResponsiveContainer width="100%" height={400}>
              <LineChart
                data={chartData}
                margin={{
                  top: 20,
                  right: 30,
                  left: 20,
                  bottom: 20,
                }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis 
                  dataKey="year" 
                  stroke="#666"
                  fontSize={12}
                />
                <YAxis 
                  stroke="#666"
                  fontSize={12}
                  tickFormatter={(value) => formatValue(value, activeChart)}
                />
                <Tooltip
                  formatter={(value: number) => [formatValue(value, activeChart), getChartTitle(activeChart)]}
                  labelStyle={{ color: '#333' }}
                  contentStyle={{
                    backgroundColor: '#fff',
                    border: '1px solid #ccc',
                    borderRadius: '4px'
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke={getChartColor(activeChart)}
                  strokeWidth={3}
                  dot={{ fill: getChartColor(activeChart), strokeWidth: 2, r: 6 }}
                  activeDot={{ r: 8, stroke: getChartColor(activeChart), strokeWidth: 2 }}
                  name={getChartTitle(activeChart)}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* Chart Summary */}
      {hasData && !isLoading && (
        <div className="chart-summary">
          <div className="summary-stats">
            <div className="stat-item">
              <label>Latest Value</label>
              <span>{formatValue(chartData[chartData.length - 1]?.value || 0, activeChart)}</span>
            </div>
            <div className="stat-item">
              <label>Trend</label>
              <span className={chartData.length > 1 && chartData[chartData.length - 1].value > chartData[0].value ? 'positive' : 'negative'}>
                {chartData.length > 1 && chartData[chartData.length - 1].value > chartData[0].value ? '↗ Growing' : '↘ Declining'}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FinancialCharts;