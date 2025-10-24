# ====================================================================================
# Script Name : Disabled_Computers_60Days.ps1
# Description : Retrieves disabled computer accounts from multiple domains
#               where 'WhenChanged' is older than 60 days.
# Uses nearest DC discovery, fallback to original DC, retry mechanism, and direct CSV append.
# Author        : Chakravarthi Pulikonda
# Created Date  : 15-04-2025
# Notes : Structured for consistency with other similar scripts.
# ====================================================================================
 
# --- Define cutoff date for 60 days ---
$cutoffDate = (Get-Date).AddDays(-60)
 
# --- Define output file path with timestamp ---
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'
$reportName = "Disabled_Computers_60Days_" + $datetime + ".csv"
$outputCsvPath = Join-Path -Path $inputMap.path -ChildPath $reportName
 
# --- Get list of domains from input reference table ---
$domainControllers = $inputMap.domains -split ','
 
# --- Initialize ordered dictionaries for per-domain and total counts ---
$computercounts = [ordered]@{}
$totalComputerCount = 0
 
# ====================================================================================
# Main Processing Loop - Iterate through each domain
# ====================================================================================
foreach ($domain in $domainControllers) {
    Write-Output "Processing domain: $domain"
 
    # --- Step 1: Discover nearest DC for the domain ---
    try {
        $nearestDC = (Get-ADDomainController -Discover -NextClosestSite -DomainName $domain).IPv4Address
        Write-Output "Nearest DC for $domain : $nearestDC"
    } catch {
        Write-Output "Failed to discover nearest DC for $domain. Skipping..."
        $computercounts[$domain] = "Discovery failed"
        continue
    }
 
    # --- Step 2: Initialize retry variables ---
    $connected = $false
    $startTime = Get-Date
    $dcToUse = $nearestDC # Start with nearest DC
 
    # ====================================================================================
    # Step 3: Retry mechanism - Attempt connection for up to 10 minutes
    # ====================================================================================
    while ((Get-Date) -lt $startTime.AddMinutes(10) -and -not $connected) {
        try {
            # --- Step 4: Retrieve disabled computers older than 60 days ---
            Get-ADComputer -Filter { Enabled -eq $false -and WhenChanged -ge $cutoffDate } `
                -Server $dcToUse `
                -Credential $concreds1 `
                -Properties Name, DNSHostName, IPv4Address, Enabled, LastLogonDate, LastLogonTimestamp, WhenCreated, WhenChanged, OperatingSystem, DistinguishedName, modifyTimeStamp |
                Select-Object `
                    @{Name="Domain";Expression={$domain}},
                    Name,
                    DNSHostName,
                    IPv4Address,
                    Enabled,
                    @{Name="WhenCreated";Expression={ if ($_.WhenCreated) { $_.WhenCreated.ToString("dd-MMM-yyyy HH:mm") } else { "" } }},
                    @{Name="WhenChanged";Expression={ if ($_.WhenChanged) { $_.WhenChanged.ToString("dd-MMM-yyyy HH:mm") } else { "" } }},
                    @{Name="ModifyTimeStamp";Expression={ if ($_.modifyTimeStamp) { $_.modifyTimeStamp.ToString("dd-MMM-yyyy HH:mm") } else { "" } }},
                    OperatingSystem,
                    DistinguishedName |
                Export-Csv -Append -NoTypeInformation -Path $outputCsvPath
 
            # --- Mark connection as successful ---
            $connected = $true
        } catch {
            Write-Output "Failed to connect to $dcToUse. Retrying..."
            
            # --- Step 5: Fallback to original DC if nearest DC fails ---
            if ($dcToUse -eq $nearestDC) {
                Write-Output "Falling back to original DC: $domain"
                $dcToUse = $domain
            }
            
            # --- Wait before retrying ---
            Start-Sleep -Seconds 60
        }
    }
 
    # --- Step 6: Handle failure after retries ---
    if (-not $connected) {
        $computercounts[$domain] = "Unable to connect"
        Write-Output "Failed to connect to $nearestDC after retries. Skipping..."
        continue
    }
 
    # --- Step 7: Count results for current domain ---
    $computers = Import-Csv -Path $outputCsvPath | Where-Object { $_.Domain -eq $domain }
    $computercounts[$domain] = $computers.Count
    $totalComputerCount += $computers.Count
}
 
# ====================================================================================
# Step 8: Convert counts to JSON for iWorkflow output
# ====================================================================================
$computerCountsJson = $computercounts | ConvertTo-Json -Compress
$totalComputerJson = $totalComputerCount | ConvertTo-Json -Compress
 
