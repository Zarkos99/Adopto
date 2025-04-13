package sweng894.project.adopto.custom

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import sweng894.project.adopto.R

class CustomSpinnerAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(
    context,
    android.R.layout.simple_spinner_item,
    items
) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as? TextView)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
            setBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as? TextView)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            setBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
        }
        return view
    }
}
