// =======================================
// Pre Block : Initialization & Input Data
// =======================================

// Set current date for file naming and email subject
def tz = TimeZone.getTimeZone("Asia/Kolkata")
def currentdate = new Date().format("dd-MMM-yy HH:mm:ss", tz)
inputMap.currentdate = currentdate
 
// Fetch email subject from reference table and append date
def subject = RefDataQuery.from("Linux_Mail_Recipients")
                .where(RefDataCriteria.exp("key").equalTo("subject"))
                .execute()[0].values.trim()
inputMap.Emailsubject = "Linux Root Login details - " + currentdate
 
// Fetch "To" recipients list from reference table
def to = RefDataQuery.from("Linux_Mail_Recipients")
             .where(RefDataCriteria.exp("key").equalTo("emailto"))
             .execute()[0].values.trim()
inputMap.emailTo = to
 
// Fetch "CC" recipients list from reference table
def cc = RefDataQuery.from("Linux_Mail_Recipients")
             .where(RefDataCriteria.exp("key").equalTo("emailcc"))
             .execute()[0].values.trim()
inputMap.emailCC = cc
 
/*// Add flavour-specific IPs
inputMap.RHEL5_IPList = []
inputMap.RHEL7_IPList = ["172.16.15.109"]
inputMap.RHEL8_IPList = ["172.30.32.186", "172.30.32.187"]
inputMap.RHEL9_IPList = ["172.16.9.63", "172.16.11.165", "172.16.11.162"]*/

// Get all IPs from reference table
def rows = RefDataQuery.from("Linux_IPs_List").execute()
 
inputMap.RHEL5_IPList = rows.findAll { it["OSFlavor"] == "RHEL5" }*.IPAddress
inputMap.Linux_IPs_List =rows.findAll { it["OSFlavor"] == "Linux" }*.IPAddress
inputMap.RHEL7_IPList = rows.findAll { it["OSFlavor"] == "RHEL7" }*.IPAddress
inputMap.RHEL8_IPList = rows.findAll { it["OSFlavor"] == "RHEL8" }*.IPAddress
inputMap.RHEL9_IPList = rows.findAll { it["OSFlavor"] == "RHEL9" }*.IPAddress
inputMap.SLES15_IPList = rows.findAll { it["OSFlavor"] == "SLES15" }*.IPAddress
inputMap.SLES12_IPList = rows.findAll { it["OSFlavor"] == "SLES12" }*.IPAddress
inputMap.SLES11_IPList = rows.findAll { it["OSFlavor"] == "SLES11" }*.IPAddress
inputMap.Ubuntu22_IPList = rows.findAll { it["OSFlavor"] == "Ubuntu22" }*.IPAddress
inputMap.Ubuntu20_IPList = rows.findAll { it["OSFlavor"] == "Ubuntu20" }*.IPAddress
inputMap.CentOS8_IPList = rows.findAll { it["OSFlavor"] == "CentOS8" }*.IPAddress
inputMap.CentOS7_IPList = rows.findAll { it["OSFlavor"] == "CentOS7" }*.IPAddress
inputMap.CentOS6_IPList = rows.findAll { it["OSFlavor"] == "CentOS6" }*.IPAddress


/*def currentdate = new Date().format("dd-MMM-YY")
inputMap.currentdate = currentdate
 
emailsubject = RefDataQuery.from("Root_Account_Logins").where(RefDataCriteria.exp("key").equalTo("subject")).execute()[0].values.trim()
inputMap.Emailsubject = emailsubject + " " + currentdate
 
to = RefDataQuery.from("Root_Account_Logins").where(RefDataCriteria.exp("key").equalTo("emailto")).execute()[0].values.trim()
inputMap.emailTo = to
 
cc = RefDataQuery.from("Root_Account_Logins").where(RefDataCriteria.exp("key").equalTo("emailcc")).execute()[0].values.trim()
inputMap.emailCC = cc*/

/*/ preblock iworkflow code
def currentdate = new Date().format("dd-MMM-YY")
inputMap.currentdate = currentdate
 
emailsubject = RefDataQuery.from("Root_Account_Logins")
    .where(RefDataCriteria.exp("key").equalTo("subject")).execute()[0].values.trim()
inputMap.Emailsubject = emailsubject + " " + currentdate
 
to = RefDataQuery.from("Root_Account_Logins")
    .where(RefDataCriteria.exp("key").equalTo("emailto")).execute()[0].values.trim()
inputMap.emailTo = to
 
cc = RefDataQuery.from("Root_Account_Logins")
    .where(RefDataCriteria.exp("key").equalTo("emailcc")).execute()[0].values.trim()
inputMap.emailCC = cc*/


