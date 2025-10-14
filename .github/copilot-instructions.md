# Copilot Instructions for Sun-Pharma

## Project Overview
This repository contains scripts and workflows for Active Directory reporting and automation, primarily focused on user and computer account status over time (e.g., 60/180 days inactive, disabled, or not reporting). The codebase is organized by report type and time window, with each directory containing relevant scripts and workflow files.

## Directory Structure & Key Files
- `180days inactive computers/`, `60days inactive computer/`, `60days inacive users/`: Contain PowerShell scripts (`*.ps1`), Groovy workflow files (`*.groovy`), and encoded file utilities for reporting on inactive/disabled accounts.
- `All Users Report/`, `Computer Account Created Disabled Reports/`: Aggregate and specialized reports for all users and computer accounts, with similar script/workflow structure.

## Patterns & Conventions
- **Script Naming**: Scripts are named for their function and time window (e.g., `Disabled_Computers_180Days.ps1`, `Inactive Computers Report 60days.groovy`).
- **Workflow Automation**: Groovy files (e.g., `iworkflow.groovy`) automate report generation and may integrate with external systems.
- **Encoded File Utilities**: Each report directory includes a `get Encoded File` utility for handling encoded data, likely for secure transfer or processing.
- **PowerShell Usage**: PowerShell scripts are used for querying and reporting on Active Directory objects. They may require domain admin privileges and execution policy adjustments.

## Developer Workflows
- **Running Reports**: Execute PowerShell scripts directly in their respective directories. Example:
  ```bash
  pwsh ./180days\ inactive\ computers/Disabled_Computers_180Days.ps1
  ```
- **Workflow Execution**: Groovy workflow files can be run with a compatible Groovy runtime or integrated automation platform.
- **Data Handling**: Use the `get Encoded File` utility for encoding/decoding report data as needed.

## Integration Points
- **Active Directory**: All scripts assume access to an AD environment.
- **Automation Platforms**: Groovy workflows may be triggered by external schedulers or CI/CD tools.

## Project-Specific Guidance
- Maintain the directory-based separation for each report type and time window.
- When adding new reports, follow the existing naming and structure conventions.
- Document any new workflow or integration steps in the relevant directory.
- If modifying encoded file handling, update all affected `get Encoded File` utilities for consistency.

## Example: Adding a New 90-Day Inactive User Report
1. Create a new directory: `90days inactive users/`
2. Add PowerShell and Groovy scripts following the naming pattern.
3. Include a `get Encoded File` utility if needed.
4. Document usage in the directory's README (if applicable).

---
For questions or unclear conventions, review existing scripts and workflows in the relevant directory for examples.
