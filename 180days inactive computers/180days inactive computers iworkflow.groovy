//180days inactive computers 

// Pre Block
path = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("path")).execute()[0].value.trim()
inputMap.path = path

// Fetch the domain from reference data
domain = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("domain")).execute()[0].value.trim()
inputMap.domainName = domain

// Fetch the email 'to' list from reference data
to = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("emailto")).execute()[0].value.trim()
inputMap.emailTo = to

// Fetch the email 'cc' list from reference data
cc = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("emailcc")).execute()[0].value.trim()
inputMap.emailCC = cc

// Fetch the internal email 'to' list from reference data
internalemailto = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("internalemails")).execute()[0].value.trim()
inputMap.internalemailTo = internalemailto

// Fetch the list of domains from reference data
Domains = RefDataQuery.from("UC_internal_Computer_accounts").where(RefDataCriteria.exp("key").equalTo("Domains")).execute()[0].value.trim()
inputMap.Domains = Domains

// Generate the current date in the required format
def currentdate = new Date().format("dd-MMM-YY")
inputMap.currentdate = currentdate

// Define the email subject
emailSubject = "SunPharma - AD Computers - Accounts Status Report - Disabled & Non-Reporting (180 Days)- "
inputMap.emailSubject = emailSubject + currentdate

// Define the email body for the case where no disabled computers are found
emailbody2 = """<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; font-size: 14px; color: #333; }
        .container { padding: 10px; }
        .highlight { font-weight: bold; color: #5bc0de; }
        .footer { font-size: 12px; color: #777; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <p>Dear User,</p>
        <p>After scanning the Active Directory, <span class="highlight">no records</span> were found for the following reports:</p>
        <ul>
            <li>180 Days Disabled Computers Account</li>
            <li>180 Days Enabled but Not Reporting Computers Account</li>
        </ul>
        <p>This indicates that all computer accounts are either active or have been maintained as per the defined policy. No further action is needed at this time.</p>
        <p>Please reach out if you need any clarifications.</p>
        <p>Thank you,</p>
        <p><strong>Team Ignio</strong></p>
        <p class="footer"><b>Note:</b> This is an automatically generated mail. Please do not reply.</p>
    </div>
</body>
</html>"""
inputMap.emailbody2 = emailbody2


//Main code

init {
    AttachmentPath = inputMap.path;
    domainName = inputMap.domainName;
    emailTo = inputMap.emailTo;
    emailCC = inputMap.emailCC;
    internalemailTO = inputMap.internalemailTo;
    emailSubject = inputMap.emailSubject;
    emailBody1 = inputMap.emailbody1;
    emailBody2 = inputMap.emailbody2;
    currentdate = inputMap.currentdate;
    DomainNames = inputMap.Domains;
    AttachmentNameDisabled = "";
    AttachmentNameEnabled = "";
    DomainCounts = "";
    zipContentDisabled = "";
    zipContentEnabled = "";
}
 
// Step 1: Get Disabled Computers Report
step("getDisabledReport", StepType.ONFUNCTION) {
    Map<String, Object> EntityMapP = new HashMap<>();
    EntityMapP.put("Name", processMap.domainName);
    Set<String> EntityLabelsP = new HashSet<>();
    EntityLabelsP.add("ADDomain");
    EntityInstanceP = ceb.get(EntityLabelsP, EntityMapP);
    Entity = ceb.get(EntityInstanceP.getId(), EntityLabelsP);
 
    Entity.GetDisabledComputers180days(path: processMap.AttachmentPath, Domains: processMap.DomainNames, ioutput: "getDisabledReport_output");
}
.to({ processMap.getDisabledReport_output.returnCode == 0 && processMap.getDisabledReport_output.outputStream.contains("Details Saved") }, "getEncodedFileDisabled")
.to({ processMap.getDisabledReport_output.returnCode == 0 && processMap.getDisabledReport_output.outputStream.contains("No disabled computers found.") }, "getEnabledReport")
.to({ processMap.getDisabledReport_output.returnCode == 0 && processMap.getDisabledReport_output.outputStream.contains("unable to connect error") }, "ConnectivityIssue")
.elseTo("ERROR")
 