/**********************************************************************************************
* Use Case Name   : Linux Root Login Details Report
* Objective       : To collect and report root login session details from multiple Linux servers
*                   across various OS flavours, generate a CSV and PDF report, and email it
*                   to a defined recipient list.
*
* Detailed Description :
* - This iWorkflow retrieves IP addresses for different Linux OS flavours from a reference table.
* - Executes an atomic function (`FetchRootLoginDetails`) on each host to get previous month's
*   root login activity.
* - Aggregates the results into a CSV report.
* - Converts the CSV into a formatted PDF table with word wrapping for better readability.
* - Sends the PDF report via email to configured recipients.
* - Handles large file scenarios by sending a notification email without the attachment if the
*   PDF exceeds size limits.
*
* Inputs :
* - Reference Data :
*     Linux_Mail_Recipients (subject, emailto, emailcc)
*     Linux_IPs_List (OSFlavor, IPAddress)
* - Process Map Variables : OS-specific IP lists, current date, counters for success/failure.
*
* Outputs :
* - Email with PDF attachment (Root Login Report).
* - Log entries for processing status, success, and failure counts.
*
* Author          : Chakravarthy Pulikonda
* Created Date    : August 2025
* Last Modified   : August 2025
**********************************************************************************************/

// init
// Purpose: Initialize process variables, counters, flavour list, and an empty report string.
//          Converts input IP lists into process variables for iterative processing.
init {
// Load metadata from inputMap
    currentdate   = inputMap.currentdate       // Current date for report timestamp
    emailCC       = inputMap.emailCC           // Email CC recipients
    emailTo       = inputMap.emailTo           // Email TO recipients
    emailsubject  = inputMap.Emailsubject      // Email subject line

    
    // Define list of OS flavors to be processed

    flavours = ["RHEL5", "Linux", "RHEL7", "RHEL8", "RHEL9", "CentOS8" , "CentOS7" , "CentOS6" , "SLES15", "SLES12", "SLES11", "Ubuntu22", "Ubuntu20"]
    
    flavourIndex = 0        // Index to track current OS flavor
    index_ip     = 0       // Index to track current IP within the flavor list

    // Load IP lists for each OS flavor from Pre-Processing Block 
    rhel5List = inputMap.RHEL5_IPList
    LinuxList = inputMap.Linux_IPs_List
    rhel7List = inputMap.RHEL7_IPList
    rhel8List = inputMap.RHEL8_IPList
    rhel9List = inputMap.RHEL9_IPList
    SLES15List = inputMap.SLES15_IPList
    SLES12List = inputMap.SLES12_IPList
    SLES11List = inputMap.SLES11_IPList
    Ubuntu22_IPList = inputMap.Ubuntu22_IPList
    Ubuntu20_IPList = inputMap.Ubuntu20_IPList
    CentOS8_IPList = inputMap.CentOS8_IPList
    CentOS7_IPList = inputMap.CentOS7_IPList
    CentOS6_IPList = inputMap.CentOS6_IPList
    
    // Initialize working variables
    IPList         = []       // Current list of IPs being processed
    currentFlavour = ""       // Current OS flavor being processed
    
    // Initialize counters for tracking progress
    totalIps    = 0       // Total number of IPs processed
    successIPs  = 0       // Number of successful executions
    failedIPs   = 0       // Number of failed executions

    // Initialize report header (CSV format)
    report = "Hostname,User,Host,Login Time,Logout Time,Duration\n"
}
 
// Step: SetNextFlavour
// Purpose: Pick the next OS flavour from the flavours list and load its associated IP list
//          into processMap.IPList. Also logs which flavour is being processed. 
// Set next flavour and corresponding IP list
step("SetNextFlavour", StepType.ONIGNIO) {
    def flavour = processMap.flavours[processMap.flavourIndex]
    processMap.currentFlavour = flavour
 
    switch(flavour) {
        case "RHEL5":
            processMap.IPList = processMap.rhel5List
            LOG.error("Processing Linux Flavour : RHEL5")
            break
        case "Linux":
            processMap.IPList = processMap.LinuxList
            LOG.error("Processing Linux Flavour : Linux")
            break
        case "RHEL7":
            processMap.IPList = processMap.rhel7List
            LOG.error("Processing Linux Flavour : RHEL7")
            break
        case "RHEL8":
            processMap.IPList = processMap.rhel8List
            LOG.error("Processing Linux Flavour : RHEL8")
            break
        case "RHEL9":
            processMap.IPList = processMap.rhel9List
            LOG.error("Processing Linux Flavour : RHEL9")
            break
        case "SLES15":
            processMap.IPList = processMap.SLES15_IPList
            LOG.error("Processing Linux Flavour : SLES15")
            break
        case "SLES12":
            processMap.IPList = processMap.SLES12_IPList
            LOG.error("Processing Linux Flavour : SLES12")
            break
        case "SLES11":
            processMap.IPList = processMap.SLES11_IPList
            LOG.error("Processing Linux Flavour : SLES11")
            break
        case "Ubuntu22":
            processMap.IPList = processMap.Ubuntu22_IPList
            LOG.error("Processing Linux Flavour : SLES15")
            break
        case "Ubuntu20":
            processMap.IPList = processMap.Ubuntu20_IPList
            LOG.error("Processing Linux Flavour : SLES15")
            break
        case "CentOS8":
            processMap.IPList = processMap.CentOS8_IPList
            LOG.error("Processing Linux Flavour : CentOS8")
            break
        case "CentOS7":
            processMap.IPList = processMap.CentOS7_IPList
            LOG.error("Processing Linux Flavour : CentOS7")
            break
        case "CentOS6":
            processMap.IPList = processMap.CentOS6_IPList
            LOG.error("Processing Linux Flavour : CentOS6")
            break
    }
 
    processMap.index_ip = 0
}
.to({true}, "CheckNextIP")
 
