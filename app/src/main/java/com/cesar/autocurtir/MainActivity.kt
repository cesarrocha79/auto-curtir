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
    private lateinit var etTapsMin: EditText
    private lateinit var etTapsMax: EditText
    private lateinit var etIntMin: EditText
    private lateinit var etIntMax: EditText
    private lateinit var etWaitMin: EditText
    private lateinit var etWaitMax: EditText
    private lateinit var etSpread: EditText
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

    private fun addRange(root: LinearLayout, label: String, min: Int, max: Int): Pair<EditText, EditText> {
        addLabel(root, label)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        fun field(dica: String, v: Int): EditText {
            val et = EditText(this)
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.setText(v.toString())
            et.hint = dica
            row.addView(et, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            return et
        }

        val a = field("mín", min)
        val b = field("máx", max)
        root.addView(row)
        return Pair(a, b)
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

        root.addView(TextView(this).apply {
            text = "Cada faixa abaixo é sorteada a cada vídeo (ou a cada toque, no caso do intervalo)."
            textSize = 12f
            setPadding(0, dp(16), 0, 0)
        })

        val taps = addRange(root, "Quantidade de toques por vídeo (mín e máx)", c.tapsMin, c.tapsMax)
        etTapsMin = taps.first
        etTapsMax = taps.second

        val inter = addRange(root, "Intervalo entre os toques, em milissegundos (mín e máx)", c.intervalMin, c.intervalMax)
        etIntMin = inter.first
        etIntMax = inter.second

        val wait = addRange(root, "Espera depois de rolar para o próximo vídeo, em milissegundos (mín e máx)", c.waitMin, c.waitMax)
        etWaitMin = wait.first
        etWaitMax = wait.second

        etSpread = addField(root, "Variação do ponto de toque, em pixels ao redor do centro (0 = sempre no mesmo ponto)", c.spread)
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

    private fun num(e: EditText, def: Int): Int = e.text.toString().toIntOrNull() ?: def

    private fun save() {
        val tapsMin = num(etTapsMin, 30).coerceAtLeast(1)
        val tapsMax = num(etTapsMax, 50).coerceAtLeast(tapsMin)
        val intMin = num(etIntMin, 120).coerceAtLeast(50)
        val intMax = num(etIntMax, 500).coerceAtLeast(intMin)
        val waitMin = num(etWaitMin, 1500).coerceAtLeast(200)
        val waitMax = num(etWaitMax, 3500).coerceAtLeast(waitMin)
        val spread = num(etSpread, 15).coerceIn(0, 200)
        val max = num(etMax, 0).coerceAtLeast(0)
        Config.save(this, Config(tapsMin, tapsMax, intMin, intMax, waitMin, waitMax, spread, max))
    }
}