// Step 2: Encode Disabled Report (if generated)
step("getEncodedFileDisabled", StepType.ONFUNCTION) {
    processMap.AttachmentNameDisabled = processMap.getDisabledReport_output.filename;
    subject.getEncodedFile(path: processMap.AttachmentPath, fileName: processMap.AttachmentNameDisabled, ioutput: "getEncodedFileDisabled_output");
}
.to({ processMap.getEncodedFileDisabled_output.returnCode == 0 }, "getEnabledReport")
.elseTo("ERROR")
 
// Step 3: Get Enabled Computers Report
step("getEnabledReport", StepType.ONFUNCTION) {
    Map<String, Object> EntityMapP = new HashMap<>();
    EntityMapP.put("Name", processMap.domainName);
    Set<String> EntityLabelsP = new HashSet<>();
    EntityLabelsP.add("ADDomain");
    EntityInstanceP = ceb.get(EntityLabelsP, EntityMapP);
    Entity = ceb.get(EntityInstanceP.getId(), EntityLabelsP);
 
    Entity.GetEnabledComputersNotReporting180Days(path: processMap.AttachmentPath, Domains: processMap.DomainNames, ioutput: "getEnabledReport_output");
}
.to({ processMap.getEnabledReport_output.returnCode == 0 && processMap.getEnabledReport_output.outputStream.contains("Details Saved") }, "getEncodedFileEnabled")
.to({ processMap.getEnabledReport_output.returnCode == 0 && processMap.getEnabledReport_output.outputStream.contains("No Enabled computers have failed to report for 180") }, "Get File on Ignio")
.to({ processMap.getEnabledReport_output.returnCode == 0 && processMap.getEnabledReport_output.outputStream.contains("unable to connect error") }, "ConnectivityIssue")
.elseTo("ERROR")
 
// Step 4: Encode Enabled Report (if generated)
step("getEncodedFileEnabled", StepType.ONFUNCTION) {
    processMap.AttachmentNameEnabled = processMap.getEnabledReport_output.filename;
    subject.getEncodedFile(path: processMap.AttachmentPath, fileName: processMap.AttachmentNameEnabled, ioutput: "getEncodedFileEnabled_output");
}
.to({ processMap.getEncodedFileEnabled_output.returnCode == 0 }, "Get File on Ignio")
.elseTo("ERROR")
 
// Step 5: Handle Connectivity Issues
step("ConnectivityIssue", StepType.ONIGNIO) {
    Mailbody = """Dear User,<br><br>
    Connectivity issue found in the AD Domain server while fetching the AD Computers Status Report.
    <br>Kindly, look into it and generate the report manually.
    <br><br>Thank You,<br>Team Ignio
    <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>""";
    
    iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.internalemailTo);
processMap.one = 0;
}
.to({ processMap.one == 0 }, "SUCCESS")
.elseTo("ERROR")
 
