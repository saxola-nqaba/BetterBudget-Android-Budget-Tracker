package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.dachkaboiz.betterbudget_bestbudget.R

/**
 * SnoozeDialogFragment
 *
 * Shown when a new expense would push a category over its monthly budget limit.
 *
 * Layout: dialog_snooze_v2.xml
 * IDs used:
 *   tvSnoozeMessage      — main message text
 *   tvSnoozeCount        — snoozes remaining label
 *   btnUseSnooze         — visible only if snoozesLeft > 0
 *   btnLogWithoutSnooze  — always visible; saves expense and marks over budget
 *   btnCloseSnooze       — red X; dismisses without saving
 *
 * Results are sent back to AddExpenseFragment via the Fragment Result API
 * under the key "snooze_result":
 *   "action" = "USE_SNOOZE"  — save expense, consume one snooze
 *   "action" = "SAVE"        — save expense without using a snooze
 *   "action" = "CANCEL"      — do not save, return to form
 *
 * Usage:
 *   SnoozeDialogFragment.newInstance(
 *       categoryName = "Groceries",
 *       currentTotal = 850.0,
 *       maxGoal      = 1000.0,
 *       newAmount    = 200.0,
 *       snoozesLeft  = 2
 *   ).show(parentFragmentManager, "snooze_dialog")
 */
class SnoozeDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_CATEGORY_NAME = "categoryName"
        private const val ARG_CURRENT_TOTAL = "currentTotal"
        private const val ARG_MAX_GOAL      = "maxGoal"
        private const val ARG_NEW_AMOUNT    = "newAmount"
        private const val ARG_SNOOZES_LEFT  = "snoozesLeft"

        /**
         * Factory method — always use this instead of the constructor
         * so the arguments survive process death.
         */

        // Code Attribution
        // This method was adapted from Android Developers
        // https://developer.android.com/guide/fragments/dialogs
        // Android Developers
        fun newInstance(
            categoryName: String,
            currentTotal: Double,
            maxGoal: Double,
            newAmount: Double,
            snoozesLeft: Int
        ): SnoozeDialogFragment {
            return SnoozeDialogFragment().apply {
                arguments = bundleOf(
                    ARG_CATEGORY_NAME to categoryName,
                    ARG_CURRENT_TOTAL to currentTotal,
                    ARG_MAX_GOAL      to maxGoal,
                    ARG_NEW_AMOUNT    to newAmount,
                    ARG_SNOOZES_LEFT  to snoozesLeft
                )
            }
        }
    }


    // Inflate


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_snooze_v2, container, false)


    // Bind views and set content


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read arguments
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME) ?: "this category"
        val currentTotal = arguments?.getDouble(ARG_CURRENT_TOTAL) ?: 0.0
        val maxGoal      = arguments?.getDouble(ARG_MAX_GOAL)      ?: 0.0
        val newAmount    = arguments?.getDouble(ARG_NEW_AMOUNT)     ?: 0.0
        val snoozesLeft  = arguments?.getInt(ARG_SNOOZES_LEFT)     ?: 0

        val tvMessage     = view.findViewById<TextView>(R.id.tvSnoozeMessage)
        val tvCount       = view.findViewById<TextView>(R.id.tvSnoozeCount)
        val btnUseSnooze  = view.findViewById<Button>(R.id.btnUseSnooze)
        val btnLogWithout = view.findViewById<Button>(R.id.btnLogWithoutSnooze)
        val btnClose      = view.findViewById<ImageView>(R.id.btnCloseSnooze)

        // Build the warning message
        val projectedTotal = currentTotal + newAmount
        val overspend      = projectedTotal - maxGoal

        if (snoozesLeft > 0) {
            // Scenario A — snoozes available
            tvMessage.text =
                "Adding R %.2f to \"$categoryName\" will put you R %.2f over budget.\n\n".format(
                    newAmount, overspend
                ) +
                        "You currently have $snoozesLeft snooze(s) left this month. " +
                        "Using a snooze will suppress the overspend warning for this expense."
            btnUseSnooze.visibility = View.VISIBLE
        } else {
            // Scenario B — no snoozes left
            tvMessage.text =
                "Adding R %.2f to \"$categoryName\" will put you R %.2f over budget.\n\n".format(
                    newAmount, overspend
                ) +
                        "You have no more snoozes left for this month."
            btnUseSnooze.visibility = View.GONE
        }

        tvCount.text = "Snoozes Left: $snoozesLeft"


        // Button listeners — send result back to AddExpenseFragment


        // USE SNOOZE — save expense and consume a snooze
        btnUseSnooze.setOnClickListener {
            sendResult("USE_SNOOZE")
            dismiss()
        }

        // LOG WITHOUT SNOOZE — save expense but mark category as over budget
        btnLogWithout.setOnClickListener {
            sendResult("SAVE")
            dismiss()
        }

        // CLOSE (red X) — cancel, do not save
        btnClose.setOnClickListener {
            sendResult("CANCEL")
            dismiss()
        }
    }


    // Result helper


    /**
     * Sends the result back to the parent fragment via the Fragment Result API.
     * AddExpenseFragment listens for "snooze_result" and acts accordingly.
     */

    // Code Attribution
    // This method was adapted from Android Developers
    // https://developer.android.com/guide/fragments/communicate
    // Android Developers
    private fun sendResult(action: String) {
        parentFragmentManager.setFragmentResult(
            "snooze_result",
            bundleOf("action" to action)
        )
    }


    // Make the dialog fill most of the screen width


    override fun onStart() {
        super.onStart()

        // Code Attribution
        // This method was adapted from Stack Overflow
        // https://stackoverflow.com/questions/23990726/how-to-make-dialogfragment-width-to-fill-parent
        // savepopulation
        // https://stackoverflow.com/users/1559852/savepopulation

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}