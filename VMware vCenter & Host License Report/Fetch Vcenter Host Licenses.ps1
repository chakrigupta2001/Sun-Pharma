# ----------------------------------------
# Script Name : VMware Host Information Collector
# Purpose     : Connect to vCenter and retrieve ESXi host details (CPU, Memory, Version, Build, License, etc.)
# Author      : Chakravarthi Pulikonda 
# Date        : 08-08-2025
# Notes       : Outputs results in CSV format without headers (to support append in workflows)
# ----------------------------------------

#Load VMware PowerCLI module
Import-Module VMware.VimAutomation.Core
 
# vCenter connection details
$vCenterIP = $inputMap.ip
$vCenterName = $inputMap.vCName
$username = $inputMap.vCenterUserName
$password = $inputMap.vCenterPassword
 
# Initialize output
$results = @()
 
try {
    # Connect to vCenter
    $vcConnection = Connect-VIServer -Server $vCenterName -User $username -Password $password -WarningAction SilentlyContinue
 
    if ($vcConnection) {
        # Retrieve all ESXi hosts connected to the vCenter
        # For each host, get hardware, licensing & system info
        $results = Get-VMHost | ForEach-Object {
            $vmHost = $_
            $vmHostView = Get-View $vmHost.Id
            
            # Extract ESXi host serial number from system info
            $serial = $vmHostView.Hardware.SystemInfo.SerialNumber
 
            [PSCustomObject]@{
                "vCenter Name"   = $vCenterName
                "vcenter IP"     = $vCenterIP
                "Host"           = $vmHost.Name
                "NumCpu"         = $vmHost.NumCpu
                "CpuUsageMhz"    = $vmHost.CpuUsageMhz
                "MemoryTotalGB"  = $vmHost.MemoryTotalGB
                "MemoryUsageGB"  = $vmHost.MemoryUsageGB
                "Version"        = $vmHost.Version
                "Build"          = $vmHost.Build
                "LicenseKey"     = $vmHost.LicenseKey
                "ProcessorType"  = $vmHost.ProcessorType
                "SerialNumber"   = $serial
                "ConnectionState"= $vmHost.ConnectionState
            }
        }
 
        # Handle if no license info found (i.e., empty $results)
        if (-not $results -or $results.Count -eq 0) {
            Write-Output "$vCenterName,$vCenterIP,,,,,,,,,,,NO_LICENSE_FOUND"
        }
        else {
            # Output only CSV lines without headers
            $results | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1
        }
 
        # Disconnect
        Disconnect-VIServer -Server $vCenterName -Confirm:$false
    }

}
catch {
    # Exception handling
    Write-Output "$vCenterName,$vCenterIP,,,,,,,,,,,EXCEPTION: $_"
}



<# Load VMware PowerCLI module
Import-Module VMware.VimAutomation.Core

# vCenter connection details
$vCenterIP = $inputMap.ip
$vCenterName = $inputMap.vCName
$username = $inputMap.vCenterUserName
$password = $inputMap.vCenterPassword

# Connect to vCenter
Connect-VIServer -Server $vCenterName -User $username -Password $password

# Prepare output
$results = Get-VMHost | Select-Object `
    @{Name="vCenter Name"; Expression={$vCenterName}},
    @{Name="vcenter IP"; Expression={$vCenterIP}},
    Name,
    NumCpu,
    CpuUsageMhz,
    MemoryTotalGB,
    MemoryUsageGB,
    Version,
    Build,
    LicenseKey,
    ConnectionState,
    ProcessorType,
    @{Name="SerialNumber"; Expression={(Get-View $_.Id).Hardware.SystemInfo.SerialNumber}},

# Print CSV lines to console
$results | ConvertTo-Csv -NoTypeInformation | ForEach-Object { $_ }

# Disconnect
Disconnect-VIServer -Server $vCenterName -Confirm:$false

#>





<#

# Load VMware PowerCLI module
Import-Module VMware.VimAutomation.Core
 
# vCenter connection details
$vCenterIP = $inputMap.ip
$vCenterName = $inputMap.vCName

#$username = "ignio.support@vsphere.local"
#$password = "Welcome@123"

$username = "ignio.support@vsphere.local"
$password = "suPPort.Ignio@425#"
 
# Initialize output
$licenseReport = @()
 
try {
    # Connect to vCenter
    $vcConnection = Connect-VIServer -Server $vCenterIP -User $username -Password $password -WarningAction SilentlyContinue
 
    if ($vcConnection) {
        #$vCenterName = $vcConnection.Name
 
        # Get LicenseManager view
        $licenseManager = Get-View -Id (Get-View ServiceInstance).Content.LicenseManager
        $licenses = $licenseManager.Licenses
 
        # Get all host IPs in vCenter (optional if needed per license)
        $hostIPs = Get-VMHost | Select-Object -ExpandProperty Name
 
        if ($licenses.Count -gt 0) {
            foreach ($license in $licenses) {
                foreach ($hostIP in $hostIPs) {
                    $licenseReport += [PSCustomObject]@{
                        VCenterName     = $vCenterName
                        VCenterIP       = $vCenterIP
                        HostIP          = $hostIP
                        Scope           = "vCenter/Host"
                        Name            = $license.Name
                        EditionKey      = $license.EditionKey
                        LicenseKey      = $license.LicenseKey
                        Total           = $license.Total
                        Used            = $license.Used
                        ExpirationDate  = $license.ExpirationDate
                        CostUnit        = $license.CostUnit
                        ProductName     = $license.Properties["ProductName"]
                        ProductVersion  = $license.Properties["ProductVersion"]
                    }
                }
            }
 
            # Output CSV rows without headers
            $licenseReport | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1
        } else {
            Write-Output "$vCenterName,$vCenterIP,,NO_LICENSE_FOUND,,,,,,,,,"
        }
 
        # Disconnect from vCenter
        Disconnect-VIServer -Server $vCenterIP -Confirm:$false
    } else {
        #Write-Output "$vCenterName,$vCenterIP,,CONNECTION_FAILED,,,,,,,,,"
    }
} catch {
    Write-Output "$vCenterName,$vCenterIP,,EXCEPTION: $_,,,,,,,,,"
}

#>