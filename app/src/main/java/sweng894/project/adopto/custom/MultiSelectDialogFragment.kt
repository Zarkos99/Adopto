package sweng894.project.adopto.custom

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import sweng894.project.adopto.R

class MultiSelectDialogFragment : DialogFragment() {

    interface MultiSelectListener {
        fun onItemsSelected(selected_items: List<String>)
    }

    private lateinit var listener: MultiSelectListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray(ARG_ITEMS) ?: arrayOf()
        val pre_selected_items = arguments?.getStringArray(ARG_SELECTED_ITEMS) ?: arrayOf()

        // Boolean array to track selected state
        val selected_items = BooleanArray(items.size) { index ->
            pre_selected_items.contains(items[index]) // Pre-select items if they exist in the saved list
        }

        val selected_list = pre_selected_items.toMutableList()  // Keep track of selections

        return AlertDialog.Builder(requireContext())
            .setTitle("Select Options")
            .setMultiChoiceItems(items, selected_items) { _, index, isChecked ->
                if (isChecked) selected_list.add(items[index]) else selected_list.remove(items[index])
            }
            .setPositiveButton("OK") { _, _ ->
                listener.onItemsSelected(selected_list)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All") { _, _ ->
                listener.onItemsSelected(emptyList())
            }.create()

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(R.color.card_background)
    }

    /**
     * Allows an external class (e.g., MultiSelectView) to set the listener dynamically.
     */
    fun setListener(listener: MultiSelectListener) {
        this.listener = listener
    }

    companion object {
        private const val ARG_ITEMS = "items"
        private const val ARG_SELECTED_ITEMS = "selected_items"

        fun newInstance(
            items: MutableList<String>,
            selected_items: List<String>
        ): MultiSelectDialogFragment {
            val fragment = MultiSelectDialogFragment()
            val args = Bundle()
            args.putStringArray(ARG_ITEMS, items.toTypedArray())
            args.putStringArray(ARG_SELECTED_ITEMS, selected_items.toTypedArray())
            fragment.arguments = args
            return fragment
        }
    }
}
