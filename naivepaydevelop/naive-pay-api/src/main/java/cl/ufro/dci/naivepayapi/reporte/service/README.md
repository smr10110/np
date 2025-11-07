# Service Package

This package implements the business logic for reporting, analytics, and data export.

## Main Classes

- **ReportService**: Handles filtering of transactions based on user criteria and provides CSV export functionality. It interacts with the repository to fetch data and formats it for the client.
- **SpendingAnalysisService**: Aggregates and groups transaction data for spending analysis, supporting grouping by category or transaction type and by day or month. Returns structured reports for visualization.
- **SpendingAnalysis**: Utility class for analyzing a list of transactions, providing methods to calculate total spent, spending by category, and spending over time.
- **UsageTrends**: Calculates usage statistics, such as the average monthly spending, by grouping transactions by month and computing averages.

Services are called by controllers to process business logic and return results to the frontend.