// Step: CheckNextIP
// Purpose: Decide the next action based on whether there are more IPs in the current flavour
//          or more flavours left to process. Routes to the appropriate next step.
// Control logic to continue or finish
step("CheckNextIP", StepType.ONIGNIO) {
    def ipCount = processMap.IPList.size()
    def flavourCount = processMap.flavours.size()
 
    if (processMap.index_ip < ipCount) {
        processMap.nextStep = "getRootLoginDetails"
    } else if (processMap.flavourIndex + 1 < flavourCount) {
        processMap.flavourIndex++
        processMap.nextStep = "SetNextFlavour"
    } else {
        processMap.nextStep = "SendEmailReport"
    }
}
.to({ processMap.nextStep == "getRootLoginDetails" }, "getRootLoginDetails")
.to({ processMap.nextStep == "SetNextFlavour" }, "SetNextFlavour")
.to({ processMap.nextStep == "SendEmailReport" }, "SendEmailReport")
 
// Step: getRootLoginDetails
// Purpose: Call the atomic function 'FetchRootLoginDetails' for the current IP
//          to fetch root login data from the target Linux server.
// Fetch data from atomic function
step("getRootLoginDetails", StepType.ONFUNCTION) {
    def ip = processMap.IPList[processMap.index_ip]
 
    Map<String, Object> entityMap = new HashMap<>()
    entityMap.put("IPAddress", ip)
 
    Set<String> entityLabels = new HashSet<>()
    entityLabels.add(processMap.currentFlavour)
 
    def entityInstance = ceb.get(entityLabels, entityMap)
    def entity = ceb.get(entityInstance.getId(), entityLabels)
 
    entity.FetchRootLoginDetails(ioutput: "output_report")
}
.to({true}, "NextIP")
 
// Step: NextIP
// Purpose: Append the fetched root login data to the report if successful,
//          or log the error and add a placeholder row for failed IPs.
//          Updates counters and moves to the next IP.
// Append output and continue loop
step("NextIP", StepType.ONIGNIO) {
    def ip = (processMap.index_ip < processMap.IPList.size()) ? processMap.IPList[processMap.index_ip] : "Unknown IP"
 
    if (processMap.output_report?.outputStream) {
    processMap.successIPs += 1
    processMap.report += processMap.output_report.outputStream
    } else {
    def errStream = processMap.output_report?.errorStream ?: "Unknown Error"
        errStream = errStream.replaceAll("[\\r\\n]+", " ").replace(",", " ")
        processMap.report += "Unknown,Unknown," + ip + "," + errStream + ",Error,Error\n"
    processMap.failedIPs += 1
    //processMap.report += "Unknown," + ip + ",Error,Error,Error,Error\n"
    }
 
    LOG.error("Processed IP: " + ip)
    processMap.index_ip++
    processMap.totalIps += 1
}
.to({true}, "CheckNextIP")

// Final email step
/*step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def csvReportName = "RootLoginReport_" + date + ".csv"
    def pdfReportName = "RootLoginReport_" + date + ".pdf"
 
    // Write CSV to disk
    def csvFile = new File(csvReportName)
    csvFile.write(processMap.report)
    //csvFile.setReadOnly()
 
    // Convert CSV to PDF
    def textContent = csvFile.text.replace("\r", "")
    def pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument()
    def font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    float fontSize = 12f
    float leading = fontSize * 1.5f
    float margin = 50f
    float pageHeight = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() as float
    float cursorY = pageHeight - margin
 
    def page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
    pdfDocument.addPage(page)
    def contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
    contentStream.setFont(font, fontSize)
    contentStream.beginText()
    contentStream.newLineAtOffset(margin as float, cursorY as float) // Explicit casting
 
    textContent.split("\n").each { line ->
        if (cursorY < margin + fontSize) {
            contentStream.endText()
            contentStream.close()
            page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
            pdfDocument.addPage(page)
            contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
            contentStream.setFont(font, fontSize)
            contentStream.beginText()
            cursorY = pageHeight - margin
            contentStream.newLineAtOffset(margin as float, cursorY as float) // Cast here
        }
        contentStream.showText(line)
        contentStream.newLineAtOffset(0 as float, -leading as float) // Cast offsets
        cursorY -= leading
    }
 
    contentStream.endText()
    contentStream.close()
    pdfDocument.save(pdfReportName)
    pdfDocument.close()
 
    // Send the email with PDF attachment
    def emailBody = """<html><body>
        Dear Team,<br><br>
        Please find attached the Root Login Report as of <b>"""+date+"""</b>.<br><br>
        Thank You,<br>
        Team ignio<br><br>
        <b>Note: This is an automatically generated mail. Please do not reply.</b>
        </body></html>"""
 
    def pdfFile = new File(pdfReportName)
    def MAX_SIZE = 10000000L
    def fileSize = pdfFile.size()
 
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(pdfFile)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with PDF attachment.")
        processMap.status = 0
    } else {
        LOG.error("PDF attachment too large to send: " + fileSize)
        processMap.status = 1
    }
 
    // Clean up files
    csvFile.delete()
    pdfFile.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")*/