// Step 6: Process Files and Send Email
/*step("Get File on Ignio", StepType.ONIGNIO) {
    processMap.DisabledZipContent = processMap.getEncodedFileDisabled_output.outputStream;
    processMap.EnabledZipContent = processMap.getEncodedFileEnabled_output.outputStream;
 
    def disabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.DisabledZipContent);
    def enabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.EnabledZipContent);

    def disabledZipFile = new File(processMap.AttachmentNameDisabled);
    def enabledZipFile = new File(processMap.AttachmentNameEnabled);
 
    def fos1 = new java.io.FileOutputStream(disabledZipFile);
    def fos2 = new java.io.FileOutputStream(enabledZipFile);
 
    fos1.write(disabledZipFileBytes);
    fos2.write(enabledZipFileBytes);
 
    fos1.close();
    fos2.close();
    
    def disabledZipFileSize= disabledZipFile.size()
    def enabledZipFileSize = enabledZipFile.size()
 
    def jsonSlurper = new groovy.json.JsonSlurper();
    def disabledComputerCountsMap = jsonSlurper.parseText(processMap.getDisabledReport_output.hashMap);
    def totalDisabledComputerCountMap = jsonSlurper.parseText(processMap.getDisabledReport_output.totalcounts);
    def enabledComputerCountsMap = jsonSlurper.parseText(processMap.getEnabledReport_output.hashMap);
    def totalEnabledComputerCountMap = jsonSlurper.parseText(processMap.getEnabledReport_output.totalcounts);
 
    date = processMap.currentdate;
    Mailbody = """<html>
    <body>
    Dear User,<br><br>
    ignio has generated the AD Computers Status Report on """ + date + """. Kindly find the attached reports.
    <style>
    table th, td { border: 2px solid black; border-collapse: collapse; }
    th { background-color: #f2f2f2; }
    td { text-align: center; vertical-align: middle; }
    </style>
    <br><br><table style="width:50%">
    <tr>
    <th>Domains</th>
    <th>Disabled Computers</th>
    <th>Enabled Computers</th>
    </tr>""";
 
    disabledComputerCountsMap.each { domain, counts ->
        Mailbody += '<tr>\n';
        Mailbody += ' <td>' + domain + '</td>\n';
        Mailbody += ' <td>' + counts + '</td>\n';
        Mailbody += ' <td>' + enabledComputerCountsMap[domain] + '</td>\n';
        Mailbody += '</tr>\n';
    }
 
    Mailbody += """
                  <tr>
                        <th>Total</th>
                        <th>""" + totalDisabledComputerCountMap + """</th>
                        <th>""" + totalEnabledComputerCountMap + """</th>
                    </tr>
    </table>
    <br><br>Thank You,<br>Team ignio
    <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body>
    </html>"""
 
    def MAX_SIZE = 5000000L;

    if(disabledZipFileSize < MAX_SIZE & enabledZipFileSize < MAX_SIZE){
            org.springframework.web.multipart.MultipartFile[] filesForAttachment= new org.springframework.web.multipart.MultipartFile[2]
            org.springframework.web.multipart.MultipartFile[] multipartFileArray1=fileUtils.createMultiPartFileFromFile(disabledZipFile)
            filesForAttachment[0]=multipartFileArray1[0]
            org.springframework.web.multipart.MultipartFile[] multipartFileArray2=fileUtils.createMultiPartFileFromFile(enabledZipFile)
            filesForAttachment[1]=multipartFileArray2[0]
            iAction.Collaboration.SendMail(Body:Mailbody,Subject:processMap.emailSubject,To:processMap.emailTo,CC:processMap.emailCC,hasAttachment:filesForAttachment)
        }

    disabledZipFile.delete();
    enabledZipFile.delete();
    processMap.StepTwoOutput = 0;
}
.to({ processMap.StepTwoOutput == 0 }, "SUCCESS")
.to({ processMap.StepThreeOutput == 0 }, "noDisabledComputersReport")
.elseTo("ERROR")
*/

