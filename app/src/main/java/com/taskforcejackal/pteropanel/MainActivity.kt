
package com.taskforcejackal.pteropanel

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences

    private val bg = Color.rgb(17, 24, 39)
    private val panel = Color.rgb(31, 41, 55)
    private val accent = Color.rgb(59, 130, 246)
    private val danger = Color.rgb(185, 28, 28)
    private val text = Color.WHITE
    private val muted = Color.rgb(209, 213, 219)

    private val refreshHandler = Handler(Looper.getMainLooper())
    private var dashboardVisible = false

    private var lastServerListLoad = 0L
    private val serverListCooldownMs = 30_000L

    private var lastCardRefresh = 0L
    private val cardRefreshCooldownMs = 30_000L

    private val wsClient = OkHttpClient()
    private val liveSockets = mutableMapOf<String, WebSocket>()

    private val serverCards = mutableListOf<ServerCardRef>()


    data class ServerCardRef(
        val identifier: String,
        val statusView: TextView,
        val startBtn: Button,
        val stopBtn: Button,
        val restartBtn: Button
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("tfj_app", Context.MODE_PRIVATE)
        showHome()
    }

    private fun baseLayout(): LinearLayout {
        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = true
        scroll.isScrollbarFadingEnabled = false
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(bg)
        root.setPadding(24, 24, 24, 24)

        scroll.addView(root)

        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = insets.getInsets(WindowInsets.Type.statusBars()).top
            val bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            root.setPadding(24, topInset + 24, 24, bottomInset + 24)
            insets
        }
        setContentView(scroll)
        return root
    }

    private fun title(value: String): TextView {
        val tv = TextView(this)
        tv.text = value
        tv.setTextColor(text)
        tv.textSize = 26f
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setPadding(0, 0, 0, 20)
        return tv
    }

    private fun body(value: String): TextView {
        val tv = TextView(this)
        tv.text = value
        tv.setTextColor(text)
        tv.textSize = 15f
        tv.setPadding(0, 0, 0, 16)
        return tv
    }

    private fun input(hint: String, password: Boolean = false): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setHintTextColor(Color.rgb(156, 163, 175))
        e.setTextColor(text)
        e.setSingleLine(true)
        e.setPadding(18, 14, 18, 14)
        e.setBackgroundColor(panel)
        if (password) e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        return e
    }

    private fun button(label: String, color: Int = accent, action: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.setBackgroundColor(color)
        b.setOnClickListener { action() }
        return b
    }

    private fun setButtonState(btn: Button, enabled: Boolean) {
        btn.isEnabled = enabled
        btn.alpha = if (enabled) 1.0f else 0.4f
    }

    private fun spacer(root: LinearLayout, height: Int = 14) {
        val v = Space(this)
        root.addView(v, LinearLayout.LayoutParams(1, height))
    }

    private fun showHome() {
        val root = baseLayout()
        root.addView(title("Task Force Jackal"))
        root.addView(body("Generic TFJ app area. This can later hold announcements, links, joining info, SOPs, or ops updates."))

        spacer(root)
        root.addView(button("Open Server Panel") {
            if (prefs.getBoolean("app_logged_in", false)) showPanelHome() else showLogin()
        })

        spacer(root)
        root.addView(body("Panel access requires a TFJ app login first. Server access then requires a personal Pterodactyl Client API key."))
    }

    private fun showLogin() {
        val root = baseLayout()
        root.addView(title("TFJ Panel Login"))

        val username = input("Username")
        val password = input("Password", true)

        root.addView(username)
        spacer(root)
        root.addView(password)
        spacer(root)

        root.addView(button("Login") {
            val user = username.text.toString().trim().lowercase()
            val pass = password.text.toString()

            // TEMPORARY LOCAL TEST LOGIN.
            // Replace this with real backend auth before sharing the APK.
            val allowed = (user == "daniel" && pass == "change_me") || (user == "admin" && pass == "change_me")

            if (allowed) {
                prefs.edit()
                    .putBoolean("app_logged_in", true)
                    .putString("app_user", user)
                    .apply()
                showPanelHome()
            } else {
                toast("Login failed")
            }
        })

        root.addView(button("Back", panel) { showHome() })
        spacer(root)
        root.addView(body("Temporary test users: daniel / change_me, admin / change_me. This is only for testing the flow."))
    }

    private fun showPanelHome() {
        val root = baseLayout()
        root.addView(title("Server Panel"))
        root.addView(body("Logged in as: ${prefs.getString("app_user", "unknown")}"))

        root.addView(button("Server Dashboard") { showDashboard() })
        spacer(root)
        root.addView(button("API Settings") { showSettings() })
        spacer(root)
        root.addView(button("Logout", danger) {
            prefs.edit().putBoolean("app_logged_in", false).remove("app_user").apply()
            showHome()
        })
        spacer(root)
        root.addView(button("Back to Home", panel) { showHome() })
    }

    private fun showSettings() {
        val root = baseLayout()
        root.addView(title("API Settings"))

        val panelUrl = input("Panel URL")
        panelUrl.setText(prefs.getString("panel_url", "https://panel.taskforcejackal.com"))

        val apiKey = input("Pterodactyl Client API Key", true)
        apiKey.setText(prefs.getString("api_key", ""))

        root.addView(body("Each user should paste their own Pterodactyl Client API key. Do not share one master key."))
        root.addView(panelUrl)
        spacer(root)
        root.addView(apiKey)
        spacer(root)

        root.addView(button("Save Settings") {
            prefs.edit()
                .putString("panel_url", panelUrl.text.toString().trim().trimEnd('/'))
                .putString("api_key", apiKey.text.toString().trim())
                .apply()
            toast("Saved")
        })

        root.addView(button("Test Connection") {
            prefs.edit()
                .putString("panel_url", panelUrl.text.toString().trim().trimEnd('/'))
                .putString("api_key", apiKey.text.toString().trim())
                .apply()
            testConnection()
        })

        root.addView(button("Back", panel) { showPanelHome() })
    }

    private fun refreshServerCards(force: Boolean = false) {
        val now = System.currentTimeMillis()

        if (!force && now - lastCardRefresh < cardRefreshCooldownMs) {
            return
        }

        lastCardRefresh = now

        for (card in serverCards) {
            loadServerResources(
                card.identifier,
                card.statusView,
                card.startBtn,
                card.stopBtn,
                card.restartBtn
            )
        }
    }

    private fun stopAutoRefresh() {
        dashboardVisible = false

        for (socket in liveSockets.values) {
            socket.close(1000, "Leaving screen")
        }

        liveSockets.clear()
    }

    private fun showDashboard() {
        dashboardVisible = true

        val root = baseLayout()
        root.addView(title("Server Dashboard"))
        root.addView(body("Servers shown are based on the logged-in user's Pterodactyl Client API key."))

        root.addView(button("Refresh Servers") {
            val now = System.currentTimeMillis()

            if (now - lastServerListLoad < serverListCooldownMs) {
                toast("Please wait before refreshing servers again")
                return@button
            }

            lastServerListLoad = now

            stopAutoRefresh()
            showDashboard()
        })
        spacer(root)
        root.addView(button("Settings", panel) {
            stopAutoRefresh()
            showSettings()
        })

        root.addView(button("Back", panel) {
            stopAutoRefresh()
            showPanelHome()
        })

        spacer(root)
        loadServers(root)
    }

    private fun showConsole(serverName: String, identifier: String) {
        stopAutoRefresh()

        val root = baseLayout()
        root.addView(title("Console"))
        root.addView(body(serverName))

        val scroll = ScrollView(this)
        var autoScroll = true

        scroll.setOnTouchListener { _, _ ->
            autoScroll = false
            false
        }

        val consoleText = TextView(this)
        consoleText.setTextColor(Color.rgb(34, 197, 94))
        consoleText.textSize = 12f
        consoleText.typeface = Typeface.MONOSPACE
        consoleText.text = "Connecting to console...\n"

        scroll.addView(consoleText)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val commandInput = input("Enter command...")
        root.addView(commandInput)

        root.addView(button("Send Command") {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                sendConsoleCommand(identifier, cmd)
                commandInput.setText("")
            }
        })

        root.addView(button("Resume Auto Scroll", panel) {
            autoScroll = true
            consoleText.post {
                scroll.fullScroll(View.FOCUS_DOWN)
            }
        })

        root.addView(button("Back", panel) {
            showDashboard()
        })

        connectConsoleSocket(identifier, consoleText, scroll) { autoScroll }
    }


    private fun loadServers(root: LinearLayout) {
        for (socket in liveSockets.values) {
            socket.close(1000, "Reloading servers")
        }

        liveSockets.clear()
        serverCards.clear()
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        if (url.isBlank() || key.isBlank()) {
            toast("Set Panel URL and API key first")
            return
        }

        val loading = body("Loading servers...")
        root.addView(loading)

        Thread {
            try {
                val result = apiGet("$url/api/client", key)
                val json = JSONObject(result)
                val data = json.getJSONArray("data")

                runOnUiThread {
                    root.removeView(loading)

                    if (data.length() == 0) {
                        root.addView(body("No servers available for this API key."))
                        return@runOnUiThread
                    }

                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val attr = item.getJSONObject("attributes")
                        val identifier = attr.getString("identifier")
                        val name = attr.getString("name")
                        val suspended = attr.optBoolean("is_suspended", false)

                        root.addView(serverCard(name, identifier, suspended))
                        spacer(root, 10)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    root.removeView(loading)
                    root.addView(body("Failed to load servers: ${e.message}"))
                }
            }
        }.start()
    }

    private fun serverCard(name: String, identifier: String, suspended: Boolean): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundColor(panel)
        card.setPadding(20, 18, 20, 18)

        val nameView = TextView(this)
        nameView.text = name
        nameView.setTextColor(text)
        nameView.textSize = 18f
        nameView.typeface = Typeface.DEFAULT_BOLD
        card.addView(nameView)

        val idView = TextView(this)
        idView.text = "ID: $identifier" + if (suspended) " | SUSPENDED" else ""
        idView.setTextColor(muted)
        idView.textSize = 13f
        idView.setPadding(0, 4, 0, 8)
        card.addView(idView)

        val statusView = TextView(this)
        statusView.text = "Loading status..."
        statusView.setTextColor(muted)
        statusView.textSize = 14f
        statusView.setPadding(0, 0, 0, 12)
        card.addView(statusView)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL

        val startBtn = button("Start") { confirmPower(name, identifier, "start") }
        val stopBtn = button("Stop", danger) { confirmPower(name, identifier, "stop") }
        val restartBtn = button("Restart") { confirmPower(name, identifier, "restart") }

        fun addBtn(btn: Button) {
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(6, 0, 6, 0)
            row.addView(btn, params)
        }

        addBtn(startBtn)
        addBtn(stopBtn)
        addBtn(restartBtn)

        card.addView(row)

        spacer(card, 8)
        card.addView(button("Console", panel) {
            showConsole(name, identifier)
        })

        val cardRef = ServerCardRef(identifier, statusView, startBtn, stopBtn, restartBtn)
        serverCards.add(cardRef)

        loadServerResources(identifier, statusView, startBtn, stopBtn, restartBtn)

        //connectLiveServer(cardRef)

        return card
    }

    private fun loadServerResources(
        identifier: String,
        statusView: TextView,
        startBtn: Button,
        stopBtn: Button,
        restartBtn: Button
    ) {
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        Thread {
            try {
                val result = apiGet("$url/api/client/servers/$identifier/resources", key)
                val json = JSONObject(result)
                val attr = json.getJSONObject("attributes")

                val state = attr.getString("current_state").uppercase()
                val resources = attr.getJSONObject("resources")

                val cpu = resources.optDouble("cpu_absolute", 0.0)
                val memoryBytes = resources.optLong("memory_bytes", 0)
                val diskBytes = resources.optLong("disk_bytes", 0)

                val memoryGb = memoryBytes / 1024.0 / 1024.0 / 1024.0
                val diskGb = diskBytes / 1024.0 / 1024.0 / 1024.0

                runOnUiThread {

                    statusView.text =
                        "$state | CPU: %.1f%% | RAM: %.2f GB | Disk: %.2f GB"
                            .format(cpu, memoryGb, diskGb)

                    val statusColor = when (state) {
                        "RUNNING" -> Color.rgb(34, 197, 94)
                        "OFFLINE" -> Color.rgb(239, 68, 68)
                        "STARTING" -> Color.rgb(234, 179, 8)
                        "STOPPING" -> Color.rgb(234, 179, 8)
                        else -> muted
                    }

                    statusView.setTextColor(statusColor)

                    when (state) {
                        "RUNNING" -> {
                            setButtonState(startBtn, false)
                            setButtonState(stopBtn, true)
                            setButtonState(restartBtn, true)
                        }

                        "OFFLINE" -> {
                            setButtonState(startBtn, true)
                            setButtonState(stopBtn, false)
                            setButtonState(restartBtn, false)
                        }

                        else -> {
                            setButtonState(startBtn, false)
                            setButtonState(stopBtn, false)
                            setButtonState(restartBtn, false)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusView.text = "Status unavailable"
                    statusView.setTextColor(muted)
                }
            }
        }.start()
    }

    private fun connectLiveServer(card: ServerCardRef) {
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        Thread {
            try {
                val result = apiGet("$url/api/client/servers/${card.identifier}/websocket", key)
                val json = JSONObject(result).getJSONObject("data")

                val token = json.getString("token")
                val socketUrl = json.getString("socket")

                val request = Request.Builder()
                    .url(socketUrl)
                    .addHeader("Origin", url)
                    .build()

                val socket = wsClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val authPayload = JSONObject()
                        authPayload.put("event", "auth")
                        authPayload.put("args", org.json.JSONArray().put(token))
                        webSocket.send(authPayload.toString())

                        val logPayload = JSONObject()
                        logPayload.put("event", "send logs")
                        logPayload.put("args", emptyList<Any>())
                        webSocket.send(logPayload.toString())

                        val statsPayload = JSONObject()
                        statsPayload.put("event", "send stats")
                        statsPayload.put("args", org.json.JSONArray())
                        webSocket.send(statsPayload.toString())
                    }

                    override fun onMessage(webSocket: WebSocket, message: String) {
                        android.util.Log.d("WS", message)
                        handleLiveMessage(card, message)
                        runOnUiThread {
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        runOnUiThread {
                            card.statusView.text = "Live update failed: ${t.message}"
                            card.statusView.setTextColor(Color.RED)
                        }
                    }
                })

                liveSockets[card.identifier] = socket

            } catch (e: Exception) {
                runOnUiThread {
                    card.statusView.text = "Live update unavailable"
                    card.statusView.setTextColor(muted)
                }
            }
        }.start()
    }

    private fun sendConsoleCommand(identifier: String, command: String) {
        val socket = liveSockets["console_$identifier"] ?: return

        val payload = JSONObject()
        payload.put("event", "send command")
        payload.put("args", org.json.JSONArray().put(command))

        socket.send(payload.toString())
    }

    private fun connectConsoleSocket(
        identifier: String,
        consoleText: TextView,
        scroll: ScrollView,
        shouldAutoScroll: () -> Boolean
    ) {
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        Thread {
            try {
                val result = apiGet("$url/api/client/servers/$identifier/websocket", key)
                val json = JSONObject(result).getJSONObject("data")

                val token = json.getString("token")
                val socketUrl = json.getString("socket")

                val request = Request.Builder()
                    .url(socketUrl)
                    .addHeader("Origin", url)
                    .build()

                val socket = wsClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val authPayload = JSONObject()
                        authPayload.put("event", "auth")
                        authPayload.put("args", org.json.JSONArray().put(token))
                        webSocket.send(authPayload.toString())

                        runOnUiThread {
                            consoleText.text = "Console connected. Authenticating...\n"
                            consoleText.post {
                                scroll.fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, message: String) {
                        try {
                            val jsonMsg = JSONObject(message)
                            val event = jsonMsg.optString("event")
                            val args = jsonMsg.optJSONArray("args")

                            if (event == "auth success") {
                                val logPayload = JSONObject()
                                logPayload.put("event", "send logs")
                                logPayload.put("args", org.json.JSONArray())
                                webSocket.send(logPayload.toString())

                                runOnUiThread {
                                    consoleText.append("Authenticated. Requesting logs...\n")

                                    if (shouldAutoScroll()) {
                                        consoleText.post {
                                            scroll.fullScroll(View.FOCUS_DOWN)
                                        }
                                    }
                                }
                                return
                            }

                            if (event == "console output" && args != null && args.length() > 0) {
                                val line = args.getString(0)

                                runOnUiThread {
                                    consoleText.append(line + "\n")
                                    if (shouldAutoScroll()) {
                                        consoleText.post {
                                            scroll.fullScroll(View.FOCUS_DOWN)
                                        }
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            runOnUiThread {
                                consoleText.append("\nFailed to parse message: ${e.message}\n")
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        runOnUiThread {
                            consoleText.append("\nConsole failed: ${t.message}\n")
                        }
                    }
                })

                liveSockets["console_$identifier"] = socket

            } catch (e: Exception) {
                runOnUiThread {
                    consoleText.append("\nConsole unavailable: ${e.message}\n")
                }
            }
        }.start()
    }

    private fun handleLiveMessage(card: ServerCardRef, message: String) {
        try {
            val json = JSONObject(message)
            val event = json.getString("event")
            val args = json.getJSONArray("args")

            when (event) {
                "status" -> {
                    val state = args.getString(0).uppercase()

                    runOnUiThread {
                        updateCardStateOnly(card, state)
                    }
                }

                "stats" -> {
                    val stats = JSONObject(args.getString(0))

                    val state = stats.optString("state", "unknown").uppercase()
                    val cpu = stats.optDouble("cpu_absolute", 0.0)
                    val memoryBytes = stats.optLong("memory_bytes", 0)
                    val diskBytes = stats.optLong("disk_bytes", 0)

                    val memoryGb = memoryBytes / 1024.0 / 1024.0 / 1024.0
                    val diskGb = diskBytes / 1024.0 / 1024.0 / 1024.0

                    runOnUiThread {
                        card.statusView.text =
                            "$state | CPU: %.1f%% | RAM: %.2f GB | Disk: %.2f GB"
                                .format(cpu, memoryGb, diskGb)

                        updateCardStateOnly(card, state)
                    }
                }

                "token expiring", "token expired", "jwt error" -> {
                    runOnUiThread {
                        card.statusView.text = "Live token expired - reconnecting..."
                        card.statusView.setTextColor(muted)
                    }

                    liveSockets[card.identifier]?.close(1000, "Refreshing token")
                    liveSockets.remove(card.identifier)

                    connectLiveServer(card)
                }
            }
        } catch (_: Exception) {
            // Ignore malformed or unsupported websocket messages for now.
        }
    }

    private fun updateCardStateOnly(card: ServerCardRef, state: String) {
        val statusColor = when (state) {
            "RUNNING" -> Color.rgb(34, 197, 94)
            "OFFLINE" -> Color.rgb(239, 68, 68)
            "STARTING" -> Color.rgb(234, 179, 8)
            "STOPPING" -> Color.rgb(234, 179, 8)
            else -> muted
        }

        card.statusView.setTextColor(statusColor)

        when (state) {
            "RUNNING" -> {
                setButtonState(card.startBtn, false)
                setButtonState(card.stopBtn, true)
                setButtonState(card.restartBtn, true)
            }

            "OFFLINE" -> {
                setButtonState(card.startBtn, true)
                setButtonState(card.stopBtn, false)
                setButtonState(card.restartBtn, false)
            }

            else -> {
                setButtonState(card.startBtn, false)
                setButtonState(card.stopBtn, false)
                setButtonState(card.restartBtn, false)
            }
        }
    }

    private fun confirmPower(serverName: String, identifier: String, signal: String) {
        val title = when (signal) {
            "start" -> "Start server?"
            "stop" -> "Stop server?"
            "restart" -> "Restart server?"
            else -> "Confirm action?"
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Are you sure you want to $signal $serverName?")
            .setPositiveButton("Yes") { _, _ ->
                power(identifier, signal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun power(identifier: String, signal: String) {
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        Thread {
            try {
                val body = """{"signal":"$signal"}"""
                apiPost("$url/api/client/servers/$identifier/power", key, body)
                runOnUiThread {
                    toast("$signal sent")

                    refreshHandler.postDelayed({
                        refreshServerCards(force = true)
                    }, 5000)
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Power action failed: ${e.message}") }
            }
        }.start()

        runOnUiThread {
            for (card in serverCards) {
                if (card.identifier == identifier) {
                    card.statusView.text = "Sending $signal..."
                }
            }
        }
    }

    private fun testConnection() {
        val url = prefs.getString("panel_url", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""

        if (url.isBlank() || key.isBlank()) {
            toast("Panel URL/API key missing")
            return
        }

        Thread {
            try {
                apiGet("$url/api/client", key)
                runOnUiThread { toast("Connection successful") }
            } catch (e: Exception) {
                runOnUiThread { toast("Connection failed: ${e.message}") }
            }
        }.start()
    }

    private fun apiGet(endpoint: String, apiKey: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        addHeaders(conn, apiKey)
        return readResponse(conn)
    }

    private fun apiPost(endpoint: String, apiKey: String, jsonBody: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        addHeaders(conn, apiKey)
        OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }
        return readResponse(conn)
    }

    private fun addHeaders(conn: HttpURLConnection, apiKey: String) {
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Content-Type", "application/json")
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        if (code !in 200..299) throw Exception("HTTP $code: $text")
        return text
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        stopAutoRefresh()
        showHome()
    }
}
