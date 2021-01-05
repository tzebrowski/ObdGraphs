package org.openobd2.core.logger.ui.dashboard

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.openobd2.core.command.CommandReply
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val tableLayout: TableLayout = root.findViewById(R.id.table_layout);
        val scrollView: ScrollView = root.findViewById(R.id.scroll_view);

        Model.commandReplyLiveData.observe(viewLifecycleOwner, Observer {
            try {
                val tableRow: TableRow = tableLayout.findViewById(it.command.hashCode())
                tableRow.background = getResources().getDrawable(R.drawable.border);
                val value: TextView = tableRow.get(0) as TextView
                value.text = spannedText(it.value,Color.parseColor("#01804F"),1.3f)
            } catch (e: IllegalStateException) {
                addRow(root, it, tableLayout, scrollView);
            }
        })
        return root
    }

    private fun addRow(
        root: View,
        it: CommandReply<*>,
        tableLayout: TableLayout,
        scrollView: ScrollView
    ) {
        val tableParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.WRAP_CONTENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )

        val tableRow = TableRow(root.context)
        tableRow.background = getResources().getDrawable(R.drawable.border);
        tableRow.id = it.command.hashCode()
        tableRow.setLayoutParams(tableParams)

        val value = TextView(context)
        val valueParams = TableRow.LayoutParams()

        valueParams.width = 50
        valueParams.weight = 1.0f
        value.layoutParams = valueParams

        value.text = spannedText(it.value,Color.parseColor("#01804F"),1.3f)
        tableRow.addView(value)

        val raw = TextView(context)
        val rawParams = TableRow.LayoutParams()

        rawParams.width = 50
        rawParams.weight = 1.0f
        raw.layoutParams = rawParams
        raw.text = it.raw?.toString()
        tableRow.addView(raw)

        val label = TextView(context)
        val labelarams = TableRow.LayoutParams()

        labelarams.width = 200
        labelarams.weight = 1.0f

        label.layoutParams = labelarams
        label.text = spannedText(it.command.label,Color.GRAY,1.1f)
        label.setSingleLine(false)
        tableRow.addView(label)

        tableLayout.addView(tableRow)
        scrollView.fullScroll(View.FOCUS_DOWN)
    }

    private fun spannedText(it:Any?,color: Int,size: Float): SpannableString {
        var valText: String
        if (it == null) {
            valText = ""
        } else {
            valText = it.toString()
        }

        val valSpanString = SpannableString(valText)
        valSpanString.setSpan( RelativeSizeSpan(size), 0, valSpanString.length, 0); // set size
        valSpanString.setSpan(UnderlineSpan(), 0, valSpanString.length, 0)
        valSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, valSpanString.length, 0)
        //valSpanString.setSpan(StyleSpan(Typeface.ITALIC), 0, valSpanString.length, 0)
        valSpanString.setSpan(
            ForegroundColorSpan(color),
            0,
            valSpanString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return valSpanString
    }
}