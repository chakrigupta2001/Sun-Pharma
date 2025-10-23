// Allusers report iworkflow

//Pre Block

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
def tz = TimeZone.getTimeZone("Asia/Kolkata")
def currentdate = new Date().format("dd-MMM-yy HH:mm:ss", tz)
inputMap.currentdate = currentdate

// Define the email subject
emailSubject = "SunPharma - All AD Users - Accounts Report - All Domains. - "
inputMap.emailSubject = emailSubject + currentdate

// Define the email body for the case where no disabled computers are found
emailbody2 = """
    <html>
        <body>
            Dear User,<br><br>
            ignio has generated the Active Directory All AD User Attributes - Daily Report on """ + currentdate + """</b>.
            However, no Users accounts were found in the specified domains.<br><br>
             
            Please verify the domain configurations and try again.<br><br>
             
            Thank You,<br>  
            <b>Team Ignio</b><br><br>  
            <b>Note:</b> This is an automatically generated email. Please do not reply.  
        </body>
    </html>
"""
inputMap.emailbody2 = emailbody2



/***********************************************************************************************
 * Use Case Name       : All AD User Attrbutes - Daily Report
 * Objective           : To generate a daily report of all enabled Active Directory user accounts
 *                       across multiple domains with specified attributes.
 *
 * Detailed Description:
 *      - This iWorkflow connects to multiple AD domains and queries only enabled user accounts.
 *      - Retrieves specific user attributes such as Title, Department, Country, etc.
 *      - Generates a CSV report with the selected fields and saves it to a predefined path.
 *      - Sends the report via email to the same recipients as the All Users Report.
 *      - Handles scenarios where the report exceeds email attachment size limits.
 *
 * Inputs:
 *  - Reference Data:
 *       1. UC_internal_Computer_accounts : Contains path, domain list, and email recipients.
 *  - System Date (for filename and email subject formatting).
 *
 * Outputs:
 *  - CSV File           : Enabled AD user accounts with selected attributes.
 *  - Email Notification : Sent to recipients with the report attached or size warning.
 *  - Ignio Output Log   : Summary of execution status.
 *
 * Purpose:
 *      To provide a daily snapshot of all active AD user accounts with key attributes
 *      for audit, compliance, and operational visibility.
 *
 * Author             : Chakravarthi Pulikonda
 * Version            : 1.0
 * Date Created       : 04-Aug-2025
 * Last Modified By   : Chakravarthi Pulikonda
 * Last Modified      : 04-Aug-2025
 ***********************************************************************************************/
// Main iworkflow code
init{
AttachmentPath = inputMap.path;
domainName = inputMap.domainName;
DomainNames = inputMap.Domains;
emailTo = inputMap.emailTo;
emailCC = inputMap.emailCC;
internalemailTO = inputMap.internalemailTo;
emailSubject = inputMap.emailSubject;
emailBody1 = inputMap.emailbody1;
emailBody2 = inputMap.emailbody2;
currentdate = inputMap.currentdate;
ReportName = "";
EncodedReport = "";
}

// Step 1: Get All Users Report  
step("Get Report",StepType.ONFUNCTION){
    Map<String, Object> EntityMapP = new HashMap<>();  
    EntityMapP.put("Name", processMap.domainName);  
    Set EntityLabelsP = new HashSet<>();  
    EntityLabelsP.add("ADDomain");  
    EntityInstanceP = ceb.get(EntityLabelsP, EntityMapP);  
    Entity = ceb.get(EntityInstanceP.getId(), EntityLabelsP);
	Entity.GetAllEnabledUsers('path':processMap.AttachmentPath,'Domains':processMap.DomainNames,ioutput : "getReport_output")
}.to(//Caption : Get Report_Cond_1 
{
processMap.getReport_output.returnCode == 0 && processMap.getReport_output.outputStream.contains("Details Saved")
},"Encode File")
.to(//Caption : Get Report_Cond_2 
{
processMap.getReport_output.returnCode == 0 && processMap.getReport_output.outputStream.contains("No user accounts found")
},"No Users Found")
.to(//Caption : Get Report_Cond_3 
{
processMap.getReport_output.returnCode == 0 && processMap.getReport_output.outputStream.contains("Unable to connect")
},"Connectivity Issue")
.elseTo("ERROR")

// Step 2: Encode File  
step("Encode File",StepType.ONFUNCTION){
    processMap.ReportName = processMap.getReport_output.filename;
	subject.getEncodedFile('path':processMap.AttachmentPath,'filename':processMap.ReportName,ioutput : "getEncodedFile_output")
}.to(//Caption : Encode File_Cond_1 
{
processMap.getEncodedFile_output.returnCode == 0
},"Get File on Ignio")
.elseTo("ERROR")

// Step 4: No Users Found  
step("No Users Found",StepType.ONIGNIO){
    Mailbody = processMap.emailBody2;  
    iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.emailTo);  
    processMap.StepOutput = 0;  
}.to(//Caption : No Users Found_Cond_1
{
processMap.StepOutput == 0
},"SUCCESS")
.elseTo("ERROR")

// Step 5: Connectivity Issue  
step("Connectivity Issue",StepType.ONIGNIO){
    Mailbody = """Dear User,  
    Connectivity issue found in the AD Domain server while fetching the All Users Report.  
    Kindly, look into it and generate the report manually.  
    Thank You, Team ignio  
    **Note: This is an automatically generated mail. Please do not reply.**""";  
      
    iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.internalemailTo);  
    processMap.StepOutput = 0;  
}.to(//Caption : Connectivity Issue_Cond_1
{
processMap.StepOutput == 0
},"SUCCESS")
.elseTo("ERROR")