# ====================================================================================
# Step 9: Final Output - Pass results to iWorkflow
# ====================================================================================
if ($totalComputerCount -gt 0) {
    Write-Output "Details Saved | $reportName | $computerCountsJson | $totalComputerJson"
} else {
    Write-Output "No Disabled computers found for the past 60 days across all domains | $computerCountsJson | $totalComputerJson"
}


<#
# Main Code for Disabled Computers (60+ Days)
 
# Define the cutoff date (60 days ago)
$cutoffDate = (Get-Date).AddDays(-60)
 
# Define the output file path
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'
$reportName = "Disabled_Computers_60Days_" + $datetime + ".csv"
 
# Get list of Domains from reference table
$domainControllers = $inputMap.domains -split ','
 
# Initialize the result array and count dictionaries
$results = @()
$computercounts = [ordered]@{}
$totalComputerCount = 0
 
# Loop through each domain controller
foreach ($dc in $domainControllers) {
    Write-Output "Processing domain: $dc"
    $connected = $false
    $startTime = Get-Date
 
    while ((Get-Date) -lt $startTime.AddMinutes(10) -and -not $connected) {
        try {
            $computers = Get-ADComputer -Filter { Enabled -eq $false -and WhenChanged -le $cutoffDate } -Server $dc -Credential $concreds1 -Properties Name, DistinguishedName, LastLogonDate, LastLogonTimestamp, WhenCreated, WhenChanged, OperatingSystem, OperatingSystemVersion, SamAccountName, ManagedBy, Enabled, modifyTimeStamp
            $connected = $true
        } catch {
            Write-Output "Failed to connect to $dc. Retrying..."
            Start-Sleep -Seconds 60
        }
    }
 
    if (-not $connected) {
        $computercounts[$dc] = "Unable to connect"
        Write-Output "Failed to connect to $dc after retries. Skipping..."
        continue
    }
 
    $computercounts[$dc] = 0
 
    foreach ($computer in $computers) {
        $results += [PSCustomObject]@{
            Domain               = $dc
            ComputerName         = $computer.Name
            WhenCreated          = if ($computer.WhenCreated) { $computer.WhenCreated.ToString("dd-MMM-yyyy HH:mm") } else { "" }
            WhenChanged          = if ($computer.WhenChanged) { $computer.WhenChanged.ToString("dd-MMM-yyyy HH:mm") } else { "" }
            modifyTimeStamp      = if ($computer.modifyTimeStamp) { $computer.modifyTimeStamp.ToString("dd-MMM-yyyy HH:mm") } else { "" }
            #LastLogonDate        = $computer.LastLogonDate
            #LastLogonTimestamp   = [datetime]::FromFileTime($computer.LastLogonTimestamp)
            OperatingSystem      = $computer.OperatingSystem
            OperatingSystemVersion = $computer.OperatingSystemVersion
            SamAccountName       = $computer.SamAccountName
            DistinguishedName    = $computer.DistinguishedName
            Enabled              = $computer.Enabled
        }
        $computercounts[$dc]++
        $totalComputerCount++
    }
}
 
$outputCsvPath = Join-Path -Path $inputMap.path -ChildPath $reportName
$computerCountsJson = $computercounts | ConvertTo-Json -Compress
$totalComputerJson = $totalComputerCount | ConvertTo-Json -Compress
 
if ($results.Count -gt 0) {
    $results | Export-Csv -Path $outputCsvPath -NoTypeInformation
    Write-Output "Details Saved | $reportName | $computerCountsJson | $totalComputerJson"
} else {
    Write-Output "No Disabled computers found for the past 60 days across all domains | $computerCountsJson | $totalComputerJson"
}
#>


<# Post Block to send iworkflow code

output = outputMap.outputStream;
if(output.contains("No Disabled computers found for the past 60 days")){
    splitoutput = output.split("\\|")
    hashMap = splitoutput[1].trim()
    totalcounts =splitoutput[2].trim()
    outputMap << [hashMap:hashMap]
    outputMap << [totalcounts:totalcounts]
    outputMap << [outputStream: output]
} else{
    splitoutput = output.split("\\|")
    filename = splitoutput[1].trim()
    hashMap = splitoutput[2].trim()
    totalcounts = splitoutput[3].trim()
    outputMap << [filename:filename]
    outputMap << [hashMap:hashMap]
    outputMap << [totalcounts:totalcounts]
    outputMap << [outputStream: output]
}
#>