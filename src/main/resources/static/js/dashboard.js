// ===== Customer dashboard logic =====
// Each menu item shows one <section>. Each section has a "load" function
// that calls the backend API and fills the HTML.

if (!localStorage.getItem("token")) window.location.href = "index.html";

let profile = null;            // { user: {...}, account: {...} }
let balanceVisible = false;
let lastStatement = [];

// ---------------------------------------------------------------- navigation
const sectionLoaders = {
    home: loadHome,
    transfer: loadBeneficiaryChips,
    beneficiaries: loadBeneficiaries,
    fd: () => { loadFds(); updateFdPreview(); },
    loans: () => { loadLoans(); updateEmi(); },
    cards: renderCard,
    statement: loadStatement,
    profile: renderProfile
};

function showSection(id) {
    document.querySelectorAll(".section").forEach(s => s.classList.add("hidden"));
    document.getElementById(id).classList.remove("hidden");
    document.querySelectorAll(".nav-item").forEach(n => n.classList.toggle("active", n.dataset.section === id));
    document.querySelector(".sidebar").classList.remove("open");
    if (sectionLoaders[id]) sectionLoaders[id]();
    window.scrollTo(0, 0);
}

document.querySelectorAll(".nav-item").forEach(item =>
    item.addEventListener("click", () => showSection(item.dataset.section)));

document.querySelectorAll("[data-go]").forEach(el =>
    el.addEventListener("click", (e) => {
        e.preventDefault();
        showSection(el.dataset.go);
        if (el.dataset.bill) selectBillType(el.dataset.bill);
    }));

// ---------------------------------------------------------------- popups
// Shows the MPIN popup and waits. Returns the MPIN, or null if cancelled.
function askMpin(text) {
    const modal = document.getElementById("mpinModal");
    const input = document.getElementById("mpinInput");
    document.getElementById("mpinText").textContent = text;
    input.value = "";
    modal.classList.remove("hidden");
    input.focus();

    return new Promise(resolve => {
        const finish = (value) => {
            modal.classList.add("hidden");
            document.getElementById("mpinOk").onclick = null;
            document.getElementById("mpinCancel").onclick = null;
            input.onkeydown = null;
            resolve(value);
        };
        document.getElementById("mpinOk").onclick = () => {
            if (!/^\d{4}$/.test(input.value)) return toast("MPIN must be 4 digits", "error");
            finish(input.value);
        };
        document.getElementById("mpinCancel").onclick = () => finish(null);
        input.onkeydown = (e) => { if (e.key === "Enter") document.getElementById("mpinOk").click(); };
    });
}

function showReceipt(receipt) {
    document.getElementById("rMessage").textContent = receipt.message;
    document.getElementById("rAmount").textContent = money(receipt.amount);
    document.getElementById("rRef").textContent = receipt.referenceNo;
    document.getElementById("rDate").textContent = formatDate(new Date());
    document.getElementById("rBalance").textContent = money(receipt.balance);
    document.getElementById("receiptModal").classList.remove("hidden");
}

// Asks MPIN, runs the API call, shows the receipt, refreshes balance.
async function payWithMpin(text, apiCall) {
    const mpin = await askMpin(text);
    if (!mpin) return false;
    try {
        const receipt = await apiCall(mpin);
        showReceipt(receipt);
        await loadProfile();
        return true;
    } catch (err) {
        toast(err.message, "error");
        return false;
    }
}

// ---------------------------------------------------------------- home
async function loadProfile() {
    profile = await api("GET", "/api/auth/me");
    const a = profile.account;
    document.getElementById("greeting").textContent = "Hi, " + profile.user.fullName.split(" ")[0];
    document.getElementById("accType").textContent = a.accountType + " ACCOUNT";
    document.getElementById("accNumber").textContent = a.accountNumber;
    document.getElementById("upiId").textContent = a.upiId;
    document.getElementById("ifsc").textContent = a.ifsc;
    document.getElementById("accStatus").innerHTML = badge(a.status);
    renderBalance();
}

function renderBalance() {
    document.getElementById("balance").textContent = balanceVisible ? money(profile.account.balance) : "\u20B9 \u2022\u2022\u2022\u2022\u2022\u2022";
    document.querySelector("#toggleBalance i").className = balanceVisible ? "bi bi-eye-slash" : "bi bi-eye";
}

