package com.example.retiretracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.retiretracker.data.*
import com.example.retiretracker.worker.BudgetAlertWorker
import com.example.retiretracker.worker.DailyUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        scheduleDailyUpdate()
        setContent { MaterialTheme { AppNav() } }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun scheduleDailyUpdate() {
        val request = PeriodicWorkRequestBuilder<DailyUpdateWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("daily_update", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

private val tabs = listOf("dashboard", "expense", "etf", "report", "settings")

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { route ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { nav.navigate(route) },
                    label = { Text(route) },
                    icon = {}
                )
            }
        }
    }) { pad ->
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.padding(pad)) {
            composable("dashboard") { DashboardPage() }
            composable("expense") { ExpensePage() }
            composable("etf") { EtfPage() }
            composable("report") { ReportPage() }
            composable("settings") { SettingsPage() }
        }
    }
}

private fun normalizeEtfSymbol(symbol: String): String {
    val trimmed = symbol.trim().uppercase()
    return if (trimmed.matches(Regex("""\d{4,6}[A-Z]?"""))) "$trimmed.TW" else trimmed
}

@Composable
fun DashboardPage() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    var todayExpense by remember { mutableDoubleStateOf(0.0) }
    var totalAsset by remember { mutableDoubleStateOf(0.0) }
    var dayChange by remember { mutableDoubleStateOf(0.0) }
    var dailyBudget by remember { mutableDoubleStateOf(0.0) }
    var lastUpdatedText by remember { mutableStateOf("尚未更新") }

    fun load() {
        scope.launch(Dispatchers.IO) {
            val today = LocalDate.now().toString()
            val yesterday = LocalDate.now().minusDays(1).toString()
            val latest = db.assetSnapshotDao().latest()
            val y = db.assetSnapshotDao().findByDate(yesterday)
            val expense = db.expenseDao().totalByDate(today)
            val budget = prefs.getDailyBudget()
            val ts = prefs.getLastUpdateEpochMs()
            withContext(Dispatchers.Main) {
                todayExpense = expense
                totalAsset = latest?.totalAsset ?: 0.0
                dayChange = (latest?.totalAsset ?: 0.0) - (y?.totalAsset ?: 0.0)
                dailyBudget = budget
                lastUpdatedText = if (ts <= 0L) "尚未更新" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("今日支出：$${"%.2f".format(todayExpense)}")
        if (dailyBudget > 0) {
            Text("每日預算：$${"%.2f".format(dailyBudget)}")
            val remain = dailyBudget - todayExpense
            if (remain < 0) {
                Text("⚠️ 今日已超支：$${"%.2f".format(-remain)}", color = MaterialTheme.colorScheme.error)
            } else {
                Text("今日預算剩餘：$${"%.2f".format(remain)}")
            }
        }
        Text("總資產：$${"%.2f".format(totalAsset)}")
        Text("日變化：$${"%.2f".format(dayChange)}")
        Text("ETF 最近更新：$lastUpdatedText")
        Button(onClick = { load() }) { Text("重新整理 Dashboard") }
    }
}


@Composable
fun ExpensePage() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("飲食") }
    var note by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val expenses by produceState(initialValue = emptyList<Expense>(), reload) {
        value = withContext(Dispatchers.IO) { db.expenseDao().all() }
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金額") })
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分類") })
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("備註") })
        Button(onClick = {
            val num = amount.toDoubleOrNull() ?: return@Button
            scope.launch(Dispatchers.IO) {
                db.expenseDao().insert(Expense(date = LocalDate.now().toString(), amount = num, category = category, note = note))
                WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<BudgetAlertWorker>().build())
                withContext(Dispatchers.Main) {
                    amount = ""
                    note = ""
                    reload++
                }
            }
        }) { Text("新增支出") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(expenses) { e -> Text("${e.date} ${e.category} $${e.amount} ${e.note}") }
        }
    }
}

