package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class ExpenseAdapter(
    private val onItemClick: (Expense) -> Unit,
    private val onItemLongClick: (Expense) -> Unit,
    private val onEditClick: (Expense) -> Unit = {},
    private val onDeleteClick: (Expense) -> Unit = {},
    private val categoryNameResolver: ((String) -> String)? = null,
    private val subCategoryNameResolver: ((String) -> String)? = null
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    /*
        ViewHolder caches references to all views in item_expense_v2
        to avoid repeated findViewById calls during scrolling.
     */
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryIcon: TextView     = itemView.findViewById(R.id.tvExpenseCategoryIcon)
        val tvCategoryName: TextView     = itemView.findViewById(R.id.tvExpenseCategoryName)
        val tvSubCategoryName: TextView  = itemView.findViewById(R.id.tvExpenseSubCategoryName)
        val tvAmount: TextView           = itemView.findViewById(R.id.tvExpenseAmount)
        val tvDate: TextView             = itemView.findViewById(R.id.tvExpenseDate)
        val tvDescription: TextView      = itemView.findViewById(R.id.tvExpenseDescription)
        val ivExpensePhoto: ImageView    = itemView.findViewById(R.id.ivExpensePhoto)
        val ivEditExpense: ImageView     = itemView.findViewById(R.id.ivEditExpense)
        val ivDeleteExpense: ImageView   = itemView.findViewById(R.id.ivDeleteExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_v2, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)


        // Resolve category display string in the format :emoji name" = "📚 Books"
        // Falls back to a generic label if the resolver  returns null
        val resolved = categoryNameResolver?.invoke(expense.categoryId)
            ?: "💰 Category ${expense.categoryId}"

        // Split the resolved string at the first space to separate icon from name
        val spaceIndex = resolved.indexOf(' ')
        if (spaceIndex != -1) {
            holder.tvCategoryIcon.text = resolved.substring(0, spaceIndex)
            holder.tvCategoryName.text = resolved.substring(spaceIndex + 1)
        } else {
            //show full string as name with a default icon if no space is found
            holder.tvCategoryIcon.text = "💰"
            holder.tvCategoryName.text = resolved
        }



        // Show subcategory name if one is linked to this expense
        // Hide the view entirely if no subcategory is assigned to keep the card compact
        if (expense.subCategoryId != null) {
            holder.tvSubCategoryName.visibility = View.VISIBLE
            holder.tvSubCategoryName.text = subCategoryNameResolver?.invoke(expense.subCategoryId)
                ?: "Subcategory ${expense.subCategoryId}"
        } else {
            holder.tvSubCategoryName.visibility = View.GONE
        }

        // Format amount as South African rand to 2 decimal places
        holder.tvAmount.text = "R %.2f".format(expense.expenseAmount)

        // Format the stored epoch millisecond timestamp into a human-readable date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(expense.expenseDate))

        // Only show description if one was provided, hide the view otherwise
        if (expense.expenseDescription.isNullOrBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = expense.expenseDescription
        }


        // Load attached photo from URI if one exists
        if (!expense.imageUri.isNullOrEmpty()) {
            holder.ivExpensePhoto.visibility = View.VISIBLE
            try {
                val uri = Uri.parse(expense.imageUri)
                holder.ivExpensePhoto.setImageURI(uri)
            } catch (e: SecurityException) {
                // show an error icon rather than crashing if URI is no longer accessible
                holder.ivExpensePhoto.setImageResource(android.R.drawable.stat_notify_error)
            }
        } else {
            holder.ivExpensePhoto.visibility = View.GONE
        }

        // Wire up click handlers
        holder.itemView.setOnClickListener { onItemClick(expense) }
        holder.itemView.setOnLongClickListener { onItemLongClick(expense); true }
        holder.ivEditExpense.setOnClickListener { onEditClick(expense) }
        holder.ivDeleteExpense.setOnClickListener { onDeleteClick(expense) }
    }


      //DiffUtil callback used by ListAdapter to compute the minimal set of changes
      //between two lists, enabling efficient RecyclerView redraws.

      //Code attribution:
      //Using DiffUtil.ItemCallback pattern for Efficient RecyclerView Updates
      //https://medium.com/@sandeepkella23/using-diffutil-itemcallback-for-efficient-recyclerview-updates-729615b3528c
      //Author: Sandeep Kella
      //https://medium.com/@sandeepkella23
       class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense) =
            oldItem.expenseID == newItem.expenseID
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense) =
            oldItem == newItem
    }
}