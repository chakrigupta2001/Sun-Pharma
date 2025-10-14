# ==========================================================================================
# ALL USERS REPORT SCRIPT
# Purpose: Fetch all Active Directory user accounts across multiple domains,
# using nearest Domain Controller connection, with retry and fallback logic.
# Output: CSV file and domain-wise user counts in JSON format (for iWorkflow post block)
# ==========================================================================================
 
# 1. Set up the CSV output filename with current timestamp
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'
$reportName = "All_Users_Report_" + $datetime + ".csv"
$outputCsvPath = Join-Path -Path $inputMap.path -ChildPath $reportName
 
# 2. Retrieve comma-separated list of domains passed via input map
$domainControllers = $inputMap.domains -split ','

# 3. Initialize dictionaries and counters
# These will be used to collect and return per-domain and total user counts
$userCounts       = [ordered]@{}  # Total user count per domain
$enabledCounts    = [ordered]@{}  # Enabled users per domain
$disabledCounts   = [ordered]@{}  # Disabled users per domain
 
$totalUserCount      = 0  # Grand total of user accounts across all domains
$totalEnabledCount   = 0  # Grand total of enabled users
$totalDisabledCount  = 0  # Grand total of disabled users
 
# 4. Loop through each domain to query AD users
foreach ($domain in $domainControllers) {
    Write-Output "Processing domain: $domain"
 
    # Step 1: Discover the nearest Domain Controller (DC) for the given domain
    try {
        $nearestDC = (Get-ADDomainController -Discover -NextClosestSite -DomainName $domain).IPv4Address
        Write-Output "Nearest DC for $domain : $nearestDC"
    } catch {
        # In case DC discovery fails, skip this domain and mark counts as 'Discovery failed'
        Write-Output "Failed to discover nearest DC for $domain. Skipping..."
        $userCounts[$domain] = "Discovery failed"
        $enabledCounts[$domain] = "Discovery failed"
        $disabledCounts[$domain] = "Discovery failed"
        continue
    }
 
    # Step 2: Retry mechanism to connect to nearest DC (up to 10 minutes)
    $connected = $false
    $startTime = Get-Date
    $dcToUse = $nearestDC  # Start with nearest DC
 
    while ((Get-Date) -lt $startTime.AddMinutes(10) -and -not $connected) {
        try {
            # Step 3: Query all users from the domain using nearest DC
            Get-ADUser -Filter * -Server $dcToUse -Credential $concreds1 -Properties `
                DisplayName, SamAccountName, Enabled, whenCreated, whenChanged, PasswordNeverExpires, PasswordExpired, `
                PasswordLastSet, AccountExpirationDate, mail, DistinguishedName, EmployeeID, employeeType |

            # Step 4: Project required attributes + Domain and export to CSV (Append mode)
            Select-Object `
                @{Name="Domain"; Expression={$domain}},
                SamAccountName,
                DisplayName,
                Enabled,
                @{Name="PasswordLastSet"; Expression={ if ($_.PasswordLastSet) { ($_.PasswordLastSet).ToString("dd-MMM-yyyy HH:mm") } else { "N/A" } }},
                @{Name="AccountExpirationDate"; Expression={ if ($_.AccountExpirationDate) { $_.AccountExpirationDate.ToString("dd-MMM-yyyy HH:mm") } else { "Never Expire" } }},
                PasswordExpired,
                PasswordNeverExpires,
                @{Name="whenCreated"; Expression={ ($_.whenCreated).ToString("dd-MMM-yyyy HH:mm") }},
                @{Name="whenChanged"; Expression={ ($_.whenChanged).ToString("dd-MMM-yyyy HH:mm") }},
                mail,
                DistinguishedName,
                EmployeeID,
                employeeType |
 
            Export-Csv -Append -NoTypeInformation -Path $outputCsvPath
 
            $connected = $true
        } catch {
            # If nearest DC fails, fallback to default domain name for retry
            Write-Output "Failed to connect to $dcToUse. Retrying..."
            if ($dcToUse -eq $domain) {
                Write-Output "Falling back to original DC: $domain"
            }
            $dcToUse = $domain
            Start-Sleep -Seconds 60
        }
    }
 
    # Step 5: If still not connected after retries, skip this domain
    if (-not $connected) {
        $userCounts[$domain] = "Unable to connect"
        $enabledCounts[$domain] = "Unable to connect"
        $disabledCounts[$domain] = "Unable to connect"
        Write-Output "Failed to connect to $nearestDC after retries. Skipping..."
        continue
    }
 
    # Step 6: Re-import only the current domain's data from CSV to calculate counts
    $users = Import-Csv -Path $outputCsvPath | Where-Object { $_.Domain -eq $domain }
 
    # Step 7: Store domain-wise and total counts
    $userCounts[$domain]     = $users.Count
    $totalUserCount         += $users.Count
 
    $enabledCounts[$domain]  = ($users | Where-Object { $_.Enabled -eq "True" }).Count
    $totalEnabledCount      += $enabledCounts[$domain]
 
    $disabledCounts[$domain] = ($users | Where-Object { $_.Enabled -eq "False" }).Count
    $totalDisabledCount     += $disabledCounts[$domain]
}
 
# 5. Convert all count objects to JSON format for post block parsing in iWorkflow
$userCountsJson       = $userCounts | ConvertTo-Json -Compress
$totalUserJson        = $totalUserCount | ConvertTo-Json -Compress
$enabledCountsJson    = $enabledCounts | ConvertTo-Json -Compress
$totalEnabledJson     = $totalEnabledCount | ConvertTo-Json -Compress
$disabledCountsJson   = $disabledCounts | ConvertTo-Json -Compress
$totalDisabledJson    = $totalDisabledCount | ConvertTo-Json -Compress
 
# 6. Final Output (picked up by iWorkflow post block)
if ($totalUserCount -gt 0) {
    # Success case - output report name + all JSON strings
    Write-Output "Details Saved | $reportName | $userCountsJson | $totalUserJson | $enabledCountsJson | $totalEnabledJson | $disabledCountsJson | $totalDisabledJson"
} else {
    # No user accounts found across all domains
    Write-Output "No user accounts found across all domains | $userCountsJson | $totalUserJson | $enabledCountsJson | $totalEnabledJson | $disabledCountsJson | $totalDisabledJson"
}