// Pdf with actual raw data (CSV)
/*step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def csvReportName = "RootLoginReport_" + date + ".csv"
    def pdfReportName = "RootLoginReport_" + date + ".pdf"
 
    // Write CSV to disk
    def csvFile = new File(csvReportName)
    csvFile.write(processMap.report)
 
    // Convert CSV to PDF with line wrapping
    def textContent = csvFile.text.replace("\r", "")
    def pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument()
    def font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    float fontSize = 12f
    float leading = fontSize * 1.5f
    float margin = 50f
    float pageHeight = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() as float
    float pageWidth = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth() as float
    float cursorY = pageHeight - margin
 
    def page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
    pdfDocument.addPage(page)
    def contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
    contentStream.setFont(font, fontSize)
    contentStream.beginText()
    contentStream.newLineAtOffset(margin, cursorY)
 
    float maxWidth = pageWidth - 2 * margin
 
    textContent.split("\n").each { line ->
        def words = line.split(' ')
        def currentLine = ""
        words.each { word ->
            def testLine = currentLine ? currentLine + " " + word : word
            def textWidth = font.getStringWidth(testLine) / 1000 * fontSize
            if (textWidth > maxWidth) {
                // Write currentLine to PDF
                if (cursorY < margin + fontSize) {
                    contentStream.endText()
                    contentStream.close()
                    page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
                    pdfDocument.addPage(page)
                    contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
                    contentStream.setFont(font, fontSize)
                    contentStream.beginText()
                    cursorY = pageHeight - margin
                    contentStream.newLineAtOffset(margin, cursorY)
                }
                contentStream.showText(currentLine)
                contentStream.newLineAtOffset(0f, -leading)
                cursorY -= leading
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        // Write the last line of the paragraph
        if (currentLine) {
            if (cursorY < margin + fontSize) {
                contentStream.endText()
                contentStream.close()
                page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
                pdfDocument.addPage(page)
                contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
                contentStream.setFont(font, fontSize)
                contentStream.beginText()
                cursorY = pageHeight - margin
                contentStream.newLineAtOffset(margin, cursorY)
            }
            contentStream.showText(currentLine)
            contentStream.newLineAtOffset(0f, -leading)
            cursorY -= leading
        }
    }
 
    contentStream.endText()
    contentStream.close()
    pdfDocument.save(pdfReportName)
    pdfDocument.close()
 
    // Send the email with PDF attachment
    def emailBody = """
    <html><body>
    Dear Team,<br><br>
    Please find attached the Root Login Report as of <b>"""+date+"""</b>.<br><br>
    Thank You,<br> Team ignio<br><br>
    <b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body></html>
    """
 
    def pdfFile = new File(pdfReportName)
    def MAX_SIZE = 10000000L
    def fileSize = pdfFile.size()
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(pdfFile)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with PDF attachment.")
        processMap.status = 0
    } else {
        LOG.error("PDF attachment too large to send: " + fileSize)
        processMap.status = 1
    }
 
    // Clean up files
    csvFile.delete()
    pdfFile.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")*/

