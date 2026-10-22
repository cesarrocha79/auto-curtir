package com.cesar.autocurtir

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var etTaps: EditText
    private lateinit var etInterval: EditText
    private lateinit var etWait: EditText
    private lateinit var etMax: EditText

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun addLabel(root: LinearLayout, text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setPadding(0, dp(14), 0, 0)
        })
    }

    private fun addField(root: LinearLayout, label: String, value: Int): EditText {
        addLabel(root, label)
        val et = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(value.toString())
        }
        root.addView(et)
        return et
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = Config.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "Auto Curtir"
            textSize = 26f
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1. Ativar serviço de acessibilidade"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(TextView(this).apply {
            text = "Em Acessibilidade, procure \"Auto Curtir\" (pode estar em \"Apps instalados\") e ative. " +
                "No Android 13 ou superior, se aparecer \"Configuração restrita\", vá em " +
                "Configurações > Apps > Auto Curtir > menu de 3 pontos > \"Permitir configurações restritas\", " +
                "e depois ative o serviço."
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        })

        etTaps = addField(root, "Quantidade de toques em cada vídeo", c.taps)
        etInterval = addField(root, "Intervalo entre os toques (milissegundos)", c.tapIntervalMs)
        etWait = addField(root, "Espera depois de rolar para o próximo vídeo (milissegundos)", c.waitAfterSwipeMs)
        etMax = addField(root, "Quantidade de vídeos (0 = infinito)", c.maxVideos)

        root.addView(Button(this).apply {
            text = "Salvar configurações"
            setOnClickListener {
                save()
                Toast.makeText(this@MainActivity, "Salvo", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(Button(this).apply {
            text = "2. Mostrar painel flutuante"
            setOnClickListener {
                val s = AutoService.instance
                if (s == null) {
                    Toast.makeText(this@MainActivity, "Ative o serviço de acessibilidade primeiro", Toast.LENGTH_LONG).show()
                } else {
                    save()
                    s.showPanel()
                    moveTaskToBack(true)
                }
            }
        })

        root.addView(TextView(this).apply {
            text = "Abra o app de vídeo, deixe no primeiro vídeo e toque em ▶ no painel flutuante. " +
                "Toque em ⏹ para parar. Arraste o painel pelo ⠿ para mudar de lugar."
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        status.text = if (AutoService.instance != null)
            "Serviço de acessibilidade: ATIVO"
        else
            "Serviço de acessibilidade: INATIVO"
    }

    private fun save() {
        val cfg = Config(
            (etTaps.text.toString().toIntOrNull() ?: 30).coerceAtLeast(1),
            (etInterval.text.toString().toIntOrNull() ?: 120).coerceAtLeast(50),
            (etWait.text.toString().toIntOrNull() ?: 1500).coerceAtLeast(200),
            (etMax.text.toString().toIntOrNull() ?: 0).coerceAtLeast(0)
        )
        Config.save(this, cfg)
    }
}