// Step 3: Get File on Ignio (Send Email with Report)
step("Get File on Ignio",StepType.ONIGNIO){
    processMap.EncodedReport = processMap.getEncodedFile_output.outputStream;
    def reportFileBytes = org.codehaus.groovy.runtime.EncodingGroovyMethods.decodeBase64(processMap.EncodedReport);
    def reportFile = new File(processMap.ReportName);
    def fos = new java.io.FileOutputStream(reportFile);
    fos.write(reportFileBytes);
    fos.close();
 
    def jsonSlurper = new groovy.json.JsonSlurper();
    def userCountsMap = jsonSlurper.parseText(processMap.getReport_output.hashMap);
    def totalUserCount = jsonSlurper.parseText(processMap.getReport_output.totalcounts);
    def enabledCountsMap = jsonSlurper.parseText(processMap.getReport_output.enabledCounts);
    def totalEnabledCount = jsonSlurper.parseText(processMap.getReport_output.totalEnabled);
    def disabledCountsMap = jsonSlurper.parseText(processMap.getReport_output.disabledCounts);
    def totalDisabledCount = jsonSlurper.parseText(processMap.getReport_output.totalDisabled);
 
    date = processMap.currentdate;
    Mailbody = """<html>
    <body>
    Dear User,<br><br>
    ignio has generated the All Users Report on """ + date + """. Kindly find the attached report.
    <style>
    table th, td { border: 2px solid black; border-collapse: collapse; }
    th { background-color: #f2f2f2; }
    td { text-align: center; vertical-align: middle; }
    </style>
    <br><br><table style="width:60%">
    <tr>
        <th>Domains</th>
        <th>Enabled Users</th>
        <th>Disabled Users</th>
        <th>Total Users</th>
    </tr>""";
 
    userCountsMap.each { domain, counts ->
        def enabledCount = enabledCountsMap.get(domain, "0");
        def disabledCount = disabledCountsMap.get(domain, "0");
 
        Mailbody += '<tr>\n';
        Mailbody += ' <td>' + domain + '</td>\n';
        Mailbody += ' <td>' + enabledCount + '</td>\n';
        Mailbody += ' <td>' + disabledCount + '</td>\n';
        Mailbody += ' <td>' + counts + '</td>\n';
        Mailbody += '</tr>\n';
    }
 
    Mailbody += """
    <tr>
        <th>Total</th>
        <th>""" + totalEnabledCount + """</th>
        <th>""" + totalDisabledCount + """</th>
        <th>""" + totalUserCount + """</th>
    </tr>
    </table>
    <br><br>Thank You,<br>Team ignio
    <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body>
    </html>""";
 
    def MAX_SIZE = 10485760L;
    if (reportFile.size() < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(reportFile);
        iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.emailTo, CC: processMap.emailCC, hasAttachment: filesForAttachment);
    }
    else {
        // Modify the email body to remove "find attached report"
        Mailbody = """<html>
        <body>
        Dear User,<br><br>
        ignio has generated the All Users Report on """ + date + """. However, the report size exceeds 10MB and cannot be attached to this email.

        <!-- It has been saved at C:\\Temp\\ """ + processMap.ReportName + """ on the MUMIGNIOJMP01.SPIL.COM {172.16.11.211}</b>. Please retrieve it manually.<br><br> -->
        <style>
        table th, td { border: 2px solid black; border-collapse: collapse; }
        th { background-color: #f2f2f2; }
        td { text-align: center; vertical-align: middle; }
        </style>
        <table style="width:60%">
        <tr>
            <th>Domains</th>
            <th>Enabled Users</th>
            <th>Disabled Users</th>
            <th>Total Users</th>
        </tr>""";
 
        userCountsMap.each { domain, counts ->
            def enabledCount = enabledCountsMap.get(domain, "0");
            def disabledCount = disabledCountsMap.get(domain, "0");
 
            Mailbody += '<tr>\n';
            Mailbody += ' <td>' + domain + '</td>\n';
            Mailbody += ' <td>' + enabledCount + '</td>\n';
            Mailbody += ' <td>' + disabledCount + '</td>\n';
            Mailbody += ' <td>' + counts + '</td>\n';
            Mailbody += '</tr>\n';
        }
 
        Mailbody += """
        <tr>
            <th>Total</th>
            <th>""" + totalEnabledCount + """</th>
            <th>""" + totalDisabledCount + """</th>
            <th>""" + totalUserCount + """</th>
        </tr>
        </table>
        <br><br>Thank You,<br>Team ignio
        <br><br><b>Note: This is an automatically generated mail. Please do not reply.</b>
        </body></html>""";
 
        // Send the email without an attachment
        iAction.Collaboration.SendMail(Body: Mailbody, Subject: processMap.emailSubject, To: processMap.emailTo, CC: processMap.emailCC);
    }
 
    reportFile.delete();
    processMap.StepOutput = 0;
}.to(//Caption : Get File on Ignio_Cond_1
{
processMap.StepOutput == 0
},"SUCCESS")
.elseTo("ERROR")

end("ERROR",Status.ERROR){
outputMap<<[outputStream : "Unable to send the mail"];
}

// End States  
end("SUCCESS",Status.SUCCESS){
outputMap<<[outputStream : "Successfully sent the mail"];
}
