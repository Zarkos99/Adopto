package sweng894.project.adopto.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import sweng894.project.adopto.R

class MultiSelectView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var m_button: Button
    private var m_text_view: TextView

    private var m_selected_items = mutableListOf<String>()

    interface OnSelectionChangeListener {
        fun onSelectionChanged(selectedItems: List<String>)
    }

    private var m_selection_change_listener: OnSelectionChangeListener? = null

    // Options to be passed to the dialog
    private var options: List<String> = listOf()

    init {
        LayoutInflater.from(context).inflate(R.layout.multi_select_view, this, true)

        m_button = findViewById(R.id.button_select)
        m_text_view = findViewById(R.id.text_view_selected)

        m_button.setOnClickListener { showMultiSelectDialog() }
        m_text_view.setOnClickListener { showMultiSelectDialog() }
    }

    /**
     * Sets the list of available options that will be displayed in the MultiSelectDialogFragment
     */
    fun setOptions(options_list: List<String>) {
        options = options_list
    }

    /**
     * Updates the selected items and updates the displayed text.
     */
    fun setSelectedItems(items: List<String>) {
        val old_selected_items = m_selected_items.toList()
        m_selected_items.clear()
        m_selected_items.addAll(items)

        // Update the UI text
        m_text_view.text = if (m_selected_items.isNotEmpty()) {
            "Selected: ${m_selected_items.joinToString(", ")}"
        } else {
            "No items selected"
        }

        if (old_selected_items != m_selected_items) {
            // Notify the parent that selection has changed
            m_selection_change_listener?.onSelectionChanged(m_selected_items)
        }
    }

    /**
     * Allows the parent (Activity/Fragment) to set a listener
     */
    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        m_selection_change_listener = listener
    }

    fun getSelectedItems(): List<String> {
        return m_selected_items.toList()
    }

    /**
     * Opens the multi-select dialog and passes the pre-selected items.
     */
    private fun showMultiSelectDialog() {
        // Ensure we are using an Activity context
        val activity = when (context) {
            is FragmentActivity -> context as FragmentActivity
            is android.view.ContextThemeWrapper -> (context as? android.view.ContextThemeWrapper)?.baseContext as? FragmentActivity
            else -> null
        }

        if (activity == null) {
            Log.e("### ERROR", "Context is not a FragmentActivity! Cannot show dialog.")
            return
        }
        if (options.isEmpty()) {
            Log.e(
                "showMultiSelectDialog ERROR",
                "Cannot open dialog because options list is EMPTY!"
            )
            return
        }

        val dialog = MultiSelectDialogFragment.newInstance(
            options.toMutableList(),
            m_selected_items.toMutableList()
        )

        dialog.setListener(object : MultiSelectDialogFragment.MultiSelectListener {
            override fun onItemsSelected(selected_items: List<String>) {
                setSelectedItems(selected_items)
            }
        })

        // Ensure the dialog is only shown if the activity is not finishing
        if (!activity.isFinishing) {
            dialog.show(activity.supportFragmentManager, "MultiSelectDialog")
        } else {
            Log.e("ERROR", "Activity is finishing, cannot show dialog!")
        }
    }

}
