# Controller Package

This package contains REST controllers that expose endpoints for the reporting features of the application.

## Main Classes

- **ReportController**: Handles HTTP requests for retrieving and exporting filtered transaction reports. It receives filter criteria and user information, delegates the logic to the service layer, and returns the results or CSV files.
- **SpendingAnalysisController**: Provides endpoints for analyzing user spending. It allows grouping and aggregating transactions by category or date, using flexible parameters.
- **UsageTrendsController**: Offers endpoints to analyze user transaction trends, such as calculating the average monthly spending.

Controllers receive requests from the frontend (Angular), extract parameters, and call the appropriate service methods to process and return the data.
