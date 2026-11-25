package com.mickster.gamersvault

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.tvStatus)
        val actionButton = findViewById<Button>(R.id.btnAction)

        actionButton.setOnClickListener {
            clickCount++
            statusText.text = "Gamer's Vault Action: $clickCount"
        }
    }
}
