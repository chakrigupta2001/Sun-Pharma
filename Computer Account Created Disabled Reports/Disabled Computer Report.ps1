# Disabled Computer Report
# Define the date for checking
$dateToCheckStart = (Get-Date).AddDays(-1).Date
$dateToCheckEnd = $dateToCheckStart.AddDays(1).Date
 
# Define the output file path with current date and time
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'
 
# Set the report name
$reportNameDailyComputerDisabled = "Daily_ComputerDisabledReport_" + $datetime + ".csv"
 
# Get list of Domains from reference table
$domainControllers = $inputMap.domains -split ','
#$domainControllers = @("ranbaxy.com")
#$domainControllers = @("spapl.com","spil.com","ranbaxy.com","il.taro.corp","alkaloida.com","ca.taro.corp","spiinc.com", "taro.corp","pharmalucence.com")

 
# Initialize an array to store the results
$results = @()
$computercounts = [ordered]@{}
$totalCounts = @{}

# Initialize total counts for created computers and total users
$totalCreatedComputers = 0
$totalUsersInDomain = 0
 
# Loop through each domain controller
foreach ($dc in $domainControllers) {
    $connected = $false
    $startTime = Get-Date
 
    while ((Get-Date) -lt $startTime.AddSeconds(10) -and -not $connected) {
        # Try server connection
        try {
            # Retrieve the disabled computer accounts modified in the last day
            $disabledComputers = Get-ADComputer -Server $dc -credential $concreds1 -Filter { Enabled -eq $false -and WhenChanged -eq $dateToCheckStart } -Properties Name, DistinguishedName, LastLogonDate, OperatingSystem, OperatingSystemVersion, WhenCreated, WhenChanged 
 
            $totalUsersCount = (Get-ADComputer -Filter * -Server $dc -credential $concreds1).Count # Total users count
            # Mark as connected
            $connected = $true
        } catch {
            # Handle connection errors
            echo "Failed to connect to $dc . Retrying..."
            Start-Sleep -Seconds 60
        }
    }
 
    if (-not $connected) {
        $computercounts[$dc] = "Unable to connect"
        $totalCounts[$dc] = "Unable to connect"
        echo "Failed to connect to $dc. Skipping..."
        continue
    }
 
    if ($connected) {
        $totalCounts[$dc] = $totalUsersCount
        $computercounts[$dc] = 0
 
        # Loop through each disabled computer and add to the results
        foreach ($computer in $disabledComputers) {
            # To store the results
            $results += [PSCustomObject]@{
                ComputerName = $computer.Name
                LastLogonDate = $computer.LastLogonDate
                Domain = $dc
                DistinguishedName = $computer.DistinguishedName
                OperatingSystem = $computer.OperatingSystem
                OperatingSystemVersion = $computer.OperatingSystemVersion
                WhenCreated = $computer.WhenCreated
                WhenChanged = $computer.WhenChanged
                Enabled = $computer.Enabled
            }
            $computercounts[$dc]++
        }
        # Update total created computers count
        $totalCreatedComputers += $computercounts[$dc]
 
        # Count total users in the domain (all computers)
        $totalUsersInDomain += (Get-ADComputer -Filter * -Server $dc -credential $concreds1).Count
    }
}


# Assigning path to generate csv file
$outputCsvPathComputerDisabled = $inputMap.path + $reportNameDailyComputerDisabled
#$results | Export-Csv -Path "C:\\Temp\Disabled.csv" -NoTypeInformation
 
# Preparing JSON data for errors if needed
$computerDisabledCounts = $computercounts | ConvertTo-Json
$totalcountsjson = $totalCounts | ConvertTo-Json

 
#if ($computercounts.Values -contains "Unable to connect") {
#    Write-Output "Server contain unable to connect error | $jsondictionary | $totalcountsjson"
#}
 
# Check if data is empty
if ($results.Count -gt 0) {
    # Export results to CSV
    $results | Export-Csv -Path $outputCsvPathComputerCreated -NoTypeInformation
    # Prepare final output
    Write-Output "Details Saved | $reportNameDailyComputerDisabled | $computerDisabledCounts | $totalcountsjson | $totalCreatedComputers | $totalUsersInDomain"
} else {
    Write-Output "No disabled computer accounts found modified in the last day. | $reportNameDailyComputerDisabled | $computerDisabledCounts | $totalcountsjson | $totalCreatedComputers | $totalUsersInDomain"
}


<# Post Block to send iworkflow code

output = outputMap.outputStream;

/*if(output.contains("Server contain unable to connect error")){
    splitoutput = output.split("\\|")
    hashMap = splitoutput[1].trim()
    totalcounts = splitoutput[2].trim()
    outputMap << [hashMap:hashMap]
    outputMap << [totalcounts:totalcounts]
    outputMap << [outputStream: output]
}*/

if(output.contains("No disabled computer")){
    splitoutput = output.split("\\|")
    hashMap = splitoutput[2].trim()
    totalcounts = splitoutput[3].trim()
    totaldisabledsum = splitoutput[4].trim()
    totalcomputerscounts = splitoutput[5].trim() 
    outputMap << [hashMap:hashMap]  // to store counts
    outputMap << [totalcounts:totalcounts] // to store total count
    outputMap << [totaldisabledsum:totaldisabledsum] // to store totalcreated computers sum
    outputMap << [totalcomputerscounts:totalcomputerscounts] // to store Totalcomputers sum
    outputMap << [outputStream: output]
}
else{
    splitoutput = output.split("\\|")
    filename = splitoutput[1].trim()
    hashMap = splitoutput[2].trim()
    totalcounts = splitoutput[3].trim()
    totaldisabledsum = splitoutput[4].trim()
    totalcomputerscounts = splitoutput[5].trim() 
    outputMap << [filename:filename] //to store reprort
    outputMap << [hashMap:hashMap]  // to store counts
    outputMap << [totalcounts:totalcounts] // to store total count
    outputMap << [totaldisabledsum:totaldisabledsum] // to store totalcreated computers sum
    outputMap << [totalcomputerscounts:totalcomputerscounts] // to store Totalcomputers sum
    outputMap << [outputStream: output]
}

#>