step("Get File on Ignio", StepType.ONIGNIO) {

    // Determine if reports are available
    boolean hasDisabledReport = processMap.getDisabledReport_output.outputStream.contains("Details Saved");
    boolean hasEnabledReport = processMap.getEnabledReport_output.outputStream.contains("Details Saved");
 
    if (hasDisabledReport && hasEnabledReport){
        processMap.DisabledZipContent = processMap.getEncodedFileDisabled_output.outputStream;
        processMap.EnabledZipContent = processMap.getEncodedFileEnabled_output.outputStream;
    
        def disabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.DisabledZipContent);
        def enabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.EnabledZipContent);

        def disabledZipFile = new File(processMap.AttachmentNameDisabled);
        def enabledZipFile = new File(processMap.AttachmentNameEnabled);
    
        def fos1 = new java.io.FileOutputStream(disabledZipFile);
        def fos2 = new java.io.FileOutputStream(enabledZipFile);
    
        fos1.write(disabledZipFileBytes);
        fos2.write(enabledZipFileBytes);
    
        fos1.close();
        fos2.close();
        
        def disabledZipFileSize= disabledZipFile.size()
        def enabledZipFileSize = enabledZipFile.size()
    
        def jsonSlurper = new groovy.json.JsonSlurper();
        def disabledComputerCountsMap = jsonSlurper.parseText(processMap.getDisabledReport_output.hashMap);
        def totalDisabledComputerCountMap = jsonSlurper.parseText(processMap.getDisabledReport_output.totalcounts);
        def enabledComputerCountsMap = jsonSlurper.parseText(processMap.getEnabledReport_output.hashMap);
        def totalEnabledComputerCountMap = jsonSlurper.parseText(processMap.getEnabledReport_output.totalcounts);
    
        date = processMap.currentdate;
        Mailbody = """<html>
            <head>
                <style>
                body { font-family: Arial, sans-serif; font-size: 14px; color: #333; }
                .container { padding: 10px; }
                .table-container { margin-top: 10px; }
                table { width: 50%; border-collapse: collapse; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: middle; }
                th { background-color: #f2f2f2; }
                .footer { font-size: 12px; color: #777; margin-top: 20px; }
                </style>
            </head>
        <body>
            <div class="container">
                <p>Dear User,</p>
                <p>ignio has generated the following reports on <strong>""" + date + """</strong>:</p>
                <ul>
                    <li><strong>Disabled Computers Report more than 180 Days</strong></li>
                    <li><strong>Enabled Computers but Not Reporting since 180 Days</strong></li>
                </ul>
                <p>Kindly find the attached reports.</p>
         
                <div class="table-container">
                    <table>
                        <tr>
                            <th>Domains</th>
                            <th>Disabled Computers Report more than 180 Days</th>
                            <th>Enabled Computers but Not Reporting since 180 Days</th>
                        </tr>
                        <!-- Dynamic rows will be inserted here -->
                        """; disabledComputerCountsMap.each { domain, counts -> 
                            Mailbody += '<tr>\n'; 
                            Mailbody += ' <td>' + domain + '</td>\n'; 
                            Mailbody += ' <td>' + counts + '</td>\n'; 
                            Mailbody += ' <td>' + enabledComputerCountsMap[domain] + '</td>\n'; 
                            Mailbody += '</tr>\n'; 
                        } 
                            
                        Mailbody += """
                        <tr>
                            <th>Total</th>
                            <th>""" + totalDisabledComputerCountMap + """</th>
                            <th>""" + totalEnabledComputerCountMap + """</th>
                        </tr>
                    </table>
                </div>
                <br>
                <p>Thank you,<br><strong>Team Ignio</strong></p>
         
                <p class="footer"><b>Note:</b> This is an automatically generated mail. Please do not reply.</p>
            </div>
        </body>
        </html>"""
    
        def MAX_SIZE = 5000000L;
        /*if (disabledZipFileSize < MAX_SIZE && enabledZipFileSize < MAX_SIZE) {
            org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(disabledZipFile, enabledZipFile);
            iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.emailTo, CC: processMap.emailCC, hasAttachment: filesForAttachment);
        }*/
        
        if(disabledZipFileSize < MAX_SIZE & enabledZipFileSize < MAX_SIZE){
                org.springframework.web.multipart.MultipartFile[] filesForAttachment= new org.springframework.web.multipart.MultipartFile[2]
                org.springframework.web.multipart.MultipartFile[] multipartFileArray1=fileUtils.createMultiPartFileFromFile(disabledZipFile)
                filesForAttachment[0]=multipartFileArray1[0]
                org.springframework.web.multipart.MultipartFile[] multipartFileArray2=fileUtils.createMultiPartFileFromFile(enabledZipFile)
                filesForAttachment[1]=multipartFileArray2[0]
                iAction.Collaboration.SendMail(Body:Mailbody,Subject:processMap.emailSubject,To:processMap.emailTo,CC:processMap.emailCC,hasAttachment:filesForAttachment)
            }

        disabledZipFile.delete();
        enabledZipFile.delete();
        processMap.StepTwoOutput = 0;
    }

    else if (hasDisabledReport){
        processMap.DisabledZipContent = processMap.getEncodedFileDisabled_output.outputStream;
    
        def disabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.DisabledZipContent);
        def disabledZipFile = new File(processMap.AttachmentNameDisabled);    
        def fos1 = new java.io.FileOutputStream(disabledZipFile);
        fos1.write(disabledZipFileBytes);
        fos1.close();
        def disabledZipFileSize= disabledZipFile.size()
    
        def jsonSlurper = new groovy.json.JsonSlurper();
        def disabledComputerCountsMap = jsonSlurper.parseText(processMap.getDisabledReport_output.hashMap);
        def totalDisabledComputerCountMap = jsonSlurper.parseText(processMap.getDisabledReport_output.totalcounts);
        def enabledComputerCountsMap = jsonSlurper.parseText(processMap.getEnabledReport_output.hashMap);
        def totalEnabledComputerCountMap = jsonSlurper.parseText(processMap.getEnabledReport_output.totalcounts);
    
        date = processMap.currentdate;
        Mailbody = """<html>
        <body>
        Dear User,<br><br>
        ignio has generated the AD Computers Status Report on """ + date + """. Kindly find the attached reports.
        <style>
        table th, td { border: 2px solid black; border-collapse: collapse; }
        th { background-color: #f2f2f2; }
        td { text-align: center; vertical-align: middle; }
        </style>
        <br><br><table style="width:50%">
        <tr>
        <th>Domains</th>
        <th>Disabled Computers</th>
        <th>Enabled Computers</th>
        </tr>""";
    
        disabledComputerCountsMap.each { domain, counts ->
            Mailbody += '<tr>\n';
            Mailbody += ' <td>' + domain + '</td>\n';
            Mailbody += ' <td>' + counts + '</td>\n';
            Mailbody += ' <td>' + enabledComputerCountsMap[domain] + '</td>\n';
            Mailbody += '</tr>\n';
        }
    
        Mailbody += """
                    <tr>
                            <th>Total</th>
                            <th>""" + totalDisabledComputerCountMap + """</th>
                            <th>""" + totalEnabledComputerCountMap + """</th>
                        </tr>
        </table>
        <br><br>Thank You,<br>Team ignio
        <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>
        </body>
        </html>"""
    
        def MAX_SIZE = 5000000L;
        /*if (disabledZipFileSize < MAX_SIZE && enabledZipFileSize < MAX_SIZE) {
            org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(disabledZipFile, enabledZipFile);
            iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.emailTo, CC: processMap.emailCC, hasAttachment: filesForAttachment);
        }*/
        
        if(disabledZipFileSize < MAX_SIZE & enabledZipFileSize < MAX_SIZE){
            org.springframework.web.multipart.MultipartFile[] filesForAttachment= fileUtils.createMultiPartFileFromFile(disabledZipFile)
            iAction.Collaboration.SendMail(Body:Mailbody,Subject:processMap.emailSubject,To:processMap.emailTo,CC:processMap.emailCC,hasAttachment:filesForAttachment)
        }

        disabledZipFile.delete();
        enabledZipFile.delete();
        processMap.StepThreeOutput = 0;
    }

    else if (hasEnabledReport){

        processMap.EnabledZipContent = processMap.getEncodedFileEnabled_output.outputStream;
        def enabledZipFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.EnabledZipContent);
        def enabledZipFile = new File(processMap.AttachmentNameEnabled);
        def fos2 = new java.io.FileOutputStream(enabledZipFile);
        fos2.write(enabledZipFileBytes);
        fos2.close();
        def enabledZipFileSize = enabledZipFile.size()
    
        def jsonSlurper = new groovy.json.JsonSlurper();
        def disabledComputerCountsMap = jsonSlurper.parseText(processMap.getDisabledReport_output.hashMap);
        def totalDisabledComputerCountMap = jsonSlurper.parseText(processMap.getDisabledReport_output.totalcounts);
        def enabledComputerCountsMap = jsonSlurper.parseText(processMap.getEnabledReport_output.hashMap);
        def totalEnabledComputerCountMap = jsonSlurper.parseText(processMap.getEnabledReport_output.totalcounts);
    
        date = processMap.currentdate;
        Mailbody = """<html>
        <body>
        Dear User,<br><br>
        ignio has generated the AD Computers Status Report on """ + date + """. Kindly find the attached reports.
        <style>
        table th, td { border: 2px solid black; border-collapse: collapse; }
        th { background-color: #f2f2f2; }
        td { text-align: center; vertical-align: middle; }
        </style>
        <br><br><table style="width:50%">
        <tr>
        <th>Domains</th>
        <th>Disabled Computers</th>
        <th>Enabled Computers</th>
        </tr>""";
    
        disabledComputerCountsMap.each { domain, counts ->
            Mailbody += '<tr>\n';
            Mailbody += ' <td>' + domain + '</td>\n';
            Mailbody += ' <td>' + counts + '</td>\n';
            Mailbody += ' <td>' + enabledComputerCountsMap[domain] + '</td>\n';
            Mailbody += '</tr>\n';
        }
    
        Mailbody += """
                    <tr>
                            <th>Total</th>
                            <th>""" + totalDisabledComputerCountMap + """</th>
                            <th>""" + totalEnabledComputerCountMap + """</th>
                        </tr>
        </table>
        <br><br>Thank You,<br>Team ignio
        <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>
        </body>
        </html>"""
    
        def MAX_SIZE = 5000000L;

        if(disabledZipFileSize < MAX_SIZE & enabledZipFileSize < MAX_SIZE){
            org.springframework.web.multipart.MultipartFile[] filesForAttachment= fileUtils.createMultiPartFileFromFile(enabledZipFile)
            iAction.Collaboration.SendMail(Body:Mailbody,Subject:processMap.emailSubject,To:processMap.emailTo,CC:processMap.emailCC,hasAttachment:filesForAttachment)
        }
        enabledZipFile.delete();
        processMap.StepFourOutput = 0;
    }

    else {
        // No reports available
        processMap.StepFiveOutput = 0;
    }
}
.to({ processMap.StepTwoOutput == 0 }, "SUCCESS")
.to({ processMap.StepThreeOutput == 0 }, "SUCCESS")
.to({ processMap.StepFourOutput == 0 }, "SUCCESS")
.to({ processMap.StepFiveOutput == 0 }, "noDisabledComputersReport")
.elseTo("ERROR")
 
// Step 7: Handle No Reports Case
step("noDisabledComputersReport", StepType.ONIGNIO) {
    iAction.Collaboration.SendMail(Body: processMap.emailBody2, Subject: processMap.emailSubject, To: processMap.emailTo, CC: processMap.emailCC);
    processMap.StepSixOutput = 0;
}
.to({ processMap.StepSixOutput == 0 }, "SUCCESS")
.elseTo("ERROR")
 
// End States
end("SUCCESS", Status.SUCCESS) {
    outputMap << [outputStream: "Successfully sent the mail"];
}
 
end("ERROR", Status.ERROR) {
    outputMap << [outputStream: "Unable to send the mail"];
}
