package com.brendon978.iaexemplo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Recebe o texto falado no launcher. Uma IA de verdade decidiria aqui o que fazer (falar, agir, notificar). */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        context.getSharedPreferences("ia", Context.MODE_PRIVATE).edit().putString("ultimo", text).apply()
        Toast.makeText(context, "IA Exemplo recebeu: $text", Toast.LENGTH_LONG).show()
    }
}
