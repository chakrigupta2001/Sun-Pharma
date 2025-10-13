#Created Computer
 
# Define the date for checking
$dateToCheckStart = (Get-Date).AddDays(-1).Date
$dateToCheckEnd = $dateToCheckStart.AddDays(1).Date
 
# Define the output file path with current date and time
$datetime = Get-Date -Format 'dd-MMM-yyyy_HHmmss'
 
# Set the report name
$reportNameDailyComputerCreation = "Daily_ComputerCreationReport_" + $datetime + ".csv"
 
# Get list of Domains from reference table           
$domainControllers = $inputMap.domains -split ','
#$domainControllers = @("spapl.com","spil.com","ranbaxy.com","il.taro.corp","alkaloida.com","ca.taro.corp","spiinc.com", "taro.corp","pharmalucence.com")
#$domainControllers = @("ranbaxy.com")
 
# Initialize an array to store the results
$results = @()
$computercounts = [ordered]@{}
$totalCounts=@{}

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
            # Retrieve the computer accounts created in the last day
            $computers = Get-ADComputer -Filter { WhenCreated -ge $dateToCheckStart -and WhenCreated -lt $dateToCheckEnd } -Server $dc -credential $concreds1 -Properties Name, DistinguishedName, LastLogonDate, OperatingSystem, OperatingSystemVersion, WhenCreated, WhenChanged, Enabled
            $totalUsersCount = (Get-ADComputer -Filter * -Server $dc -credential $concreds1).Count
            # Mark as connected
            $connected = $true
        } catch {
            # Handle connection error
            echo "Failed to connect to $dc . Retrying..."
            Start-Sleep -Seconds 60
        } 
    }
    if (-not $connected) {
        $computercounts[$dc] = "Unable to connect"
        $totalCounts[$dc] = "Unable to connect "
        echo "Failed to connect to $dc, Retried now Skipping..."
        continue
    }
    if ($connected) {
        $totalCounts[$dc] = $totalUsersCount
        $computercounts[$dc]= 0# Initialize computer count for the domain

        # Loop through each computer and add to the results
        foreach ($computer in $computers) {
            #To store the results
            $results += [PSCustomObject]@{
                ComputerName = $computer.Name
                CreatedDate = $computer.WhenCreated
                Domain = $dc
                DistinguishedName = $computer.DistinguishedName
                OperatingSystem = $computer.OperatingSystem
                OperatingSystemVersion = $computer.OperatingSystemVersion
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
$outputCsvPathComputerCreated = $inputMap.path+$reportNameDailyComputerCreation
#$results | Export-Csv -Path "C:\\Temp\ADComputercreation.csv" -NoTypeInformation


# Preparing JSON data for errors if needed
$computercreatedcounts = $computercounts | ConvertTo-Json
$totalcountsjson = $totalCounts | ConvertTo-Json

#if ($computercounts.Values -contains "Unable to connect") {
#   Write-Output "Server contain unable to connect error | $jsondictionary | $totalcountsjson"
#}
# Check if data is empty.
if ($results.Count -gt 0) {
    # Export results to CSV
    $results | Export-Csv -Path $outputCsvPathComputerCreated -NoTypeInformation
    # Prepare final output
    Write-Output "Details Saved | $reportNameDailyComputerCreation | $computercreatedcounts | $totalcountsjson | $totalCreatedComputers | $totalUsersInDomain"
} else {
    Write-Output "No new computer accounts created today. | $reportNameDailyComputerCreation | $computercreatedcounts | $totalcountsjson | $totalCreatedComputers | $totalUsersInDomain"
}


<#
    output = outputMap.outputStream;

/*if(output.contains("Server contain unable to connect error")){
    splitoutput = output.split("\\|")
    hashMap = splitoutput[1].trim()
    totalcounts = splitoutput[2].trim()
    outputMap << [hashMap:hashMap]
    outputMap << [totalcounts:totalcounts]
    outputMap << [outputStream: output]
}*/
if(output.contains("No new computer")){
    splitoutput = output.split("\\|")
    hashMap = splitoutput[2].trim()
    totalcounts = splitoutput[3].trim()
    totalcreatedsum = splitoutput[4].trim()
    totalcomputerscounts = splitoutput[5].trim() 
    outputMap << [hashMap:hashMap]  // to store counts
    outputMap << [totalcounts:totalcounts] // to store total count
    outputMap << [totalcreatedsum:totalcreatedsum] // to store totalcreated computers sum
    outputMap << [totalcomputerscounts:totalcomputerscounts] // to store Totalcomputers sum
    outputMap << [outputStream: output]
}
else{
    splitoutput = output.split("\\|")
    filename = splitoutput[1].trim()
    hashMap = splitoutput[2].trim()
    totalcounts = splitoutput[3].trim()
    totalcreatedsum = splitoutput[4].trim()
    totalcomputerscounts = splitoutput[5].trim() 
    outputMap << [filename:filename] //to store reprort
    outputMap << [hashMap:hashMap]  // to store counts
    outputMap << [totalcounts:totalcounts] // to store total count
    outputMap << [totalcreatedsum:totalcreatedsum] // to store totalcreated computers sum
    outputMap << [totalcomputerscounts:totalcomputerscounts] // to store Totalcomputers sum
    outputMap << [outputStream: output]
}
# This script generates a report of computer accounts created in the last 24 hours across specified domains.
#>