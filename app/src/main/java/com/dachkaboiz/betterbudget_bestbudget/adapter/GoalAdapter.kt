package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal

class GoalAdapter(
    private var items: List<Triple<CategoryGoal, Category?, Double>>,
    private val onCardClick: (String) -> Unit,   // String firebaseId, was Int categoryID
    private val onEditClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView      = view.findViewById(R.id.tvGoalIcon)
        val tvTitle: TextView     = view.findViewById(R.id.tvGoalTitle)
        val tvCurrent: TextView   = view.findViewById(R.id.tvCurrentAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.goalProgressBar)
        val tvDate: TextView      = view.findViewById(R.id.tvGoalDate)
        val tvMinGoal: TextView   = view.findViewById(R.id.tvMinGoal)
        val tvTarget: TextView    = view.findViewById(R.id.tvTargetAmount)
        val ivEdit: ImageView     = view.findViewById(R.id.ivEditGoal)
        val ivDelete: ImageView   = view.findViewById(R.id.ivDeleteGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_v2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (goal, category, totalSpent) = items[position]

        // Icon and title — handle null category gracefully
        // Category can be null if the CategoryGoal owner hasn't
        // migrated yet and the firebaseId link doesn't match
        holder.tvIcon.text  = category?.categoryIcon ?: "🎯"
        holder.tvTitle.text = category?.categoryName ?: "Unknown Category"
        holder.tvCurrent.text = "R %.2f".format(totalSpent)

        // Goal period — uses month/year from goal since goalDate
        // was removed. This fills tvDate which was previously empty.
        holder.tvDate.text = "Period: ${goal.month}/${goal.year}"

        // Goal limits
        val minGoal = goal.minGoal ?: 0.0
        val maxGoal = goal.maxGoal ?: 0.0

        holder.tvMinGoal.text = "Min: R %.2f".format(minGoal)
        holder.tvTarget.text  = "Max: R %.2f".format(maxGoal)

        // Progress bar
        if (maxGoal > 0) {
            holder.progressBar.max = 100
            val progressPercent = ((totalSpent / maxGoal) * 100).toInt()
            holder.progressBar.progress = progressPercent.coerceAtMost(100)
        } else {
            holder.progressBar.progress = 0
        }

        // Card click — passes firebaseId (String) not categoryID (Int)
        // CategoryBreakdownFragment now expects a String constructor arg
        // If category is null (migration not done yet), disable card click
        holder.itemView.setOnClickListener {
            category?.firebaseId?.let { id -> onCardClick(id) }
        }

        // Edit and delete still use Room integer goal ID
        // These will be updated when CategoryGoal owner migrates
        holder.ivEdit.setOnClickListener {
            onEditClick(goal.goalId)
        }

        holder.ivDelete.setOnClickListener {
            onDeleteClick(goal.goalId)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Triple<CategoryGoal, Category?, Double>>) {
        items = newItems
        notifyDataSetChanged()
    }
}