// PDF With Table
/*step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def csvReportName = "RootLoginReport_" + date + ".csv"
    def pdfReportName = "RootLoginReport_" + date + ".pdf"
 
    // Write CSV to disk
    def csvFile = new File(csvReportName)
    csvFile.write(processMap.report)
 
    // Convert CSV to PDF with table format
    def textContent = csvFile.text.replace("\r", "")
    def pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument()
    def font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    def boldFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    float fontSize = 8f
    float leading = fontSize * 1.5f
    float margin = 40f
    float pageHeight = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() as float
    float pageWidth = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth() as float
    float cursorY = pageHeight - margin as float
 
    // Calculate column widths
    def rows = textContent.split("\n")
    def headers = rows[0].split(",", -1)
    int columnCount = headers.size()
    
    // Define relative column widths (adjust these percentages as needed)
    def colPercentages = [23f, 13f, 15f, 20f, 20f, 9f] // Must sum to 100
    float tableWidth = pageWidth - 2 * margin
    float[] colWidths = colPercentages.collect { (it/100f) * tableWidth } as float[]
 
    // Create first page
    def page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
    pdfDocument.addPage(page)
    def contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
    contentStream.setFont(font, fontSize)
    contentStream.setStrokingColor(0, 0, 0) // Black border color
 
    float rowHeight = leading + 4f
    float startX = margin as float
 
    // Function to draw a table row
    def drawRow = { String[] cells, boolean isHeader ->
        float x = startX
        float y = cursorY - rowHeight
        
        // Draw cell borders and content
        for (int i = 0; i < columnCount; i++) {
            // Draw cell border
            contentStream.addRect(x as float, y as float, colWidths[i] as float, rowHeight as float)
            contentStream.stroke()
            
            // Draw cell content
            String cellText = (i < cells.length) ? cells[i].trim() : ""
            contentStream.beginText()
            if (isHeader) {
                contentStream.setFont(boldFont, fontSize)
            }
            // Center text vertically and add left padding
            contentStream.newLineAtOffset((x + 2f) as float, (y + (rowHeight - fontSize)/2) as float)
            contentStream.showText(cellText)
            contentStream.endText()
            
            x += colWidths[i]
        }
        cursorY -= rowHeight
    }
 
    // Draw header row
    drawRow(headers, true)
    
    // Draw data rows
    for (int i = 1; i < rows.size(); i++) {
        if (cursorY < margin + rowHeight) {
            // New page needed
            contentStream.close()
            page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
            pdfDocument.addPage(page)
            contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
            contentStream.setFont(font, fontSize)
            contentStream.setStrokingColor(0, 0, 0)
            cursorY = pageHeight - margin as float
            
            // Redraw header on new page
            drawRow(headers, true)
        }
        
        def cells = rows[i].split(",", -1)
        drawRow(cells, false)
    }
 
    contentStream.close()
    pdfDocument.save(pdfReportName)
    pdfDocument.close()
 
    // Send the email with PDF attachment
    def emailBody = """
    <html><body>
    Dear Team,<br><br>
    Please find attached the Root Login Report as of <b>"""+date+"""</b>.<br><br>
    Thank You,<br> Team ignio<br><br>
    <b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body></html>
    """
 
    def pdfFile = new File(pdfReportName)
    def MAX_SIZE = 10000000L
    def fileSize = pdfFile.size()
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(pdfFile)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with PDF attachment.")
        processMap.status = 0
    } else {
        LOG.error("PDF attachment too large to send: " + fileSize)
        processMap.status = 1
    }
 
    // Clean up files
    csvFile.delete()
    pdfFile.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")*/

/*/ CSV to PDF with word wrap table.
step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def csvReportName = "RootLoginReport_" + date + ".csv"
    def pdfReportName = "RootLoginReport_" + date + ".pdf"
 
    // Write CSV to disk
    def csvFile = new File(csvReportName)
    csvFile.write(processMap.report)
 
    // Convert CSV to PDF with table format and word wrapping
    def textContent = csvFile.text.replace("\r", "")
    def pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument()
    def font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    def boldFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    float fontSize = 8f
    float leading = fontSize * 1.5f
    float margin = 40f
    float pageHeight = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() as float
    float pageWidth = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth() as float
    float cursorY = pageHeight - margin
 
    def rows = textContent.split("\n")
    def headers = rows[0].split(",", -1)
    int columnCount = headers.size()
 
    // Define column width percentages (must total 100)
    def colPercentages = [23f, 13f, 15f, 20f, 20f, 9f]
    float tableWidth = pageWidth - 2 * margin
    float[] colWidths = colPercentages.collect { (it / 100f) * tableWidth } as float[]
 
    // Create first page
    def page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
    pdfDocument.addPage(page)
    def contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
    contentStream.setStrokingColor(0, 0, 0)
 
    float startX = margin
 
    // Word wrapping row drawer
    def drawRow = { String[] cells, boolean isHeader ->
        float x = startX
        float maxRowHeight = leading + 4f
        List<List<String>> wrappedLinesList = []
 
        for (int i = 0; i < columnCount; i++) {
            String text = (i < cells.length) ? cells[i].trim() : ""
            def lines = []
            def currentLine = ""
            def words = text.split(" ")
 
            for (word in words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word
                float textWidth = (isHeader ? boldFont : font).getStringWidth(testLine) / 1000 * fontSize
                if (textWidth > colWidths[i]) {
                    lines << currentLine
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine) lines << currentLine
            wrappedLinesList << lines
            float heightForCell = lines.size() * leading + 4f
            maxRowHeight = Math.max(maxRowHeight, heightForCell)
        }
 
        float y = cursorY - maxRowHeight
 
        // Draw cell borders and text
        for (int i = 0; i < columnCount; i++) {
            float cellX = x
            float cellY = y
            float cellWidth = colWidths[i]
 
            contentStream.addRect(cellX, cellY, cellWidth, maxRowHeight)
            contentStream.stroke()
 
            contentStream.beginText()
            contentStream.setFont(isHeader ? boldFont : font, fontSize)
            contentStream.newLineAtOffset((float)(cellX + 2f), (float)(cellY + maxRowHeight - leading))
            for (line in wrappedLinesList[i]) {
                contentStream.showText(line)
                contentStream.newLineAtOffset(0f, -leading)
            }
            contentStream.endText()
 
            x += colWidths[i]
        }
 
        cursorY -= maxRowHeight
    }
 
    // Draw header
    drawRow(headers, true)
 
    // Draw rows with page breaks
    for (int i = 1; i < rows.size(); i++) {
        def cells = rows[i].split(",", -1)
 
        // Estimate wrapped row height before drawing
        float estimatedHeight = leading * 2 // default
        for (int j = 0; j < columnCount; j++) {
            String text = (j < cells.length) ? cells[j].trim() : ""
            def words = text.split(" ")
            def line = ""
            def lines = 1
            for (word in words) {
                String testLine = line.isEmpty() ? word : line + " " + word
                float width = font.getStringWidth(testLine) / 1000 * fontSize
                if (width > colWidths[j]) {
                    line = word
                    lines++
                } else {
                    line = testLine
                }
            }
            estimatedHeight = Math.max(estimatedHeight, lines * leading + 4f)
        }
 
        if (cursorY < margin + estimatedHeight) {
            contentStream.close()
            page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
            pdfDocument.addPage(page)
            contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
            contentStream.setStrokingColor(0, 0, 0)
            cursorY = pageHeight - margin
            drawRow(headers, true)
        }
 
        drawRow(cells, false)
    }
 
    contentStream.close()
    pdfDocument.save(pdfReportName)
    pdfDocument.close()
 
    // Email body
    def emailBody = """
    <html><body>
    Dear Team,<br><br>
    Please find attached the Root Login Report as of <b>"""+date+"""</b>.<br><br>
    Thank You,<br>Team ignio<br><br>
    <b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body></html>
    """
 
    def pdfFile = new File(pdfReportName)
    def MAX_SIZE = 10000000L
    def fileSize = pdfFile.size()
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(pdfFile)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with PDF attachment.")
        processMap.status = 0
    } else {
        LOG.error("PDF attachment too large to send: " + fileSize)
        processMap.status = 1
    }
 
    csvFile.delete()
    pdfFile.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")*/