document.getElementById("toggleBalance").addEventListener("click", () => {
    balanceVisible = !balanceVisible;
    renderBalance();
});

async function loadHome() {
    await loadProfile();
    const list = await api("GET", "/api/account/transactions?limit=6");
    document.getElementById("recentList").innerHTML = list.length
        ? list.map(transactionRow).join("")
        : `<div class="empty">No transactions yet</div>`;
}

const categoryIcons = { DEPOSIT: "bi-plus-circle", WITHDRAW: "bi-cash", TRANSFER: "bi-arrow-left-right", BILL: "bi-lightning-charge", RECHARGE: "bi-phone", FD: "bi-piggy-bank", LOAN: "bi-bank" };

function transactionRow(t) {
    const cls = t.type === "CREDIT" ? "credit" : "debit";
    return `<div class="txn">
        <div class="ico ${cls}"><i class="bi ${categoryIcons[t.category] || "bi-circle"}"></i></div>
        <div class="info"><div>${esc(t.description)}</div><small>${formatDate(t.createdAt)} &middot; ${esc(t.referenceNo)}</small></div>
        <div class="amt ${cls}">${t.type === "CREDIT" ? "+" : "-"} ${money(t.amount)}<small>Bal ${money(t.balanceAfter)}</small></div>
    </div>`;
}

// ---------------------------------------------------------------- send money
document.getElementById("verifyPayee").addEventListener("click", verifyPayee);

async function verifyPayee() {
    const box = document.getElementById("payeeName");
    const to = document.getElementById("tTo").value.trim();
    if (!to) return toast("Enter account number or UPI ID", "error");
    try {
        const payee = await api("GET", "/api/account/lookup?to=" + encodeURIComponent(to));
        box.innerHTML = `<i class="bi bi-patch-check-fill"></i> ${esc(payee.name)} &middot; A/c ${esc(maskAccount(payee.accountNumber))}`;
        box.classList.remove("hidden");
        return payee;
    } catch (err) {
        box.classList.add("hidden");
        toast(err.message, "error");
        return null;
    }
}

document.getElementById("transferForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payee = await verifyPayee();
    if (!payee) return;
    const amount = document.getElementById("tAmount").value;
    const ok = await payWithMpin(`Send ${money(amount)} to ${payee.name}`, (mpin) =>
        api("POST", "/api/account/transfer", {
            to: document.getElementById("tTo").value.trim(),
            amount,
            remarks: document.getElementById("tRemarks").value,
            mpin
        }));
    if (ok) {
        e.target.reset();
        document.getElementById("payeeName").classList.add("hidden");
    }
});

async function loadBeneficiaryChips() {
    const list = await api("GET", "/api/beneficiaries");
    const box = document.getElementById("benChips");
    box.innerHTML = list.length
        ? list.map(b => `<span class="chip" data-acc="${esc(b.accountNumber)}"><i class="bi bi-person"></i> ${esc(b.nickname || b.name)}</span>`).join("")
        : `<span class="muted">No beneficiaries yet. Add one from the Beneficiaries menu.</span>`;
    box.querySelectorAll(".chip").forEach(chip => chip.addEventListener("click", () => {
        document.getElementById("tTo").value = chip.dataset.acc;
        verifyPayee();
    }));
}

// ---------------------------------------------------------------- beneficiaries
async function loadBeneficiaries() {
    const list = await api("GET", "/api/beneficiaries");
    document.getElementById("benList").innerHTML = list.length
        ? list.map(b => `<div class="txn">
              <div class="ico credit"><i class="bi bi-person-fill"></i></div>
              <div class="info"><div>${esc(b.name)} ${b.nickname ? `<small>(${esc(b.nickname)})</small>` : ""}</div>
                  <small>A/c ${esc(b.accountNumber)} &middot; ${esc(b.ifsc)}</small></div>
              <button class="btn btn-sm btn-accent" onclick="payBeneficiary('${esc(b.accountNumber)}')">Pay</button>
              <button class="btn btn-sm btn-red" onclick="deleteBeneficiary(${b.id})"><i class="bi bi-trash"></i></button>
          </div>`).join("")
        : `<div class="empty">No beneficiaries yet</div>`;
}

