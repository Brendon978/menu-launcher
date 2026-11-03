package com.brendon978.iaexemplo

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/** Abre no toque do símbolo e mostra o último comando recebido. */
class MainActivity : Activity() {
    override fun onResume() {
        super.onResume()
        val last = getSharedPreferences("ia", MODE_PRIVATE).getString("ultimo", null)
        setContentView(TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 20f
            text = if (last == null) "IA Exemplo\n\nNenhum comando ainda." else "Último comando:\n\n$last"
        })
    }
}