@Composable
fun EtfPage() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var holdings by remember { mutableStateOf(emptyList<EtfHolding>()) }
    var newSymbol by remember { mutableStateOf("") }
    var newShares by remember { mutableStateOf("") }
    var newCost by remember { mutableStateOf("") }
    val prefs = remember { Prefs(context) }
    var lastUpdatedText by remember { mutableStateOf("尚未更新") }
    var updateStatus by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }

    fun load() {
        scope.launch(Dispatchers.IO) {
            val data = db.etfHoldingDao().all()
            withContext(Dispatchers.Main) {
                holdings = data
                val ts = prefs.getLastUpdateEpochMs()
                lastUpdatedText = if (ts <= 0L) "尚未更新" else DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("新增/更新持倉", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = newSymbol, onValueChange = { newSymbol = it.uppercase() }, label = { Text("代碼（例：VTI / 0050）") })
        OutlinedTextField(value = newShares, onValueChange = { newShares = it }, label = { Text("股數") })
        OutlinedTextField(value = newCost, onValueChange = { newCost = it }, label = { Text("平均成本") })
        Button(onClick = {
            val symbol = normalizeEtfSymbol(newSymbol)
            val shares = newShares.toDoubleOrNull() ?: return@Button
            val avgCost = newCost.toDoubleOrNull() ?: return@Button
            if (symbol.isEmpty()) return@Button
            scope.launch(Dispatchers.IO) {
                db.etfHoldingDao().upsert(EtfHolding(symbol = symbol, shares = shares, avgCost = avgCost, lastPrice = avgCost))
                withContext(Dispatchers.Main) {
                    newSymbol = ""
                    newShares = ""
                    newCost = ""
                    load()
                }
            }
        }) { Text("儲存持倉") }

        Divider()
        Text("最近更新：$lastUpdatedText")

        Button(
            enabled = !isUpdating,
            onClick = {
                isUpdating = true
                updateStatus = "更新中..."
                scope.launch(Dispatchers.IO) {
                    val current = db.etfHoldingDao().all()
                    if (current.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            updateStatus = "尚無持倉可更新"
                        }
                        return@launch
                    }

                    val prices = try {
                        PriceRepository().fetchPrices(current.map { it.symbol })
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            updateStatus = "更新失敗：${e.message ?: e::class.java.simpleName}"
                        }
                        return@launch
                    }

                    if (prices.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            updateStatus = "找不到報價，請確認 ETF 代碼（例：VTI / 0050）"
                        }
                        return@launch
                    }

                    val updated = current.map { h -> h.copy(lastPrice = prices[h.symbol] ?: h.lastPrice) }
                    db.etfHoldingDao().upsertAll(updated)
                    db.assetSnapshotDao().insert(
                        AssetSnapshot(date = LocalDate.now().toString(), totalAsset = updated.sumOf { it.shares * it.lastPrice })
                    )
                    prefs.setLastUpdateEpochMs(System.currentTimeMillis())

                    withContext(Dispatchers.Main) {
                        isUpdating = false
                        updateStatus = "已更新 ${prices.size}/${current.size} 筆報價"
                        load()
                    }
                }
            }
        ) { Text(if (isUpdating) "更新中..." else "手動更新 ETF 報價") }
        if (updateStatus.isNotBlank()) {
            Text(updateStatus)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(holdings) { h ->
                val value = h.shares * h.lastPrice
                val pnl = (h.lastPrice - h.avgCost) * h.shares
                Text("${h.symbol} 股數:${h.shares} 成本:$${h.avgCost} 現價:$${h.lastPrice} 損益:$${"%.2f".format(pnl)} 市值:$${"%.2f".format(value)}")
            }
        }
    }
}


@Composable
fun ReportPage() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var categoryTotals by remember { mutableStateOf(emptyList<CategoryTotal>()) }
    var recentSnapshots by remember { mutableStateOf(emptyList<AssetSnapshot>()) }

    fun load() {
        scope.launch(Dispatchers.IO) {
            val ym = LocalDate.now().toString().substring(0, 7)
            val totals = db.expenseDao().totalsByCategoryInMonth(ym)
            val snapshots = db.assetSnapshotDao().latestN(7)
            withContext(Dispatchers.Main) {
                categoryTotals = totals
                recentSnapshots = snapshots
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("本月分類支出", style = MaterialTheme.typography.titleMedium)
        if (categoryTotals.isEmpty()) {
            Text("尚無資料")
        } else {
            categoryTotals.forEach { item ->
                Text("${item.category}: $${"%.2f".format(item.total)}")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("最近 7 筆資產快照", style = MaterialTheme.typography.titleMedium)
        if (recentSnapshots.isEmpty()) {
            Text("尚無資料")
        } else {
            recentSnapshots.forEach { row ->
                Text("${row.date}  總資產: $${"%.2f".format(row.totalAsset)}")
            }
        }

        Button(onClick = { load() }) { Text("重新整理") }
    }
}



@Composable
fun SettingsPage() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var budgetText by remember { mutableStateOf(if (prefs.getDailyBudget() == 0.0) "" else prefs.getDailyBudget().toString()) }
    var savedValue by remember { mutableDoubleStateOf(prefs.getDailyBudget()) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("每日預算設定", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = it },
            label = { Text("每日預算金額") }
        )
        Button(onClick = {
            val v = budgetText.toDoubleOrNull() ?: return@Button
            prefs.setDailyBudget(v)
            savedValue = v
        }) { Text("儲存設定") }

        Text("目前每日預算：$${"%.2f".format(savedValue)}")
    }
}

@Composable
fun SimplePage(title: String) { Box(Modifier.fillMaxSize().padding(16.dp)) { Text(title) } }
