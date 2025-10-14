# Define the output file path with current date and time
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'

# Set the report name for all computer accounts
$reportNameAllComputers = "All_ComputerAccountsReport_" + $datetime + ".csv"
 
# Get list of Domains from reference table
$domainControllers = $inputMap.domains -split ','
#$domainControllers = @("spapl.com","spil.com","ranbaxy.com","il.taro.corp","alkaloida.com","ca.taro.corp","spiinc.com", "taro.corp","pharmalucence.com")
#$domainControllers = @("ranbaxy.com")
 
# Initialize an array to store the results
$results = @()
 
# Loop through each domain controller
foreach ($dc in $domainControllers) {
    $connected = $false
    $startTime = Get-Date
 
    while ((Get-Date) -lt $startTime.AddMinutes(10) -and -not $connected) {
        # Try server connection
        try {
            # Retrieve all computer accounts with all properties
            $computers = Get-ADComputer -Filter * -Server $dc  -Properties *
            $connected = $true
 
        } catch {
            # Handle connection error
            Write-Host "Failed to connect to $dc. Retrying..."
            Start-Sleep -Seconds 60
        }
    }
 
    if (-not $connected) {
        Write-Host "Failed to connect to $dc after multiple attempts. Skipping..."
        continue
    }

    if ($connected) {
    # Add retrieved computer accounts directly to results
    $results += $computers
    }
}
 
# Assigning path to generate csv file
$outputCsvPathAllComputers = $inputMap.path + $reportNameAllComputers
#$results | Export-Csv -Path "C:\\Temp\AllComputerAccounts.csv" -NoTypeInformation
 
# Check if data is empty
if ($results.Count -gt 0) {
    # Export results to CSV
    $results | Export-Csv -Path $outputCsvPathAllComputers -NoTypeInformation
    Write-Output "Deatails saved | $reportNameAllComputers"
} else {
    Write-Output "No computer accounts found."
}