function payBeneficiary(accountNumber) {
    showSection("transfer");
    document.getElementById("tTo").value = accountNumber;
    verifyPayee();
}

async function deleteBeneficiary(id) {
    if (!confirm("Remove this beneficiary?")) return;
    try {
        await api("DELETE", "/api/beneficiaries/" + id);
        toast("Beneficiary removed", "success");
        loadBeneficiaries();
    } catch (err) { toast(err.message, "error"); }
}

document.getElementById("benForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        await api("POST", "/api/beneficiaries", Object.fromEntries(new FormData(e.target).entries()));
        toast("Beneficiary added", "success");
        e.target.reset();
        e.target.ifsc.value = "SMBK0001234";
        loadBeneficiaries();
    } catch (err) { toast(err.message, "error"); }
});

// ---------------------------------------------------------------- deposit / withdraw
function cashForm(formId, url, word) {
    document.getElementById(formId).addEventListener("submit", async (e) => {
        e.preventDefault();
        const amount = e.target.querySelector("input").value;
        const ok = await payWithMpin(`${word} ${money(amount)}`, (mpin) => api("POST", url, { amount, mpin }));
        if (ok) e.target.reset();
    });
}
cashForm("depositForm", "/api/account/deposit", "Deposit");
cashForm("withdrawForm", "/api/account/withdraw", "Withdraw");

// ---------------------------------------------------------------- bills & recharge
const billers = {
    ELECTRICITY: { label: "Consumer number", operators: ["TPCODL (Odisha)", "BSES Rajdhani", "Tata Power Mumbai", "MSEDCL", "BESCOM"] },
    MOBILE: { label: "Mobile number", operators: ["Jio", "Airtel", "Vi", "BSNL"] },
    DTH: { label: "Subscriber ID", operators: ["Tata Play", "Airtel Digital TV", "Dish TV", "Sun Direct"] },
    WATER: { label: "Consumer number", operators: ["Delhi Jal Board", "BMC Water", "PHED Odisha"] },
    GAS: { label: "Consumer number", operators: ["Indane", "HP Gas", "Bharat Gas", "Mahanagar Gas"] }
};
let billType = "ELECTRICITY";

function selectBillType(type) {
    billType = type;
    document.querySelectorAll("#billTypes .tile").forEach(t => t.classList.toggle("selected", t.dataset.type === type));
    document.getElementById("bOperator").innerHTML = billers[type].operators.map(o => `<option>${o}</option>`).join("");
    document.getElementById("bNumberLabel").textContent = billers[type].label;
}
document.querySelectorAll("#billTypes .tile").forEach(t => t.addEventListener("click", () => selectBillType(t.dataset.type)));
selectBillType("ELECTRICITY");

document.getElementById("billForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
        billType,
        operator: document.getElementById("bOperator").value,
        consumerNumber: document.getElementById("bNumber").value.trim(),
        amount: document.getElementById("bAmount").value
    };
    const ok = await payWithMpin(`Pay ${money(body.amount)} to ${body.operator}`, (mpin) =>
        api("POST", "/api/bills/pay", { ...body, mpin }));
    if (ok) e.target.reset();
});

// ---------------------------------------------------------------- fixed deposits
async function updateFdPreview() {
    const amount = document.getElementById("fdAmount").value;
    const months = document.getElementById("fdMonths").value;
    if (!amount || amount <= 0) return;
    try {
        const p = await api("GET", `/api/fd/preview?amount=${amount}&months=${months}`);
        document.getElementById("fdPreview").innerHTML = `
            <div><span class="muted">Interest rate</span><b>${p.interestRate}% p.a.</b></div>
            <div><span class="muted">Maturity amount</span><b>${money(p.maturityAmount)}</b></div>
            <div><span class="muted">Maturity date</span><b>${formatDay(p.maturityDate)}</b></div>`;
    } catch (err) { /* ignore while typing */ }
}
document.getElementById("fdAmount").addEventListener("input", updateFdPreview);
document.getElementById("fdMonths").addEventListener("change", updateFdPreview);

