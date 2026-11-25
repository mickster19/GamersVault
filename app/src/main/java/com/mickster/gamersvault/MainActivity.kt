package com.mickster.gamersvault

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class VaultItem(val title: String, val price: String, val desc: String)

class MainActivity : AppCompatActivity() {

    private val dbUrl = "https://moby-42aca-default-rtdb.firebaseio.com"
    private lateinit var bodyContainer: LinearLayout
    private lateinit var navButtons: Map<String, LinearLayout>
    private var activeTab = "home"

    private val campaigns = mutableListOf<VaultItem>()
    private val games = mutableListOf<VaultItem>()
    private val accounts = mutableListOf<VaultItem>()
    private val mods = mutableListOf<VaultItem>()
    private var accountsMode = "buy"
    private var signedInUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F111A"))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151824"))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            gravity = Gravity.CENTER_VERTICAL
        }
        val logoText = TextView(this).apply {
            val full = "MICKSTER VAULT"
            val sp = SpannableString(full)
            sp.setSpan(ForegroundColorSpan(Color.parseColor("#F3F4F6")), 0, 8, 0)
            sp.setSpan(ForegroundColorSpan(Color.parseColor("#4A63F0")), 9, full.length, 0)
            text = sp
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }
        header.addView(logoText)
        root.addView(header)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(24))
        }
        scroll.addView(bodyContainer)
        root.addView(scroll)

        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151824"))
            setPadding(dp(4), dp(8), dp(4), dp(10))
        }
        val tabs = listOf(
            "home" to "Home",
            "games" to "Games",
            "accounts" to "Accounts",
            "mods" to "Mods",
            "chat" to "Chat",
            "profile" to "Profile"
        )
        val btnMap = mutableMapOf<String, LinearLayout>()
        tabs.forEach { (key, label) ->
            val tabBtn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(2), dp(6), dp(2), dp(6))
                setOnClickListener { switchTab(key) }
            }
            val tv = TextView(this).apply {
                text = label
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
            }
            tabBtn.addView(tv)
            bottomNav.addView(tabBtn)
            btnMap[key] = tabBtn
        }
        navButtons = btnMap
        root.addView(bottomNav)

        setContentView(root)
        switchTab("home")
        fetchAllCatalog()
    }

    private fun switchTab(tab: String) {
        activeTab = tab
        navButtons.forEach { (k, btn) ->
            val tv = btn.getChildAt(0) as TextView
            if (k == tab) {
                tv.setTextColor(Color.parseColor("#4A63F0"))
                btn.setBackgroundColor(Color.parseColor("#1E2235"))
            } else {
                tv.setTextColor(Color.parseColor("#9CA3AF"))
                btn.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        renderActiveTab()
    }

    private fun renderActiveTab() {
        bodyContainer.removeAllViews()
        when (activeTab) {
            "home" -> renderHome()
            "games" -> renderListPage("GAMES", games, "")
            "accounts" -> renderAccountsPage()
            "mods" -> renderListPage("MODS", mods, "")
            "chat" -> renderChatPage()
            "profile" -> renderProfilePage()
        }
    }

    private fun renderHome() {
        addHorizontalSection("VAULT CAMPAIGNS", campaigns, "#2E3B8B")
        addHorizontalSection("NEW GAMES", games, "#151824")
        addHorizontalSection("EFOOTBALL ACCOUNTS", accounts, "#151824")
        addHorizontalSection("PREMIUM MODS", mods, "#151824")
    }

    private fun addHorizontalSection(title: String, items: List<VaultItem>, bgHex: String) {
        bodyContainer.addView(sectionTitle(title))
        val hScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(18), 0, dp(18), dp(8))
            clipToPadding = false
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (items.isEmpty()) {
            row.addView(makeCard("Inapakia...", "Firebase", bgHex, dp(180)))
        } else {
            items.take(8).forEach { row.addView(makeCard(it.title, it.price, bgHex, dp(180))) }
        }
        hScroll.addView(row)
        bodyContainer.addView(hScroll)
    }

    private fun renderListPage(title: String, items: List<VaultItem>, subtitleNote: String) {
        bodyContainer.addView(sectionTitle(title))
        if (subtitleNote.isNotEmpty()) {
            bodyContainer.addView(mutedText(subtitleNote))
        }
        if (items.isEmpty()) {
            bodyContainer.addView(mutedText("Hakuna bidhaa kwa sasa."))
            return
        }
        items.forEach {
            bodyContainer.addView(makeCard(it.title, "${it.price} · ${it.desc}", "#151824", LinearLayout.LayoutParams.MATCH_PARENT))
        }
    }

    private fun renderAccountsPage() {
        bodyContainer.addView(sectionTitle("EFOOTBALL ACCOUNTS"))
        val modeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(4), dp(18), dp(10))
        }
        listOf("buy" to "Nunua", "exchange" to "Exchange", "sell" to "Uza").forEach { (m, label) ->
            val b = Button(this).apply {
                text = label
                textSize = 12f
                setAllCaps(false)
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) }
                setBackgroundColor(if (accountsMode == m) Color.parseColor("#4A63F0") else Color.parseColor("#1E2235"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    accountsMode = m
                    renderActiveTab()
                }
            }
            modeBar.addView(b)
        }
        bodyContainer.addView(modeBar)

        when (accountsMode) {
            "buy" -> {
                if (accounts.isEmpty()) bodyContainer.addView(mutedText("Hakuna akaunti za kuuzwa sasa."))
                accounts.forEach {
                    bodyContainer.addView(makeCard(it.title, "${it.price} · ${it.desc}", "#151824", LinearLayout.LayoutParams.MATCH_PARENT))
                }
            }
            "exchange" -> {
                bodyContainer.addView(mutedText("Ada ya usimamizi wa Exchange ni Tsh 8,000 baada ya pande zote kuridhia."))
                bodyContainer.addView(makeCard("Ofa za Exchange", "Chagua akaunti au weka ofa yako ya kubadilishana.", "#1E2235", LinearLayout.LayoutParams.MATCH_PARENT))
            }
            "sell" -> {
                bodyContainer.addView(mutedText("Mahitaji: Strength 3200+, hakuna deni la coins, haijaunganishwa na Google Play."))
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(6), dp(18), dp(12))
                }
                val input = EditText(this).apply {
                    hint = "Weka bei unayotaka (mfano 45000)"
                    setHintTextColor(Color.parseColor("#9CA3AF"))
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#1E2235"))
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                val btn = Button(this).apply {
                    text = "Tuma Ombi la Kuuza"
                    setAllCaps(false)
                    setBackgroundColor(Color.parseColor("#4A63F0"))
                    setTextColor(Color.WHITE)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                    lp.topMargin = dp(10)
                    layoutParams = lp
                    setOnClickListener {
                        if (input.text.toString().trim().isEmpty()) {
                            Toast.makeText(this@MainActivity, "Weka bei kwanza", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Ombi limepokelewa kwa ukaguzi", Toast.LENGTH_SHORT).show()
                            input.setText("")
                        }
                    }
                }
                box.addView(input)
                box.addView(btn)
                bodyContainer.addView(box)
            }
        }
    }

    private fun renderChatPage() {
        bodyContainer.addView(sectionTitle("CHAT (INBOX & VIKUNDI)"))
        bodyContainer.addView(mutedText("Ujumbe binafsi (DM) na vikundi vya wanachama."))
        bodyContainer.addView(makeCard("Inbox / DM", "Anza mazungumzo kwa namba ya simu au jina.", "#151824", LinearLayout.LayoutParams.MATCH_PARENT))
        bodyContainer.addView(makeCard("Vikundi", "Unda au jiunge na group ya wachezaji.", "#151824", LinearLayout.LayoutParams.MATCH_PARENT))
    }

    private fun renderProfilePage() {
        bodyContainer.addView(sectionTitle("PROFILE"))
        val status = signedInUser ?: "Hajaingia (Guest)"
        bodyContainer.addView(makeCard("Akaunti: $status", "MICKSTER VAULT · Native Kotlin", "#151824", LinearLayout.LayoutParams.MATCH_PARENT))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(12))
        }
        val userInput = EditText(this).apply {
            hint = "Username au namba ya simu"
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E2235"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val btn = Button(this).apply {
            text = if (signedInUser == null) "Ingia kwenye Akaunti" else "Toka (Logout)"
            setAllCaps(false)
            setBackgroundColor(Color.parseColor("#4A63F0"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener {
                if (signedInUser == null) {
                    val u = userInput.text.toString().trim()
                    if (u.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Weka username kwanza", Toast.LENGTH_SHORT).show()
                    } else {
                        signedInUser = u
                        renderActiveTab()
                    }
                } else {
                    signedInUser = null
                    renderActiveTab()
                }
            }
        }
        if (signedInUser == null) box.addView(userInput)
        box.addView(btn)
        bodyContainer.addView(box)
    }

    private fun fetchAllCatalog() {
        thread {
            val loadedCampaigns = fetchNode("mv_campaigns", "Tsh 0", "Campaign")
            val loadedGames = fetchNode("mv_games", "FREE", "Game")
            val loadedAccounts = fetchNode("mv_accounts", "Tsh 0", "Account")
            val loadedMods = fetchNode("mv_mods", "FREE", "Mod")
            runOnUiThread {
                campaigns.clear(); campaigns.addAll(loadedCampaigns)
                games.clear(); games.addAll(loadedGames)
                accounts.clear(); accounts.addAll(loadedAccounts)
                mods.clear(); mods.addAll(loadedMods)
                if (activeTab == "home" || activeTab == "games" || activeTab == "accounts" || activeTab == "mods") {
                    renderActiveTab()
                }
            }
        }
    }

    private fun fetchNode(node: String, defaultPrice: String, fallbackDesc: String): List<VaultItem> {
        return try {
            val conn = (URL("$dbUrl/$node.json").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val nameRegex = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
            val titleRegex = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"")
            val priceRegex = Regex("\"price\"\\s*:\\s*([0-9]+)")
            val titles = nameRegex.findAll(json).map { it.groupValues[1] }.toList().ifEmpty {
                titleRegex.findAll(json).map { it.groupValues[1] }.toList()
            }
            val prices = priceRegex.findAll(json).map { "Tsh ${it.groupValues[1]}" }.toList()
            titles.take(12).mapIndexed { i, t ->
                VaultItem(t, if (i < prices.size) prices[i] else defaultPrice, fallbackDesc)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#F3F4F6"))
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(18), dp(14), dp(18), dp(8))
    }

    private fun mutedText(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#9CA3AF"))
        textSize = 13f
        setPadding(dp(18), dp(4), dp(18), dp(10))
    }

    private fun makeCard(title: String, subtitle: String, bgHex: String, widthPx: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#222638"))
            }
            background = bg
            setPadding(dp(14), dp(14), dp(14), dp(14))
            val lp = if (widthPx == LinearLayout.LayoutParams.MATCH_PARENT) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(18), dp(6), dp(18), dp(6))
                }
            } else {
                LinearLayout.LayoutParams(widthPx, dp(115)).apply { marginEnd = dp(12) }
            }
            layoutParams = lp
            gravity = Gravity.BOTTOM
        }
        val tView = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#F3F4F6"))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
        }
        val sView = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#4A63F0"))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        }
        card.addView(tView)
        card.addView(sView)
        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
