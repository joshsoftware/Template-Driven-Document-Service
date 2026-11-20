-- Seeds the first template: account statement (template_id 'statement', version 1), ACTIVE.
-- Source: src/test/resources/statement/v1 (template.html, styles.css, assets/images/bank-header.png,
-- contract.json, mapping.json).
-- template_html is the published, self-contained form: styles.css is inlined as a <style> block and
-- images as base64 data: URIs, because the PDF renderer loads only data: URIs (DataUriOnlyResolver).
-- Rows are write-once (trg_templates_write_once), so a change to this template ships as a new version
-- in a new migration; never edit this file once applied.

INSERT INTO templates (
    template_id,
    description,
    version,
    status,
    template_html,
    contract_json,
    mapping_json,
    created_by,
    activated_at
) VALUES (
    'statement',
    'Current and Savings Account Statement',
    1,
    'ACTIVE',
$html$<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<!--
  templateId: statement · version: v1 (template, contract and mapping are versioned together)
  Contract: contract.json in this folder (every ${...} is a contract key).
  Files: styles.css, assets/images/bank-header.png - all paths relative to this folder.
  The Java publish step inlines styles.css and assets (base64) and checks: th:text only (no unescaped output),
  no scripts, no external URLs, well-formed XHTML. Renderer: Thymeleaf 3.1 -> OpenHTMLtoPDF.
  Layout measured from the bank's sample (account-statement-1765362768775.pdf), units in pt.
-->
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
<meta charset="UTF-8"/>
<title>Current and Savings Account Statement</title>
<style type="text/css">/*<![CDATA[*/
/* statement · v1 · styles
   Loaded by template.html (<link href="styles.css">). Paths are relative to this version folder.
   OpenHTMLtoPDF features: running elements, @page margin boxes, -fs-table-paginate. No flexbox/grid. */

/* ---------- Page: banner on top, red bar + page number at the bottom, on every page ---------- */
@page {
  size: A4;
  margin: 82p
  <div class="disclaimer">
    <h3>Disclaimer</h3>
    <p>a)Call charges may apply depending on the service provider.</p>
    <p>b)For further details and applicable terms and conditions related to Insurance, you may visit the Debit Card section under www.bandhanbank.com.</p>

    <h3 style="margin-top: 22pt">For reporting loss/theft/misuse/unauthorised use of card or PIN/termination of your Debit Card reach us at:</h3>
    <p>a) Dedicated 24*7 Helpline No:</p>
    <p>-Toll-free number: 1800 258 8181</p>
    <p>-Customer Care Number: 033 4409 9090</p>
    <p>b) Through Internet banking and mobile banking app (mBandhan).</p>
    <p>c) You may also visit your nearest Bandhan Bank branch.</p>
    <p>d)For insurance related queries, please reach out: nonlife.insurance@bandhanbank.com.</p>

    <p class="gap">This is a system generated statement.</p>
  </div>
  <p class="end">**End of Statement**</p>t 31pt 62pt 31pt;
  @top-center    { content: element(pageHeader); vertical-align: top; }
  @bottom-center { content: element(pageFooter); vertical-align: bottom; }
}
.page-header { position: running(pageHeader); padding-top: 10pt; }
.page-header img { width: 533pt; height: 60pt; display: block; }
.page-footer { position: running(pageFooter); padding-bottom: 14pt; }
.page-footer .bar { margin: 0 5pt; height: 12pt; background-color: #cc0000; }
.page-footer .pages { margin-top: 3pt; text-align: center; font-size: 8pt; color: #555555; }
.page-no::before    { content: counter(page); }
.page-count::before { content: counter(pages); }

body {
  margin: 0; padding: 0 5pt;
  font-family: Helvetica, Arial, sans-serif;
  font-size: 11pt; line-height: 1.2; color: #000000;
}
.helv { font-family: Helvetica, Arial, sans-serif; }

h1.doc-title { margin: 22pt 0 0; text-align: center; font-size: 18pt; font-weight: bold; }
h1.doc-title span { border-bottom: 1.2pt solid #000000; padding-bottom: 1pt; }
.address { margin-top: 12pt; line-height: 16.5pt; }
.as-on { margin: 10pt 0 0; text-align: right; }

h2.section { margin: 12pt 0 8pt; font-size: 13pt; font-weight: bold; color: #004180; }
h2.section.center { text-align: center; margin-top: 32pt; }
.note { margin: 14pt 0 0; font-size: 10pt; font-weight: bold; color: #004180; }

table.grid { width: 100%; border-collapse: collapse; table-layout: fixed; -fs-table-paginate: paginate; }
table.grid th, table.grid td { border: 0.75pt solid #000000; }
table.grid thead th {
  background-color: #003366; color: #ffffff; font-family: Helvetica, Arial, sans-serif;
  font-size: 10pt; font-weight: bold; text-align: center; vertical-align: top; padding: 2pt 3pt;
}
thead { display: table-header-group; }
tr { page-break-inside: avoid; }

table.details td { height: 20pt; padding: 0 5pt; vertical-align: middle; }
table.details td.label { font-size: 10pt; font-weight: bold; }

table.txns td { padding: 1pt 2pt; text-align: center; vertical-align: middle; font-size: 10.5pt; }
table.txns td.desc { text-align: left; vertical-align: top; word-wrap: break-word; }
.empty { margin: 0; padding: 8pt; border: 0.75pt solid #000000; text-align: center; }

table.summary td { text-align: center; padding: 2pt; }
table.insurance td { text-align: center; vertical-align: middle; padding: 4pt 3pt; font-size: 10pt; }
table.insurance td p { margin: 0 0 11pt; }
table.insurance td p.last { margin: 0; }

.disclaimer h3 { margin: 40pt 0 2pt; font-size: 10pt; font-weight: bold; color: #004180; }
.disclaimer p { margin: 0; line-height: 16.5pt; }
.disclaimer .gap { margin-top: 22pt; }
.end { margin-top: 30pt; text-align: center; font-size: 10pt; font-weight: bold; color: #004180; }

/* ---------- Browser preview only ----------
   Browsers ignore running elements, @page margin boxes and counter(pages), so without this the banner
   and footer sit inline at the top. OpenHTMLtoPDF and print use print media, so the PDF is unaffected. */
@media screen {
  html { background: #e5e7eb; }
  body { position: relative; box-sizing: border-box; width: 595pt; margin: 16pt auto; padding: 82pt 36pt 62pt; background: #ffffff; }
  .page-header { position: absolute; top: 0; left: 31pt; right: 31pt; }
  .page-header img { width: 100%; height: auto; }
  .page-footer { position: absolute; bottom: 0; left: 31pt; right: 31pt; }
  .page-footer .pages { display: none; }
}

/*]]>*/</style>
</head>
<body>

  <!-- Running elements: OpenHTMLtoPDF moves these into the page margins of every page. -->
  <div class="page-header"><img src="data:image/png;base64,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" alt="Bandhan Bank"/></div>
  <div class="page-footer">
    <div class="bar"></div>
    <div class="pages helv">Page <span class="page-no"></span> of <span class="page-count"></span></div>
  </div>

  <h1 class="doc-title"><span>Current and Savings Account Statement</span></h1>

  <!-- Address block -->
  <div class="address helv">
    <div th:text="${customerName}">CUSTOMER NAME</div>
    <div th:each="line : ${address.lines}" th:text="${line}">ADDRESS LINE</div>
    <div th:text="${address.city + ' - ' + address.pinCode}">CITY - 000000</div>
    <div th:text="${address.state}">STATE</div>
    <div th:if="${address.country != null}" th:text="${address.country}">COUNTRY</div>
  </div>

  <p class="as-on helv">Account Statement as on
    <span th:text="${#temporals.format(statementDate, 'MMMM dd, yyyy')}">January 01, 2025</span></p>

  <!-- Customer Account Details -->
  <h2 class="section">Customer Account Details</h2>
  <table class="grid details">
    <colgroup><col style="width: 174.3pt"/><col/></colgroup>
    <tbody>
      <tr><td class="label">Account Number</td><td th:text="${accountNumber}">XXXXXXXXXXXXXX</td></tr>
      <tr><td class="label">Customer Name</td><td th:text="${customerName}">CUSTOMER NAME</td></tr>
      <tr><td class="label">Account Type</td><td th:text="${accountType}">Savings Account</td></tr>
      <tr><td class="label">Branch Details</td><td th:text="${branchDetails}">Branch Name</td></tr>
      <tr><td class="label">Customer ID / CIF</td><td th:text="${customerId}">XXXXXXXXX</td></tr>
      <tr><td class="label">IFSC</td><td th:text="${ifsc}">XXXX0000000</td></tr>
      <tr><td class="label">MICR Code</td><td th:text="${micrCode}">XXXXXX</td></tr>
      <tr><td class="label">Statement Period</td>
          <td>From <span th:text="${#temporals.format(periodFrom, 'dd MMMM yyyy')}">01 January 2025</span>
              to <span th:text="${#temporals.format(periodTo, 'dd MMMM yyyy')}">31 January 2025</span></td></tr>
    </tbody>
  </table>

  <!-- Statement Details: header row repeats on every page (-fs-table-paginate). -->
  <h2 class="section center">Statement Details</h2>
  <table class="grid txns" th:unless="${#lists.isEmpty(transactions)}">
    <colgroup>
      <col style="width: 80.5pt"/><col style="width: 80.5pt"/><col style="width: 160.9pt"/>
      <col style="width: 80.5pt"/><col style="width: 40.2pt"/><col style="width: 80.5pt"/>
    </colgroup>
    <thead>
      <tr><th>Transaction Date</th><th>Value Date</th><th>Description</th>
          <th>Amount</th><th>Dr/Cr</th><th>Balance</th></tr>
    </thead>
    <tbody>
      <tr th:each="t : ${transactions}">
        <td th:text="${#temporals.format(t.transactionDate, 'MMMM dd, yyyy')}">January 01, 2025</td>
        <td th:text="${#temporals.format(t.valueDate, 'MMMM dd, yyyy')}">January 01, 2025</td>
        <td class="desc" th:text="${t.description}">Transaction description</td>
        <td th:text="${t.amount.toPlainString()}">0</td>
        <td th:text="${t.drCr}">D</td>
        <td th:text="${#numbers.formatDecimal(t.balance, 1, 'NONE', 2, 'POINT')}">0.00</td>
      </tr>
    </tbody>
  </table>
  <p class="empty" th:if="${#lists.isEmpty(transactions)}">No transactions for this period</p>

  <!-- Statement Summary -->
  <h2 class="section">Statement Summary</h2>
  <table class="grid summary helv">
    <thead><tr><th>Opening Balance</th><th>Total Credits</th><th>Total Debits</th><th>Closing Balance</th></tr></thead>
    <tbody><tr>
      <td th:text="${#numbers.formatDecimal(openingBalance, 1, 'COMMA', 2, 'POINT')}">0.00</td>
      <td th:text="${#numbers.formatDecimal(totalCredits, 1, 'COMMA', 2, 'POINT')}">0.00</td>
      <td th:text="${#numbers.formatDecimal(totalDebits, 1, 'COMMA', 2, 'POINT')}">0.00</td>
      <td th:text="${#numbers.formatDecimal(closingBalance, 1, 'COMMA', 2, 'POINT')}">0.00</td>
    </tr></tbody>
  </table>

  <p class="note">Statement generated on
    <span th:text="${#temporals.format(generatedAt, 'yyyy-MM-dd HH:mm:ss')}">2025-01-01 00:00:00</span> IST</p>

  <!-- ===== Fixed bank text (not in the contract) ===== -->
  <p class="note" style="margin-top: 22pt">Please find the Details of Complimentary Insurance Cover on your Debit Card</p>
  <table class="grid insurance" style="margin-top: 34pt">
    <colgroup>
      <col style="width: 72.6pt"/><col style="width: 116.2pt"/><col style="width: 101.7pt"/>
      <col style="width: 159.8pt"/><col style="width: 72.6pt"/>
    </colgroup>
    <thead>
      <tr><th>Debit Card Variant Name</th><th>Insurance Coverage Details</th><th>Name of Insurance Company</th>
          <th>Address of Insurance Company</th><th>Telephone Number of Insurance Company</th></tr>
    </thead>
    <tbody>
      <tr>
        <td>Rupay Platinum</td>
        <td>Personal Accident insurance and Permanent Disability cover upto INR 2 Lakhs.</td>
        <td>Tata AIG General Insurance Company Ltd</td>
        <td>TATA – AIG General Insurance Co. Ltd, 8th Floor, R Tech Park, Village Pahadi Taluka Goregaon, Mumbai Suburban, CTS No: 588/2(pt), 586/4(Apt), 586/7/B Goregaon, Mumbai - 400063</td>
        <td>18002667780</td>
      </tr>
      <tr>
        <td>Mastercard Platinum</td>
        <td><p>Personal Accident Death Cover (Rail/Road/Air) INR 10 lakh</p>
            <p>Air Accident Death Cover INR 1 Cr (If ticket is purchased with Bandhan Bank Mastercard Platinum only)</p>
            <p>Purchase Protection: Up to INR 2 lakh</p>
            <p class="last">Lost Card Liability(including counterfeit/skimming /online fraud): Up to INR 5 lakh</p></td>
        <td>ICICI Lombard General Insurance Company Ltd</td>
        <td>ICICI Bank Tower, Plot No. 12, Financial District, Nanakramguda, Gachibowli, Hyderabad, Telangana, PIN: 500032</td>
        <td>18002666</td>
      </tr>
      <tr>
        <td>Visa Platinum</td>
        <td><p>Personal Accident Death Cover (Rail/Road/Air): INR 10 lakh</p>
            <p>Air Accident Death Cover: INR 1 Cr (If ticket is purchased with Bandhan Bank Visa Platinum Card only)</p>
            <p>Purchase Protection: Up to INR 2 lakh</p>
            <p class="last">Lost Card Liability(including counterfeit/skimming /online fraud): Up to INR 3.5 lakh</p></td>
        <td>ICICI Lombard General Insurance Company Ltd</td>
        <td>ICICI Bank Tower, Plot No. 12, Financial District, Nanakramguda, Gachibowli, Hyderabad, Telangana, PIN: 500032</td>
        <td>18002666</td>
      </tr>
      <tr>
        <td>Visa Classic</td>
        <td><p>Personal Accident Death Cover (Rail/Road): INR 2 lakh</p>
            <p>Purchase Protection:Up to INR 50000</p>
            <p class="last">Lost Card Liability(including counterfeit/skimming /online fraud): Up to INR 50,000</p></td>
        <td>ICICI Lombard General Insurance Company Ltd</td>
        <td>ICICI Bank Tower, Plot No. 12, Financial District, Nanakramguda, Gachibowli, Hyderabad, Telangana, PIN: 500032</td>
        <td>18002666</td>
      </tr>
      <tr>
        <td>Avni Debit Card</td>
        <td><p>Personal Accident Death Cover (Rail/Road/Air) INR 10 lakh</p>
            <p>Air Accident Death Cover INR 1 Cr (If ticket is purchased with Bandhan Bank Mastercard Platinum only)</p>
            <p>Purchase Protection: Up to INR 2 lakh</p>
            <p class="last">Lost Card Liability(including counterfeit/skimming /online fraud): Up to INR 3.5 lakh</p></td>
        <td>ICICI Lombard General Insurance Company Ltd</td>
        <td>ICICI Bank Tower, Plot No. 12, Financial District, Nanakramguda, Gachibowli, Hyderabad, Telangana, PIN: 500032</td>
        <td>18002666</td>
      </tr>
    </tbody>
  </table>

  <div class="disclaimer">
    <h3>Disclaimer</h3>
    <p>a)Call charges may apply depending on the service provider.</p>
    <p>b)For further details and applicable terms and conditions related to Insurance, you may visit the Debit Card section under www.bandhanbank.com.</p>

    <h3 style="margin-top: 22pt">For reporting loss/theft/misuse/unauthorised use of card or PIN/termination of your Debit Card reach us at:</h3>
    <p>a) Dedicated 24*7 Helpline No:</p>
    <p>-Toll-free number: 1800 258 8181</p>
    <p>-Customer Care Number: 033 4409 9090</p>
    <p>b) Through Internet banking and mobile banking app (mBandhan).</p>
    <p>c) You may also visit your nearest Bandhan Bank branch.</p>
    <p>d)For insurance related queries, please reach out: nonlife.insurance@bandhanbank.com.</p>

    <p class="gap">This is a system generated statement.</p>
  </div>
  <p class="end">**End of Statement**</p>

</body>
</html>
$html$,
$contract${
  "fields": {
    "customerName":   { "type": "string", "required": true },
    "address":        { "type": "object", "required": true, "fields": {
      "lines":   { "type": "array",  "required": true,
                   "items": { "type": "string" } },
      "city":    { "type": "string", "required": true },
      "state":   { "type": "string", "required": true },
      "pinCode": { "type": "string", "required": true },
      "country": { "type": "string", "required": false } } },
    "statementDate":  { "type": "date",   "required": true },
    "accountNumber":  { "type": "string", "required": true },
    "accountType":    { "type": "string", "required": true },
    "branchDetails":  { "type": "string", "required": true },
    "customerId":     { "type": "string", "required": true },
    "ifsc":           { "type": "string", "required": true },
    "micrCode":       { "type": "string", "required": true },
    "periodFrom":     { "type": "date",   "required": true },
    "periodTo":       { "type": "date",   "required": true },
    "transactions":   { "type": "array",  "required": true,
                        "items": { "type": "object", "fields": {
      "transactionDate": { "type": "date",   "required": true },
      "valueDate":       { "type": "date",   "required": true },
      "description":     { "type": "string", "required": true },
      "amount":          { "type": "number", "required": true },
      "drCr":            { "type": "enum",   "required": true,
                           "values": ["D", "C"] },
      "balance":         { "type": "number", "required": true } } } },
    "openingBalance": { "type": "number", "required": true },
    "totalCredits":   { "type": "number", "required": true },
    "totalDebits":    { "type": "number", "required": true },
    "closingBalance": { "type": "number", "required": true },
    "generatedAt":    { "type": "datetime", "required": true }
  }
}
$contract$,
$mapping${
  "dateFormats": { "date": "yyyyMMdd", "datetime": "yyyy-MM-dd'T'HH:mm:ss" },
  "transform": {
    "accountNumber": "AccountDetails.AccountId",
    "periodFrom":    "StatementPeriod.FromDate",
    "periodTo":      "StatementPeriod.ToDate",
    "transactions": {
      "from": "AccountStatement",
      "map": {
        "transactionDate": "TransactionDate",   "valueDate": "ValueDate",
        "description":     "Desc",              "amount":    "TransactionAmount",
        "drCr":            "CodeDRCR",          "balance":   "RunningBalance"
      }
    },
    "customerName":   "CustomerDetails.CustomerName",
    "address": {
      "from":   "CustomerDetails.AddressDetails",
      "select": { "AddressType": "Resedential Address" },
      "map": {
        "lines": "AddressLine",  "city":    "City",     "state": "State",
        "pinCode": "PinCode",    "country": "Country"
      }
    },
    "customerId":     "CustomerDetails.CustomerId",
    "accountType":    "AccountDetails.AccountType",
    "branchDetails":  "AccountDetails.BranchName",
    "ifsc":           "AccountDetails.IFSC",
    "micrCode":       "AccountDetails.MICR",
    "statementDate":  "system.today()",
    "generatedAt":    "system.now()",
    "openingBalance": "AccountSummary.OpeningBalance",
    "totalCredits":   "AccountSummary.TotalCredits",
    "totalDebits":    "AccountSummary.TotalDebits",
    "closingBalance": "AccountSummary.ClosingBalance"
  }
}
$mapping$,
    'flyway-seed',
    NOW()
);
