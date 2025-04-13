package sweng894.project.adopto.custom

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckedTextView
import android.widget.TextView
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import sweng894.project.adopto.R

class MultiSelectDialogFragment : DialogFragment() {

    interface MultiSelectListener {
        fun onItemsSelected(selected_items: List<String>)
    }

    private lateinit var listener: MultiSelectListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray(arg_items) ?: arrayOf()
        val pre_selected_items = arguments?.getStringArray(arg_selected_items) ?: arrayOf()
        val selected_items = pre_selected_items.toMutableList()

        val custom_title = TextView(requireContext()).apply {
            text = "Select Options"
            setPadding(40, 30, 40, 20)
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            textSize = 20f
        }

        val selected_flags = BooleanArray(items.size) { index ->
            selected_items.contains(items[index])
        }

        val dialog_context = ContextThemeWrapper(
            requireContext(),
            androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert
        )

        return AlertDialog.Builder(dialog_context)
            .setCustomTitle(custom_title)
            .setMultiChoiceItems(items, selected_flags) { _, index, isChecked ->
                val selected_item = items[index]
                if (isChecked) {
                    selected_items.add(selected_item)
                } else {
                    selected_items.remove(selected_item)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                listener.onItemsSelected(selected_items)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All") { _, _ ->
                listener.onItemsSelected(emptyList())
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as? AlertDialog ?: return
        val context = requireContext()

        val primaryColor = ContextCompat.getColor(context, R.color.primary_text)
        val backgroundColor = ContextCompat.getColor(context, R.color.card_background)
        val buttonColor = ContextCompat.getColor(context, R.color.primary_button)

        alertDialog.window?.setBackgroundDrawableResource(R.color.card_background)

        alertDialog.listView?.apply {
            divider = null
            selector = ContextCompat.getDrawable(context, android.R.color.transparent)
            setBackgroundColor(backgroundColor)

            // Set initial item text color
            for (i in 0 until childCount) {
                (getChildAt(i) as? CheckedTextView)?.setTextColor(primaryColor)
            }

            // Update color after layout
            post {
                for (i in 0 until childCount) {
                    (getChildAt(i) as? CheckedTextView)?.setTextColor(primaryColor)
                }
            }
        }

        // 🔹 Set button text colors manually
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(buttonColor)
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(buttonColor)
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(buttonColor)
    }

    fun setListener(listener: MultiSelectListener) {
        this.listener = listener
    }

    companion object {
        private const val arg_items = "items"
        private const val arg_selected_items = "selected_items"

        fun newInstance(
            items: MutableList<String>,
            selected_items: List<String>
        ): MultiSelectDialogFragment {
            val fragment = MultiSelectDialogFragment()
            val args = Bundle().apply {
                putStringArray(arg_items, items.toTypedArray())
                putStringArray(arg_selected_items, selected_items.toTypedArray())
            }
            fragment.arguments = args
            return fragment
        }
    }
}