// Step: SendEmailReport
// Purpose: Convert the accumulated CSV report to PDF with word-wrapped table formatting,
//          send it via email, and handle cases where the PDF size exceeds limits.
// CSV to PDF with word wrap table.
step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def csvReportName = "RootLoginReport_" + date + ".csv"
    def pdfReportName = "RootLoginReport_" + date + ".pdf"
 
    // Write CSV to disk
    def csvFile = new File(csvReportName)
    csvFile.write(processMap.report)
 
    // Convert CSV to PDF with table format and word wrapping
    def textContent = csvFile.text.replace("\r", "")
    def pdfDocument = new org.apache.pdfbox.pdmodel.PDDocument()
    def font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
    def boldFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    float fontSize = 8f
    float leading = fontSize * 1.5f
    float margin = 40f
    float pageHeight = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getHeight() as float
    float pageWidth = org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER.getWidth() as float
    float cursorY = pageHeight - margin
 
    def rows = textContent.split("\n")
    def headers = rows[0].split(",", -1)
    int columnCount = headers.size()
 
    // Define column width percentages (must total 100)
    def colPercentages = [23f, 13f, 15f, 20f, 20f, 9f]
    float tableWidth = pageWidth - 2 * margin
    float[] colWidths = colPercentages.collect { (it / 100f) * tableWidth } as float[]
 
    // Create first page
    def page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
    pdfDocument.addPage(page)
    def contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)

    // === Add Report Title ===
    String reportTitle = "Root User Login Details Report"
    def titleFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
    float titleFontSize = 14f
    float titleWidth = titleFont.getStringWidth(reportTitle) / 1000 * titleFontSize
    float titleX = (pageWidth - titleWidth) / 2
    float titleY = cursorY // use current Y
    
    contentStream.beginText()
    contentStream.setFont(titleFont, titleFontSize)
    contentStream.newLineAtOffset(titleX, titleY)
    contentStream.showText(reportTitle)
    contentStream.endText()
    
    cursorY -= leading * 2  // leave space after title
    contentStream.setStrokingColor(0, 0, 0)
 
    float startX = margin
 
    // Word wrapping row drawer
    def drawRow = { String[] cells, boolean isHeader ->
        float x = startX
        float maxRowHeight = leading + 4f
        List<List<String>> wrappedLinesList = []
 
        for (int i = 0; i < columnCount; i++) {
            String text = (i < cells.length) ? cells[i].trim() : ""
            def lines = []
            def currentLine = ""
            def words = text.split(" ")
 
            for (word in words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word
                float textWidth = (isHeader ? boldFont : font).getStringWidth(testLine) / 1000 * fontSize
                if (textWidth > colWidths[i]) {
                    lines << currentLine
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine) lines << currentLine
            wrappedLinesList << lines
            float heightForCell = lines.size() * leading + 4f
            maxRowHeight = Math.max(maxRowHeight, heightForCell)
        }
 
        float y = cursorY - maxRowHeight
 
        // Draw cell borders and text
        for (int i = 0; i < columnCount; i++) {
            float cellX = x
            float cellY = y
            float cellWidth = colWidths[i]
 
            contentStream.addRect(cellX, cellY, cellWidth, maxRowHeight)
            contentStream.stroke()
 
            contentStream.beginText()
            contentStream.setFont(isHeader ? boldFont : font, fontSize)
            contentStream.newLineAtOffset((float)(cellX + 2f), (float)(cellY + maxRowHeight - leading))
            for (line in wrappedLinesList[i]) {
                contentStream.showText(line)
                contentStream.newLineAtOffset(0f, -leading)
            }
            contentStream.endText()
 
            x += colWidths[i]
        }
 
        cursorY -= maxRowHeight
    }
 
    // Draw header
    drawRow(headers, true)
 
    // Draw rows with page breaks
    for (int i = 1; i < rows.size(); i++) {
        def cells = rows[i].split(",", -1)
 
        // Estimate wrapped row height before drawing
        float estimatedHeight = leading * 2 // default
        for (int j = 0; j < columnCount; j++) {
            String text = (j < cells.length) ? cells[j].trim() : ""
            def words = text.split(" ")
            def line = ""
            def lines = 1
            for (word in words) {
                String testLine = line.isEmpty() ? word : line + " " + word
                float width = font.getStringWidth(testLine) / 1000 * fontSize
                if (width > colWidths[j]) {
                    line = word
                    lines++
                } else {
                    line = testLine
                }
            }
            estimatedHeight = Math.max(estimatedHeight, lines * leading + 4f)
        }
 
        if (cursorY < margin + estimatedHeight) {
            contentStream.close()
            page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER)
            pdfDocument.addPage(page)
            contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page)
            contentStream.setStrokingColor(0, 0, 0)
            cursorY = pageHeight - margin
            drawRow(headers, true)
        }
 
        drawRow(cells, false)
    }
 
    contentStream.close()
    pdfDocument.save(pdfReportName)
    pdfDocument.close()
 
    // Email body
    def emailBody = """
    <html><body>
        Dear Team,<br><br>
        Please find attached the Root Login Report as of <b>"""+date+"""</b>.<br><br>
        
        <h3>Root Login Uers Summary</h3>
        <table cellpadding="6" cellspacing="0" border="0" style="background-color: #f9f9f9; border: 1px solid #dddddd; border-radius: 6px;">
                  <tr>
                    <td><b>Success Count:</b></td>
                    <td>""" + processMap.successIPs + """</td>
                  </tr>
                  <tr>
                    <td><b>Failure Count:</b></td>
                    <td>""" + processMap.failedIPs + """</td>
                  </tr>
                  <tr>
                    <td><b>Total Count:</b></td>
                    <td>""" + processMap.totalIps + """</td>
                  </tr>
         </table><br>
        
        Thank You,<br>Team ignio<br><br>
        <hr style="border: none; border-top: 1px solid #cccccc; margin: 20px 0;">
            <i style="color: #888888;">Note: This is an automatically generated mail. Please do not reply.</i>
    </body>
    </html>
    """
 
    def pdfFile = new File(pdfReportName)
    def MAX_SIZE = 9990000L
    def fileSize = pdfFile.size()
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(pdfFile)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with PDF attachment.")
        processMap.status = 0
    } else {
        LOG.error("PDF attachment too large to send: " + fileSize)
        mailBody = """
        <html>
          <body style="font-family: Arial, sans-serif; font-size: 14px; color: #333333;">
            <p>
              Dear Team,
              <br><br>
              This is to inform you that the <b>Root User Login Report</b> could not be attached as the file size exceeded the permissible email limit (10MB).
              <br><br>
              Kindly reach out to the <b>Linux Support Team</b>.
              <br><br>
            
            <h3>Root Login Uers Summary</h3>
            <table cellpadding="6" cellspacing="0" border="0" style="background-color: #f9f9f9; border: 1px solid #dddddd; border-radius: 6px;">
                      <tr>
                        <td><b>Success Count:</b></td>
                        <td>""" + processMap.successIPs + """</td>
                      </tr>
                      <tr>
                        <td><b>Failure Count:</b></td>
                        <td>""" + processMap.failedIPs + """</td>
                      </tr>
                      <tr>
                        <td><b>Total Count:</b></td>
                        <td>""" + processMap.totalIps + """</td>
                      </tr>
             </table><br>
             
              <b>Report Date:</b> """ + date + """
              <br><br>
              Thank you,<br>
              <b>Team ignio</b>
            </p>
            <hr style="border: none; border-top: 1px solid #cccccc; margin: 20px 0;">
            <p style="color: #888888;"><i>Note: This is an automatically generated mail. Please do not reply.</i></p>
          </body>
        </html>
        """
        iAction.Collaboration.SendMail(
            Body: mailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
        )
        processMap.status = 1
    }
 
    csvFile.delete()
    pdfFile.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")
 
