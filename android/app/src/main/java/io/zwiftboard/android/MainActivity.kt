package io.zwiftboard.android

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var leftStatus: TextView
    private lateinit var rightStatus: TextView
    private lateinit var linkStatus: TextView
    private lateinit var leftUnlockStatus: TextView
    private lateinit var rightUnlockStatus: TextView
    private lateinit var lastPressStatus: TextView
    private lateinit var modeButton: Button
    private lateinit var instruction: TextView
    private lateinit var signInButton: Button
    private val unlockClient by lazy { ZwiftUnlockClient(applicationContext) }

    private val refresh = object : Runnable {
        override fun run() {
            renderStatus()
            statusHandler.postDelayed(this, 1000)
        }
    }
    private val statusHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val bluetoothGranted = requiredBluetoothPermissions().all {
                grants[it] == true || checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
            if (bluetoothGranted) {
                startBridgeService()
            } else {
                instruction.text = getString(R.string.permission_required)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
        renderStatus()
        updateSignInButton()
    }

    override fun onResume() {
        super.onResume()
        statusHandler.removeCallbacks(refresh)
        statusHandler.post(refresh)
        updateSignInButton()
    }

    override fun onPause() {
        statusHandler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun createContent(): android.view.View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
        }
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            gravity = Gravity.CENTER
        }
        instruction = TextView(this).apply {
            textSize = 15f
            text = getString(R.string.instructions)
            setPadding(0, 16, 0, 20)
        }
        leftStatus = makeStatus(getString(R.string.left_pod))
        rightStatus = makeStatus(getString(R.string.right_pod))
        linkStatus = makeStatus(getString(R.string.mywhoosh_link))
        leftUnlockStatus = makeStatus(getString(R.string.left_pod_unlock))
        rightUnlockStatus = makeStatus(getString(R.string.right_pod_unlock))
        lastPressStatus = makeStatus(getString(R.string.last_button_press))

        val start = Button(this).apply {
            text = getString(R.string.start_bridge)
            setOnClickListener { requestPermissionsAndStart() }
        }
        val stop = Button(this).apply {
            text = getString(R.string.stop_bridge)
            setOnClickListener {
                stopService(Intent(this@MainActivity, ClickService::class.java))
                renderStatus()
            }
        }
        modeButton = Button(this).apply {
            setOnClickListener { toggleMode() }
        }
        signInButton = Button(this).apply {
            text = getString(R.string.sign_in_to_zwift)
            setOnClickListener { showSignInDialog() }
        }

        content.addView(title, matchWidth())
        content.addView(instruction, matchWidth())
        content.addView(modeButton, matchWidth())
        content.addView(leftStatus, matchWidth())
        content.addView(rightStatus, matchWidth())
        content.addView(leftUnlockStatus, matchWidth())
        content.addView(rightUnlockStatus, matchWidth())
        content.addView(lastPressStatus, matchWidth())
        content.addView(linkStatus, matchWidth())
        content.addView(start, matchWidth())
        content.addView(stop, matchWidth())
        content.addView(signInButton, matchWidth())
        return ScrollView(this).apply { addView(content) }
    }

    private fun makeStatus(label: String) = TextView(this).apply {
        text = getString(R.string.status_line, label, getString(R.string.not_running))
        textSize = 17f
        setPadding(0, 8, 0, 8)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun updateSignInButton() {
        signInButton.text = if (unlockClient.ready()) {
            getString(R.string.zwift_signed_in)
        } else {
            getString(R.string.sign_in_to_zwift)
        }
        signInButton.isEnabled = true
    }

    private fun showSignInDialog() {
        val emailField = EditText(this).apply {
            hint = getString(R.string.email_hint)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }
        val passwordField = EditText(this).apply {
            hint = getString(R.string.password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            addView(emailField, matchWidth())
            addView(passwordField, matchWidth())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.sign_in_title)
            .setView(container)
            .setPositiveButton(R.string.sign_in, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ok.setOnClickListener {
                val email = emailField.text?.toString()?.trim().orEmpty()
                val password = passwordField.text?.toString().orEmpty()
                if (email.isEmpty()) {
                    emailField.error = getString(R.string.email_required)
                    return@setOnClickListener
                }
                if (password.isEmpty()) {
                    passwordField.error = getString(R.string.password_required)
                    return@setOnClickListener
                }

                ok.isEnabled = false
                unlockClient.loginAsync(email, password) { success ->
                    runOnUiThread {
                        if (!dialog.isShowing) return@runOnUiThread
                        ok.isEnabled = true
                        if (success) {
                            instruction.text = getString(R.string.sign_in_success)
                            updateSignInButton()
                            dialog.dismiss()
                        } else {
                            instruction.text = getString(R.string.sign_in_failed)
                            passwordField.error = getString(R.string.sign_in_failed)
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun requestPermissionsAndStart() {
        val required = requiredBluetoothPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (required.isEmpty()) startBridgeService() else permissionLauncher.launch(required.toTypedArray())
    }

    private fun requiredBluetoothPermissions(): List<String> =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    private fun startBridgeService() {
        val intent = Intent(this, ClickService::class.java)
        startForegroundService(intent)
    }

    private fun renderStatus() {
        val prefs = getSharedPreferences(ClickService.PREFERENCES, MODE_PRIVATE)
        val running = prefs.getBoolean(ClickService.KEY_RUNNING, false)
        val mode = prefs.getString(ClickService.KEY_MODE, MyWhooshProtocol.Mode.SINGLE.name)
        modeButton.text = if (mode == MyWhooshProtocol.Mode.DOUBLE.name) {
            getString(R.string.mode_double)
        } else {
            getString(R.string.mode_single)
        }
        leftStatus.text = getString(
            R.string.status_line,
            getString(R.string.left_pod),
            statusValue(running, ClickService.KEY_LEFT, R.string.scanning),
        )
        rightStatus.text = getString(
            R.string.status_line,
            getString(R.string.right_pod),
            statusValue(running, ClickService.KEY_RIGHT, R.string.scanning),
        )
        linkStatus.text = getString(
            R.string.status_line,
            getString(R.string.mywhoosh_link),
            statusValue(running, ClickService.KEY_LINK, R.string.starting),
        )
        val leftUnlock = if (running) {
            prefs.getString(ClickService.KEY_LEFT_UNLOCK, LeftPodUnlockState.Value.UNKNOWN.name)
                ?: LeftPodUnlockState.Value.UNKNOWN.name
        } else {
            LeftPodUnlockState.Value.UNKNOWN.name
        }
        leftUnlockStatus.text = getString(
            R.string.status_line,
            getString(R.string.left_pod_unlock),
            unlockStatusLabel(leftUnlock),
        )
        rightUnlockStatus.text = getString(
            R.string.status_line,
            getString(R.string.right_pod_unlock),
            getString(R.string.unlock_not_required),
        )
        val lastButton = prefs.getString(ClickService.KEY_LAST_PRESS_BUTTON, null)
        val lastPress = if (lastButton == null) {
            getString(R.string.no_button_press)
        } else {
            val side = prefs.getString(ClickService.KEY_LAST_PRESS_SIDE, "") ?: ""
            val timestamp = prefs.getLong(ClickService.KEY_LAST_PRESS_AT, 0L)
            if (timestamp > 0L) {
                getString(
                    R.string.last_button_press_value,
                    side,
                    lastButton,
                    DateFormat.getTimeFormat(this).format(timestamp),
                )
            } else {
                getString(R.string.last_button_press_name, side, lastButton)
            }
        }
        lastPressStatus.text = getString(
            R.string.status_line,
            getString(R.string.last_button_press),
            lastPress,
        )
    }

    private fun unlockStatusLabel(value: String): String = when (value) {
        LeftPodUnlockState.Value.LOCKED.name -> getString(R.string.unlock_locked)
        LeftPodUnlockState.Value.UNLOCKING.name -> getString(R.string.unlock_in_progress)
        LeftPodUnlockState.Value.UNLOCKED.name -> getString(R.string.unlock_unlocked)
        else -> getString(R.string.unlock_unknown)
    }

    private fun toggleMode() {
        val prefs = getSharedPreferences(ClickService.PREFERENCES, MODE_PRIVATE)
        val current = prefs.getString(ClickService.KEY_MODE, MyWhooshProtocol.Mode.SINGLE.name)
        val next = if (current == MyWhooshProtocol.Mode.DOUBLE.name) {
            MyWhooshProtocol.Mode.SINGLE
        } else {
            MyWhooshProtocol.Mode.DOUBLE
        }
        prefs.edit().putString(ClickService.KEY_MODE, next.name).apply()
        renderStatus()
    }

    private fun statusValue(running: Boolean, key: String, fallback: Int): String =
        if (running) {
            getSharedPreferences(ClickService.PREFERENCES, MODE_PRIVATE).getString(key, getString(fallback))
                ?: getString(fallback)
        } else {
            getString(R.string.not_running)
        }
}
