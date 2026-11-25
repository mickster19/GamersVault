package com.mickster.gamersvault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class VaultEntry(
    val id: String,
    val title: String,
    val priceText: String,
    val badge: String,
    val desc: String,
    val images: List<String>
)

class MainActivity : AppCompatActivity() {

    private val dbUrl = "https://moby-42aca-default-rtdb.firebaseio.com"
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    private lateinit var rootLayout: FrameLayout
    private lateinit var mainColumn: LinearLayout
    private lateinit var bodyContainer: LinearLayout
    private lateinit var navButtons: Map<String, LinearLayout>
    private lateinit var viewerOverlay: FrameLayout
    private lateinit var viewerImage: ImageView

    private var activeTab = "home"
    private var accountsMode = "buy"
    private var signedInUser: String? = null

    private val campaigns = mutableListOf<VaultEntry>()
    private val games = mutableListOf<VaultEntry>()
    private val accounts = mutableListOf<VaultEntry>()
    private val mods = mutableListOf<VaultEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0F111A"))
        }

        mainColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Top Header (MICKSTER VAULT)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151824"))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            val full = "MICKSTER VAULT"
            val sp = SpannableString(full)
            sp.setSpan(ForegroundColorSpan(Color.parseColor("#F3F4F6")), 0, 8, 0)
            sp.setSpan(ForegroundColorSpan(Color.parseColor("#4A63F0")), 9, full.length, 0)
            text = sp
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val avatar = TextView(this).apply {
            text = "M"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4A63F0"))
            }
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            setOnClickListener { switchTab("profile") }
        }
        header.addView(logo)
        header.addView(avatar)
        mainColumn.addView(header)

        // Scrollable Content
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        bodyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }
        scroll.addView(bodyContainer)
        mainColumn.addView(scroll)

        // Bottom Navigation
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#151824"))
            setPadding(dp(4), dp(8), dp(4), dp(10))
        }
        val tabItems = listOf(
            "home" to "Home",
            "games" to "Games",
            "accounts" to "Accounts",
            "mods" to "Mods",
            "chat" to "Chat",
            "profile" to "Profile"
        )
        val map = mutableMapOf<String, LinearLayout>()
        tabItems.forEach { (key, label) ->
            val item = LinearLayout(this).apply {
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
            item.addView(tv)
            bottomNav.addView(item)
            map[key] = item
        }
        navButtons = map
        mainColumn.addView(bottomNav)
        rootLayout.addView(mainColumn)

        // Fullscreen Image Lightbox
        viewerOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F206070C"))
            visibility = View.GONE
            setOnClickListener { visibility = View.GONE }
        }
        viewerImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(dp(14), dp(48), dp(14), dp(48)) }
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#222638"))
            }
            layoutParams = FrameLayout.LayoutParams(dp(38), dp(38)).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dp(16), dp(16), 0)
            }
            setOnClickListener { viewerOverlay.visibility = View.GONE }
        }
        viewerOverlay.addView(viewerImage)
        viewerOverlay.addView(closeBtn)
        rootLayout.addView(viewerOverlay)

        setContentView(rootLayout)
        switchTab("home")
        loadCatalog()
    }

    private fun openImageLightbox(url: String) {
        if (url.isBlank()) return
        val cached = bitmapCache[url]
        if (cached != null) {
            viewerImage.setImageBitmap(cached)
            viewerOverlay.visibility = View.VISIBLE
        } else {
            loadBitmap(url) { bmp ->
                viewerImage.setImageBitmap(bmp)
                viewerOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun switchTab(tab: String) {
        activeTab = tab
        navButtons.forEach { (key, btn) ->
            val tv = btn.getChildAt(0) as TextView
            if (key == tab) {
                tv.setTextColor(Color.parseColor("#4A63F0"))
                btn.setBackgroundColor(Color.parseColor("#1E2235"))
            } else {
                tv.setTextColor(Color.parseColor("#9CA3AF"))
                btn.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        renderPage()
    }

    private fun renderPage() {
        bodyContainer.removeAllViews()
        when (activeTab) {
            "home" -> {
                addHSection("VAULT CAMPAIGNS", campaigns, dp(250), dp(165), "#2E3B8B")
                addHSection("NEW GAMES", games, dp(168), dp(175), "#151824")
                addHSection("EFOOTBALL ACCOUNTS", accounts, dp(168), dp(175), "#151824")
                addHSection("PREMIUM MODS", mods, dp(168), dp(175), "#151824")
            }
            "games" -> addGridLikeList("GAMES", games)
            "accounts" -> renderAccounts()
            "mods" -> addGridLikeList("MODS", mods)
            "chat" -> renderChat()
            "profile" -> renderProfile()
        }
    }

    private fun addHSection(title: String, list: List<VaultEntry>, w: Int, h: Int, bgHex: String) {
        bodyContainer.addView(sectionHeader(title))
        val hs = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(18), 0, dp(18), dp(8))
            clipToPadding = false
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (list.isEmpty()) {
            row.addView(buildEntryCard(VaultEntry("0", "Inapakia...", "", "", "Firebase", emptyList()), w, h, bgHex, true))
        } else {
            list.take(8).forEach { row.addView(buildEntryCard(it, w, h, bgHex, true)) }
        }
        hs.addView(row)
        bodyContainer.addView(hs)
    }

    private fun addGridLikeList(title: String, list: List<VaultEntry>) {
        bodyContainer.addView(sectionHeader(title))
        if (list.isEmpty()) {
            bodyContainer.addView(mutedLabel("Hakuna vitu kwa sasa."))
            return
        }
        list.forEach {
            bodyContainer.addView(buildEntryCard(it, LinearLayout.LayoutParams.MATCH_PARENT, dp(195), "#151824", false))
        }
    }

    private fun renderAccounts() {
        bodyContainer.addView(sectionHeader("EFOOTBALL ACCOUNTS"))
        val modeRow = LinearLayout(this).apply {
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
                    renderPage()
                }
            }
            modeRow.addView(b)
        }
        bodyContainer.addView(modeRow)

        when (accountsMode) {
            "buy" -> {
                if (accounts.isEmpty()) bodyContainer.addView(mutedLabel("Hakuna akaunti za kuuzwa."))
                accounts.forEach {
                    bodyContainer.addView(buildEntryCard(it, LinearLayout.LayoutParams.MATCH_PARENT, dp(195), "#151824", false))
                }
            }
            "exchange" -> {
                bodyContainer.addView(mutedLabel("Ada ya Exchange: Tsh 8,000 baada ya wote kuridhia."))
                bodyContainer.addView(buildEntryCard(
                    VaultEntry("xch1", "Badilishana Akaunti", "Tsh 8,000", "EXCHANGE", "Weka ofa au chagua ofa ya mwenzako.", emptyList()),
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(110), "#1E2235", false
                ))
            }
            "sell" -> {
                bodyContainer.addView(mutedLabel("Strength 3200+ · Hakuna deni la coins · Sio linked na Google Play."))
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(18), dp(6), dp(18), dp(14))
                }
                val priceInput = EditText(this).apply {
                    hint = "Bei unayotaka (mfano 45000)"
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
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                        topMargin = dp(10)
                    }
                    setOnClickListener {
                        if (priceInput.text.toString().trim().isEmpty()) {
                            Toast.makeText(this@MainActivity, "Weka bei kwanza", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Ombi limetumwa kwa admin", Toast.LENGTH_SHORT).show()
                            priceInput.setText("")
                        }
                    }
                }
                box.addView(priceInput)
                box.addView(btn)
                bodyContainer.addView(box)
            }
        }
    }

    private fun renderChat() {
        bodyContainer.addView(sectionHeader("CHAT"))
        bodyContainer.addView(mutedLabel("Inbox (DM) na Vikundi vya wachezaji."))
        bodyContainer.addView(buildEntryCard(
            VaultEntry("dm", "Inbox / DM", "BINAFSI", "NEW", "Tuma ujumbe binafsi kwa namba ya simu au jina.", emptyList()),
            LinearLayout.LayoutParams.MATCH_PARENT, dp(105), "#151824", false
        ))
        bodyContainer.addView(buildEntryCard(
            VaultEntry("grp", "Vikundi", "GROUPS", "HOT", "Jiunge au unda group ya wachezaji.", emptyList()),
            LinearLayout.LayoutParams.MATCH_PARENT, dp(105), "#151824", false
        ))
    }

    private fun renderProfile() {
        bodyContainer.addView(sectionHeader("PROFILE"))
        val who = signedInUser ?: "Guest"
        bodyContainer.addView(buildEntryCard(
            VaultEntry("prof", "Mtumiaji: $who", "MICKSTER VAULT", "VIP", "Native Kotlin Profile", emptyList()),
            LinearLayout.LayoutParams.MATCH_PARENT, dp(110), "#151824", false
        ))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(14))
        }
        val userInput = EditText(this).apply {
            hint = "Username au namba ya simu"
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E2235"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val btn = Button(this).apply {
            text = if (signedInUser == null) "Ingia" else "Toka"
            setAllCaps(false)
            setBackgroundColor(Color.parseColor("#4A63F0"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                topMargin = dp(10)
            }
            setOnClickListener {
                if (signedInUser == null) {
                    val v = userInput.text.toString().trim()
                    if (v.isEmpty()) Toast.makeText(this@MainActivity, "Weka username", Toast.LENGTH_SHORT).show()
                    else { signedInUser = v; renderPage() }
                } else {
                    signedInUser = null; renderPage()
                }
            }
        }
        if (signedInUser == null) box.addView(userInput)
        box.addView(btn)
        bodyContainer.addView(box)
    }

    private fun buildEntryCard(item: VaultEntry, widthPx: Int, heightPx: Int, bgHex: String, isHorizontal: Boolean): View {
        val frame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#222638"))
            }
            clipToOutline = true
            layoutParams = if (isHorizontal) {
                LinearLayout.LayoutParams(widthPx, heightPx).apply { marginEnd = dp(12) }
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx).apply {
                    setMargins(dp(18), dp(6), dp(18), dp(6))
                }
            }
        }

        val imgUrl = item.images.firstOrNull { it.startsWith("http") || it.startsWith("data:image") }
        if (imgUrl != null) {
            val iv = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frame.addView(iv)
            loadBitmap(imgUrl) { bmp -> iv.setImageBitmap(bmp) }
            iv.setOnClickListener { openImageLightbox(imgUrl) }
        }

        if (item.badge.isNotEmpty()) {
            val badgeTv = TextView(this).apply {
                text = item.badge.uppercase()
                setTextColor(Color.WHITE)
                textSize = 9f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(
                        when (item.badge.lowercase()) {
                            "hot", "sold" -> Color.parseColor("#EF4444")
                            "sale" -> Color.parseColor("#F59E0B")
                            else -> Color.parseColor("#4A63F0")
                        }
                    )
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    setMargins(dp(10), dp(10), 0, 0)
                }
            }
            frame.addView(badgeTv)
        }

        val foot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC0F111A"))
            setPadding(dp(12), dp(8), dp(12), dp(10))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            setOnClickListener {
                if (imgUrl != null) openImageLightbox(imgUrl)
                else Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
            }
        }
        val titleTv = TextView(this).apply {
            text = item.title
            setTextColor(Color.parseColor("#F3F4F6"))
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }
        val priceTv = TextView(this).apply {
            text = if (item.priceText.isNotEmpty()) item.priceText else item.desc
            setTextColor(if (item.priceText == "FREE") Color.parseColor("#11B981") else Color.parseColor("#4A63F0"))
            textSize = 11.5f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        }
        foot.addView(titleTv)
        foot.addView(priceTv)
        frame.addView(foot)

        return frame
    }

    private fun loadBitmap(urlStr: String, onReady: (Bitmap) -> Unit) {
        bitmapCache[urlStr]?.let {
            onReady(it)
            return
        }
        thread {
            try {
                val bmp = if (urlStr.startsWith("data:image")) {
                    val b64 = urlStr.substringAfter("base64,", "")
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                }
                if (bmp != null) {
                    bitmapCache[urlStr] = bmp
                    runOnUiThread { onReady(bmp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadCatalog() {
        thread {
            val cList = parseNode("mv_campaigns", false)
            val gList = parseNode("mv_games", true)
            val aList = parseNode("mv_accounts", false)
            val mList = parseNode("mv_mods", true)
            runOnUiThread {
                campaigns.clear(); campaigns.addAll(cList)
                games.clear(); games.addAll(gList)
                accounts.clear(); accounts.addAll(aList)
                mods.clear(); mods.addAll(mList)
                renderPage()
            }
        }
    }

    private fun parseNode(node: String, defaultFree: Boolean): List<VaultEntry> {
        return try {
            val conn = (URL("$dbUrl/$node.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val out = mutableListOf<VaultEntry>()
            val objRegex = Regex("\\{([^{}]*(?:\\[[^\\[\\]]*\\][^{}]*)*)\\}")
            objRegex.findAll(raw).forEachIndexed { idx, match ->
                val block = match.groupValues[1]
                val title = Regex("\"(?:name|title)\"\\s*:\\s*\"([^\"]+)\"").find(block)?.groupValues?.get(1)
                if (!title.isNullOrBlank()) {
                    val isFree = Regex("\"free\"\\s*:\\s*true").containsMatchIn(block) || defaultFree
                    val priceNum = Regex("\"price\"\\s*:\\s*([0-9]+)").find(block)?.groupValues?.get(1)?.toLongOrNull()
                    val priceText = when {
                        isFree && (priceNum == null || priceNum == 0L) -> "FREE"
                        priceNum != null && priceNum > 0L -> "Tsh ${String.format("%,d", priceNum)}"
                        else -> ""
                    }
                    val badge = Regex("\"badge\"\\s*:\\s*\"([^\"]+)\"").find(block)?.groupValues?.get(1) ?: ""
                    val desc = Regex("\"desc\"\\s*:\\s*\"([^\"]+)\"").find(block)?.groupValues?.get(1) ?: ""
                    val imgs = Regex("\"(?:icon|image)\"\\s*:\\s*\"([^\"]+)\"|\"https?://[^\"]+\"")
                        .findAll(block)
                        .map { it.groupValues[1].ifBlank { it.value.trim('"') } }
                        .filter { it.startsWith("http") || it.startsWith("data:image") }
                        .distinct()
                        .toList()
                    out.add(VaultEntry("$node-$idx", title, priceText, badge, desc, imgs))
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun sectionHeader(t: String): TextView = TextView(this).apply {
        text = t
        setTextColor(Color.parseColor("#F3F4F6"))
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(18), dp(14), dp(18), dp(8))
    }

    private fun mutedLabel(t: String): TextView = TextView(this).apply {
        text = t
        setTextColor(Color.parseColor("#9CA3AF"))
        textSize = 12.5f
        setPadding(dp(18), dp(4), dp(18), dp(10))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