/*
// Final email step
step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    def emailBody = """
        <html><body>
        Dear Team,<br><br>
        Please find attached the Root Login Report as of <b>""" +date+ """</b>.<br><br>
        Thank You,<br>
        Team ignio<br><br>
        <b>Note: This is an automatically generated mail. Please do not reply.</b>
        </body></html>
    """
 
    def reportName = "RootLoginReport_" +date+ ".csv"
    def file = new File(reportName)
file.write(processMap.report)
    file.setReadOnly()
    def MAX_SIZE = 10000000L
    def fileSize = file.size()
 
    if (fileSize < MAX_SIZE) {
        org.springframework.web.multipart.MultipartFile[] filesForAttachment = fileUtils.createMultiPartFileFromFile(file)
        iAction.Collaboration.SendMail(
            Body: emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with attachment.")
        processMap.status = 0
    } else {
        LOG.error("Attachment too large to send: " + fileSize)
        processMap.status = 1
    }
 
    file.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")*/
 
end("SUCCESS", Status.SUCCESS) {
    outputMap << [outputStream: "Root login report mail sent successfully.\nSuccess Count: " + processMap.successIPs + "\nFailure Count: " + processMap.failedIPs + "\nTotal Count:" + processMap.totalIps]
}
 
end("ERROR", Status.ERROR) {
    outputMap << [outputStream: "Failed to send root login report.\nSuccess Count: " + processMap.successIPs + "\nFailure Count: " + processMap.failedIPs + "\nTotal Count:" + processMap.totalIps]
}

