package com.samco.grapheasy.displaytrackgroup

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.FeatureType

const val EXISTING_FEATURES_ARG_KEY = "existingFeatures"
const val EXISTING_FEATURES_DELIM = ","

class AddFeatureDialogFragment : DialogFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var scrollView: ScrollView
    private lateinit var baseLinearLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var nameEditText: EditText
    private lateinit var featureTypeSpinner: Spinner
    private lateinit var discreteValuesTextView: TextView
    private lateinit var discreteValuesLinearLayout: LinearLayout
    private lateinit var addDiscreteValueButton: ImageButton
    private lateinit var alertDialog: AlertDialog
    private lateinit var listener: AddFeatureDialogListener
    private lateinit var viewModel: AddFeatureDialogViewModel
    private lateinit var existingFeatures: List<String>

    interface AddFeatureDialogListener {
        fun getViewModel(): AddFeatureDialogViewModel
        fun onAddFeature(name: String, featureType: FeatureType, discreteValues: List<String>)
    }

    interface AddFeatureDialogViewModel {
        var featureName: String?
        var featureType: FeatureType?
        var discreteValues: MutableList<String>?
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            listener = parentFragment as AddFeatureDialogListener
            viewModel = listener.getViewModel()
            val view = it.layoutInflater.inflate(R.layout.feature_input_dialog, null)
            scrollView = view.findViewById(R.id.scrollView)
            baseLinearLayout = view.findViewById(R.id.baseLinearLayout)
            errorText = view.findViewById(R.id.errorText)
            nameEditText = view.findViewById(R.id.featureNameText)
            featureTypeSpinner = view.findViewById(R.id.featureTypeSpinner)
            discreteValuesTextView = view.findViewById(R.id.discreteValuesTextView)
            discreteValuesLinearLayout = view.findViewById(R.id.discreteValues)
            addDiscreteValueButton = view.findViewById(R.id.addDiscreteValueButton)
            existingFeatures = arguments
                ?.getString(EXISTING_FEATURES_ARG_KEY)
                ?.split(EXISTING_FEATURES_DELIM)
                ?: listOf()

            initFromViewModel()

            nameEditText.setSelection(nameEditText.text.length)
            nameEditText.addTextChangedListener(nameEditTextFormValidator())
            nameEditText.requestFocus()
            featureTypeSpinner.onItemSelectedListener = this
            addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.add) { _, _ -> onPositiveClicked() }
                .setNegativeButton(R.string.cancel) { _, _ -> clearViewModel() }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                inflateDiscreteValuesFromViewModel()
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initFromViewModel() {
        if (viewModel.featureName != null) nameEditText.setText(viewModel.featureName!!)
        else viewModel.featureName = ""
        if (viewModel.featureType != null) featureTypeSpinner.setSelection(spinnerIndexOf(viewModel.featureType!!))
        else viewModel.featureType = FeatureType.CONTINUOUS
        if (viewModel.discreteValues == null) viewModel.discreteValues = mutableListOf()
    }

    private fun inflateDiscreteValuesFromViewModel() {
        viewModel.discreteValues!!.forEachIndexed { i, v -> inflateDiscreteValue(i, v) }
    }

    private fun spinnerIndexOf(featureType: FeatureType): Int = when(featureType) {
        FeatureType.CONTINUOUS -> 0
        else -> 1
    }

    private fun inflateDiscreteValue(viewModelIndex: Int, initialText: String) {
        val item = layoutInflater.inflate(R.layout.feature_discrete_value_list_item, discreteValuesLinearLayout, false)
        val inputText = item.findViewById<EditText>(R.id.discreteValueNameText)
        inputText.setText(initialText)
        inputText.addTextChangedListener(discreteValueTextFormValidator(viewModelIndex))
        item.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener { onDeleteDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.upButton).setOnClickListener { onUpClickedDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.downButton).setOnClickListener { onDownClickedDiscreteValue(item) }
        discreteValuesLinearLayout.addView(item)
        reIndexDiscreteValueListItems()
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
            inputText.requestFocus()
        }
        validateForm()
    }

    private fun onAddDiscreteValue() {
        inflateDiscreteValue(viewModel.discreteValues!!.size, "")
    }

    private fun discreteValueTextFormValidator(index: Int) = object: TextWatcher {
        override fun afterTextChanged(editText: Editable?) {
            viewModel.discreteValues!![index] = editText.toString()
            validateForm()
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    }

    private fun nameEditTextFormValidator() = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            viewModel.featureName = nameEditText.text.toString()
            validateForm()
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    }

    private fun validateForm() {
        var errorSet = false
        var discreteValueStrings = viewModel.discreteValues!!
        if (viewModel.featureType!! == FeatureType.DISCRETE && discreteValueStrings.isNullOrEmpty()) {
            setErrorText(getString(R.string.discrete_feature_needs_at_least_one_value))
            errorSet = true
        }
        for (s in discreteValueStrings) {
            if (s.isEmpty()) {
                setErrorText(getString(R.string.discrete_value_must_have_name))
                errorSet = true
            }
        }
        nameEditText.text.let {
            if (it.isEmpty()) {
                setErrorText(getString(R.string.feature_name_cannot_be_null))
                errorSet = true
            }
            else if (existingFeatures.contains(it.toString())) {
                setErrorText(getString(R.string.feature_with_that_name_exists))
                errorSet = true
            }
        }
        if (errorSet) {
            setPositiveButtonEnabled(false)
        } else {
            setPositiveButtonEnabled(true)
            clearErrorText()
        }
    }

    private fun setErrorText(text: String) {
        errorText.text = text
        errorText.visibility = View.VISIBLE
    }

    private fun clearErrorText() {
        setErrorText("")
        errorText.visibility = View.GONE
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabled
    }

    private fun onDownClickedDiscreteValue(item: View) {
        val currIndex = discreteValuesLinearLayout.indexOfChild(item)
        if (currIndex == discreteValuesLinearLayout.childCount-1) return
        discreteValuesLinearLayout.removeView(item)
        discreteValuesLinearLayout.addView(item, currIndex+1)
        reIndexDiscreteValueListItems()
    }

    private fun onUpClickedDiscreteValue(item: View) {
        val currIndex = discreteValuesLinearLayout.indexOfChild(item)
        if (currIndex == 0) return
        discreteValuesLinearLayout.removeView(item)
        discreteValuesLinearLayout.addView(item, currIndex-1)
        reIndexDiscreteValueListItems()
    }

    private fun onDeleteDiscreteValue(item: View) {
        discreteValuesLinearLayout.removeView(item)
        reIndexDiscreteValueListItems()
    }

    private fun reIndexDiscreteValueListItems() {
        viewModel.discreteValues = mutableListOf()
        discreteValuesLinearLayout.children.forEachIndexed { index, view ->
            view.findViewById<TextView>(R.id.indexText).text = "$index :"
            val currText = view.findViewById<EditText>(R.id.discreteValueNameText).text.toString()
            viewModel.discreteValues!!.add(currText)
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) = when(position) {
        0 -> onFeatureTypeSelected(false)
        1 -> onFeatureTypeSelected(true)
        else -> {}
    }
    override fun onNothingSelected(p0: AdapterView<*>?) { }

    private fun onFeatureTypeSelected(discrete: Boolean) {
        viewModel.featureType = if (discrete) FeatureType.DISCRETE else FeatureType.CONTINUOUS
        val vis = if (discrete) View.VISIBLE else View.GONE
        discreteValuesTextView.visibility = vis
        discreteValuesLinearLayout.visibility = vis
        addDiscreteValueButton.visibility = vis
        validateForm()
    }

    private fun onPositiveClicked() {
        listener.onAddFeature(nameEditText.text.toString(), viewModel.featureType!!, viewModel.discreteValues!!)
        clearViewModel()
    }

    private fun clearViewModel() {
        viewModel.featureName = null
        viewModel.discreteValues = null
        viewModel.featureType = null
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        clearViewModel()
    }
}