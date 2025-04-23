package sweng894.project.adopto.custom

import android.content.Context
import android.text.Editable
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import sweng894.project.adopto.R
import sweng894.project.adopto.databinding.ViewStringInputBinding

class StringInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    def_style: Int = 0
) : LinearLayout(context, attrs, def_style) {

    private val binding: ViewStringInputBinding

    init {
        orientation = VERTICAL
        val inflater = LayoutInflater.from(context)
        binding = ViewStringInputBinding.inflate(inflater, this)

        attrs?.let {
            val typed_array = context.obtainStyledAttributes(it, R.styleable.StringInputView)
            val title = typed_array.getString(R.styleable.StringInputView_titleText)
            val input_height = typed_array.getDimensionPixelSize(
                R.styleable.StringInputView_inputHeight,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val isClickableMode =
                typed_array.getBoolean(R.styleable.StringInputView_clickableMode, false)
            typed_array.recycle()

            binding.titleField.text = title

            // ✅ Set height correctly by updating layout params and reassigning it
            val params = binding.inputTextField.layoutParams
                ?: LayoutParams(LayoutParams.MATCH_PARENT, input_height)

            params.height = input_height
            binding.inputTextField.layoutParams = params
            binding.inputTextField.movementMethod = ScrollingMovementMethod.getInstance()

            if (isClickableMode) {
                // Disable user editing, only display custom set text
                binding.inputTextField.isFocusable = false
                binding.inputTextField.isClickable = false
                binding.inputTextField.isLongClickable = false
                binding.inputTextField.isCursorVisible = false
                // Forward internal EditText click to the parent view's onClickListener
                binding.inputTextField.setOnClickListener {
                    performClick() // This lets the outer view's click listener run
                }

                binding.titleField.setOnClickListener {
                    performClick() // also forward title click
                }
            }
        }
    }

    fun getInputText(): String = binding.inputTextField.text.toString()

    fun setInputText(text: String) {
        binding.inputTextField.setText(text)
    }

    fun setTitle(text: String) {
        binding.titleField.text = text
    }

    fun setInputHeight(height_px: Int) {
        val params = binding.inputTextField.layoutParams
        params.height = height_px
        binding.inputTextField.layoutParams = params
        binding.inputTextField.requestLayout()
    }

    fun doAfterTextChanged(action: (Editable?) -> Unit) {
        binding.inputTextField.doAfterTextChanged(action)
    }
}