/*

init {
    IPList = inputMap.RHEL9_IPList;
    emailTo = inputMap.emailTo;
    emailCC = inputMap.emailCC;
    emailsubject = inputMap.Emailsubject;
    currentdate = inputMap.currentdate;
    index_ip = 0;
    report = "Hostname,User,Host,Login Time,Logout Time,Duration\n";
}
 
step("getRootLoginDetails", StepType.ONFUNCTION) {
    def ip = processMap.IPList[processMap.index_ip]
 
    Map<String, Object> entityMap = new HashMap<>()
    entityMap.put("IPAddress", ip)
 
    Set<String> entityLabels = new HashSet<>()
    entityLabels.add("RHEL9") // Or your appropriate Linux OS label
 
    def entityInstance = ceb.get(entityLabels, entityMap)
    def entity = ceb.get(entityInstance.getId(), entityLabels)
 
    // Call the atomic function
    entity.FetchRootLoginDetails(ioutput: "output_report")
}
.to({ true }, "NextIP")
.elseTo("ERROR")
 
step("NextIP", StepType.ONIGNIO) {
    if (processMap.output_report?.outputStream) {
        processMap.report += processMap.output_report.outputStream
    } else {
        def ip = (processMap.index_ip < processMap.IPList.size()) ? processMap.IPList[processMap.index_ip] : "Unknown IP"
        processMap.report += "Unknown," +ip+ ",Error,Error,Error,Error\n"
    }
 
    LOG.error("Processed IP: " + ((processMap.index_ip < processMap.IPList.size()) ? processMap.IPList[processMap.index_ip] : "N/A"))
    processMap.index_ip++
}
.to({ processMap.index_ip < processMap.IPList.size() }, "getRootLoginDetails")
.to({ processMap.index_ip >= processMap.IPList.size() }, "SendEmailReport")
 

step("SendEmailReport", StepType.ONIGNIO) {
    def date = processMap.currentdate
    
    // Prepare email body
    def emailBody = """
    <html><body>
    Dear Team,<br><br>
    Please find attached the Root Login Report as of <b> """ +date+ """</b>.<br><br>
    Thank You,<br>
    Team ignio<br><br>
    <b>Note: This is an automatically generated mail. Please do not reply.</b>
    </body></html>
    """
    def reportName = "RootLoginReport_" + date + ".csv"

    // Write the report to a temporary file
    def file = new File(reportName)
    file.write(processMap.report)
    file.setReadOnly() 
    def MAX_SIZE = 3000000L
    def fileSize = file.size()

    // Check file size before attaching
    if (fileSize < MAX_SIZE) {
        
        org.springframework.web.multipart.MultipartFile[] filesForAttachment= fileUtils.createMultiPartFileFromFile(file)
        iAction.Collaboration.SendMail(
            Body:emailBody,
            Subject: processMap.emailsubject,
            To: processMap.emailTo,
            CC: processMap.emailCC,
            hasAttachment: filesForAttachment
        )
        LOG.error("Email sent successfully with attachment.")
        processMap.status = 0
    } else {
        LOG.error("Attachment too large to send: " + file.size())
        processMap.status = 1
    }
 
    // Clean up
    file.delete()
}
.to({ processMap.status == 0 }, "SUCCESS")
.elseTo("ERROR")
 
end("SUCCESS", Status.SUCCESS) {
    outputMap << [outputStream: "Root login report mail sent successfully."]
}
 
end("ERROR", Status.ERROR) {
    outputMap << [outputStream: "Failed to send root login report."]
}*/
 