document.getElementById("fdForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const amount = document.getElementById("fdAmount").value;
    const tenureMonths = Number(document.getElementById("fdMonths").value);
    const ok = await payWithMpin(`Open FD of ${money(amount)}`, (mpin) => api("POST", "/api/fd", { amount, tenureMonths, mpin }));
    if (ok) loadFds();
});

async function loadFds() {
    const list = await api("GET", "/api/fd");
    document.getElementById("fdList").innerHTML = list.length ? `<table>
        <tr><th>FD #</th><th>Principal</th><th>Rate</th><th>Tenure</th><th>Maturity</th><th>Maturity date</th><th>Status</th><th></th></tr>
        ${list.map(fd => `<tr>
            <td>${fd.id}</td><td>${money(fd.principal)}</td><td>${fd.interestRate}%</td><td>${fd.tenureMonths} m</td>
            <td>${money(fd.maturityAmount)}</td><td>${formatDay(fd.maturityDate)}</td><td>${badge(fd.status)}</td>
            <td>${fd.status === "ACTIVE" ? `<button class="btn btn-sm btn-outline" onclick="closeFd(${fd.id})">Close</button>` : ""}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No fixed deposits yet</div>`;
}

async function closeFd(id) {
    if (!confirm("Closing before maturity gives 1% less interest. Continue?")) return;
    const ok = await payWithMpin("Close FD #" + id, (mpin) => api("POST", `/api/fd/${id}/close`, { mpin }));
    if (ok) loadFds();
}

// ---------------------------------------------------------------- loans
async function updateEmi() {
    const type = document.getElementById("lType").value;
    const amount = document.getElementById("lAmount").value;
    const months = document.getElementById("lMonths").value;
    if (!amount || !months || amount <= 0 || months <= 0) return;
    try {
        const r = await api("GET", `/api/loans/emi-calculator?loanType=${type}&amount=${amount}&months=${months}`);
        document.getElementById("emiResult").innerHTML = `
            <div><span class="muted">Interest rate</span><b>${r.interestRate}% p.a.</b></div>
            <div><span class="muted">Monthly EMI</span><b>${money(r.emi)}</b></div>
            <div><span class="muted">Total interest</span><b>${money(r.totalInterest)}</b></div>
            <div><span class="muted">Total payable</span><b>${money(r.totalPayable)}</b></div>`;
    } catch (err) { /* ignore while typing */ }
}
["lType", "lAmount", "lMonths"].forEach(id => document.getElementById(id).addEventListener("input", updateEmi));

document.getElementById("loanForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const r = await api("POST", "/api/loans", {
            loanType: document.getElementById("lType").value,
            amount: document.getElementById("lAmount").value,
            tenureMonths: Number(document.getElementById("lMonths").value)
        });
        toast(r.message, "success");
        loadLoans();
    } catch (err) { toast(err.message, "error"); }
});

async function loadLoans() {
    const list = await api("GET", "/api/loans");
    document.getElementById("loanList").innerHTML = list.length ? `<table>
        <tr><th>Loan #</th><th>Type</th><th>Amount</th><th>Rate</th><th>EMI</th><th>EMIs paid</th><th>Outstanding</th><th>Status</th><th></th></tr>
        ${list.map(l => `<tr>
            <td>${l.id}</td><td>${l.loanType}</td><td>${money(l.principal)}</td><td>${l.interestRate}%</td><td>${money(l.emi)}</td>
            <td>${l.emisPaid}/${l.tenureMonths}</td><td>${money(l.outstanding)}</td><td>${badge(l.status)}</td>
            <td>${l.status === "APPROVED" ? `<button class="btn btn-sm" onclick="payEmi(${l.id}, ${l.emi})">Pay EMI</button>` : ""}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No loans yet</div>`;
}

async function payEmi(id, emi) {
    const ok = await payWithMpin(`Pay EMI of ${money(emi)} for loan #${id}`, (mpin) => api("POST", `/api/loans/${id}/pay-emi`, { mpin }));
    if (ok) loadLoans();
}

// ---------------------------------------------------------------- cards
function renderCard() {
    const a = profile.account;
    document.getElementById("cardNumber").textContent = a.cardNumber.replace(/(\d{4})(?=\d)/g, "$1 ").replace(/^\d{4} \d{4} \d{4}/, "XXXX XXXX XXXX");
    document.getElementById("cardName").textContent = profile.user.fullName;
    document.getElementById("debitCard").classList.toggle("blocked", a.cardBlocked);
    document.getElementById("cardStatus").innerHTML = badge(a.cardBlocked ? "BLOCKED" : "ACTIVE");
    const btn = document.getElementById("cardToggle");
    btn.textContent = a.cardBlocked ? "Unblock Card" : "Block Card";
    btn.className = "btn " + (a.cardBlocked ? "btn-green" : "btn-red");
}

document.getElementById("cardToggle").addEventListener("click", async () => {
    const action = profile.account.cardBlocked ? "unblock" : "block";
    try {
        const r = await api("POST", "/api/account/card/" + action);
        toast(r.message, "success");
        await loadProfile();
        renderCard();
    } catch (err) { toast(err.message, "error"); }
});

// ---------------------------------------------------------------- statement
const today = new Date().toISOString().slice(0, 10);
document.getElementById("sTo").value = today;
document.getElementById("sFrom").value = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10);
document.getElementById("sSearch").addEventListener("click", loadStatement);

async function loadStatement() {
    const from = document.getElementById("sFrom").value;
    const to = document.getElementById("sTo").value;
    try {
        lastStatement = await api("GET", `/api/account/statement?from=${from}&to=${to}`);
    } catch (err) { return toast(err.message, "error"); }
    document.getElementById("statementTable").innerHTML = lastStatement.length ? `<table>
        <tr><th>Date</th><th>Description</th><th>Reference</th><th>Debit</th><th>Credit</th><th>Balance</th></tr>
        ${lastStatement.map(t => `<tr>
            <td>${formatDate(t.createdAt)}</td><td>${esc(t.description)}</td><td>${esc(t.referenceNo)}</td>
            <td class="debit">${t.type === "DEBIT" ? money(t.amount) : ""}</td>
            <td class="credit">${t.type === "CREDIT" ? money(t.amount) : ""}</td>
            <td>${money(t.balanceAfter)}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No transactions in this period</div>`;
}

document.getElementById("sCsv").addEventListener("click", () => {
    if (!lastStatement.length) return toast("Nothing to download", "error");
    const rows = [["Date", "Description", "Reference", "Type", "Amount", "Balance"]]
        .concat(lastStatement.map(t => [t.createdAt, t.description, t.referenceNo, t.type, t.amount, t.balanceAfter]));
    const csv = rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    link.download = "statement.csv";
    link.click();
});

// ---------------------------------------------------------------- profile
function renderProfile() {
    const u = profile.user, a = profile.account;
    document.getElementById("profileDetails").innerHTML = `
        <div style="display:flex;align-items:center;gap:16px;margin-bottom:16px">
            <div class="logo-mark" style="width:56px;height:56px;font-size:22px">${esc(u.fullName[0])}</div>
            <div><h3 style="margin:0">${esc(u.fullName)}</h3><span class="muted">Customer since ${formatDay(u.createdAt)}</span></div>
        </div>
        <table>
            <tr><td class="muted">Username</td><td>${esc(u.username)}</td><td class="muted">Email</td><td>${esc(u.email)}</td></tr>
            <tr><td class="muted">Mobile</td><td>${esc(u.mobile)}</td><td class="muted">Account no.</td><td>${esc(a.accountNumber)}</td></tr>
            <tr><td class="muted">Branch</td><td>${esc(a.branch)}</td><td class="muted">IFSC</td><td>${esc(a.ifsc)}</td></tr>
        </table>`;
}

function simpleForm(formId, url) {
    document.getElementById(formId).addEventListener("submit", async (e) => {
        e.preventDefault();
        try {
            const r = await api("POST", url, Object.fromEntries(new FormData(e.target).entries()));
            toast(r.message, "success");
            e.target.reset();
        } catch (err) { toast(err.message, "error"); }
    });
}
simpleForm("pwdForm", "/api/account/change-password");
simpleForm("mpinForm", "/api/account/change-mpin");

// ---------------------------------------------------------------- start
loadHome().catch(err => toast(err.message, "error"));
