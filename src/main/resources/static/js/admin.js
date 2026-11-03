// ===== Admin panel logic =====
if (localStorage.getItem("role") !== "ADMIN") window.location.href = "index.html";
document.getElementById("greeting").textContent = "Hi, " + (localStorage.getItem("fullName") || "Admin");

let currentTab = "customers";
const tabLoaders = { customers: loadCustomers, loans: loadLoans, transactions: loadTransactions };

document.querySelectorAll(".tab").forEach(tab => tab.addEventListener("click", () => {
    currentTab = tab.dataset.tab;
    document.querySelectorAll(".tab").forEach(t => t.classList.toggle("active", t === tab));
    refresh();
}));

async function refresh() {
    try {
        await Promise.all([loadStats(), tabLoaders[currentTab]()]);
    } catch (err) { toast(err.message, "error"); }
}

async function loadStats() {
    const s = await api("GET", "/api/admin/stats");
    document.getElementById("stCustomers").textContent = s.customers;
    document.getElementById("stDeposits").textContent = money(s.totalDeposits);
    document.getElementById("stTxns").textContent = s.transactions;
    document.getElementById("stLoans").textContent = s.pendingLoans;
}

function setContent(html) {
    document.getElementById("tabContent").innerHTML = html;
}

async function loadCustomers() {
    const rows = await api("GET", "/api/admin/customers");
    setContent(rows.length ? `<table>
        <tr><th>Name</th><th>Username</th><th>Mobile</th><th>Account no.</th><th>Type</th><th>Balance</th><th>Status</th><th>Action</th></tr>
        ${rows.map(({ user: u, account: a }) => `<tr>
            <td><b>${esc(u.fullName)}</b><br><small class="muted">${esc(u.email)}</small></td>
            <td>${esc(u.username)}</td><td>${esc(u.mobile)}</td><td>${esc(a.accountNumber)}</td><td>${a.accountType}</td>
            <td>${money(a.balance)}</td><td>${badge(a.status)}</td>
            <td>${a.status === "ACTIVE"
                ? `<button class="btn btn-sm btn-red" onclick="setStatus(${a.id}, 'freeze')">Freeze</button>`
                : `<button class="btn btn-sm btn-green" onclick="setStatus(${a.id}, 'unfreeze')">Unfreeze</button>`}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No customers</div>`);
}

async function setStatus(accountId, action) {
    try {
        const r = await api("POST", `/api/admin/accounts/${accountId}/${action}`);
        toast(r.message, "success");
        refresh();
    } catch (err) { toast(err.message, "error"); }
}

async function loadLoans() {
    const rows = await api("GET", "/api/admin/loans");
    setContent(rows.length ? `<table>
        <tr><th>#</th><th>Customer</th><th>Account</th><th>Type</th><th>Amount</th><th>Tenure</th><th>EMI</th><th>Applied</th><th>Status</th><th>Action</th></tr>
        ${rows.map(({ loan: l, customerName, accountNumber }) => `<tr>
            <td>${l.id}</td><td>${esc(customerName)}</td><td>${esc(accountNumber)}</td><td>${l.loanType}</td>
            <td>${money(l.principal)}</td><td>${l.tenureMonths} m</td><td>${money(l.emi)}</td><td>${formatDay(l.appliedAt)}</td>
            <td>${badge(l.status)}</td>
            <td>${l.status === "PENDING" ? `
                <button class="btn btn-sm btn-green" onclick="decideLoan(${l.id}, 'approve')">Approve</button>
                <button class="btn btn-sm btn-red" onclick="decideLoan(${l.id}, 'reject')">Reject</button>` : ""}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No loan applications</div>`);
}

async function decideLoan(loanId, action) {
    try {
        const r = await api("POST", `/api/admin/loans/${loanId}/${action}`);
        toast(r.message, "success");
        refresh();
    } catch (err) { toast(err.message, "error"); }
}

async function loadTransactions() {
    const rows = await api("GET", "/api/admin/transactions?limit=100");
    setContent(rows.length ? `<table>
        <tr><th>Date</th><th>Account</th><th>Description</th><th>Category</th><th>Reference</th><th>Amount</th><th>Balance</th></tr>
        ${rows.map(({ transaction: t, accountNumber }) => `<tr>
            <td>${formatDate(t.createdAt)}</td><td>${esc(accountNumber)}</td><td>${esc(t.description)}</td><td>${t.category}</td>
            <td>${esc(t.referenceNo)}</td>
            <td class="${t.type === "CREDIT" ? "credit" : "debit"}">${t.type === "CREDIT" ? "+" : "-"} ${money(t.amount)}</td>
            <td>${money(t.balanceAfter)}</td>
        </tr>`).join("")}</table>` : `<div class="empty">No transactions</div>`);
}

refresh();
