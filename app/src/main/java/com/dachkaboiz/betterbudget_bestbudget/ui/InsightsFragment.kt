package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseSnoozeRepository
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsightsFragment : Fragment(R.layout.fragment_insights_v3) {

    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView
    private lateinit var btnLoad: MaterialButton
    private lateinit var barChart: BarChart
    private lateinit var tvNoData: TextView

    private lateinit var pieChart: PieChart



    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    private lateinit var goalRepo: FirebaseCategoryGoalRepository
    private lateinit var categoryRepo: FirebaseCategoryRepository
    private lateinit var expenseRepo: ExpenseRepository

    private lateinit var snoozeRepo: FirebaseSnoozeRepository
    private var currentMonth: Int = 0
    private var currentYear: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        goalRepo     = FirebaseCategoryGoalRepository(uid)
        categoryRepo = FirebaseCategoryRepository()
        expenseRepo  = ExpenseRepository(uid)
        snoozeRepo = FirebaseSnoozeRepository(uid)
        val now = Calendar.getInstance()
        currentMonth = now.get(Calendar.MONTH) + 1
        currentYear = now.get(Calendar.YEAR)
        tvDateFrom = view.findViewById(R.id.tvGraphDateFrom)
        tvDateTo   = view.findViewById(R.id.tvGraphDateTo)
        btnLoad    = view.findViewById(R.id.btnLoadGraph)
        barChart   = view.findViewById(R.id.barChart)
        tvNoData   = view.findViewById(R.id.tvNoGraphData)
        pieChart   = view.findViewById(R.id.categoryPieChart)

        // ── Default date range: FROM = today, TO = 2 days from now ───────────
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val fromCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        dateFrom = fromCal.timeInMillis
        tvDateFrom.text = "Start: ${fmt.format(fromCal.time)} ⌵"

        val toCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        dateTo = toCal.timeInMillis
        tvDateTo.text = "End: ${fmt.format(toCal.time)} ⌵"
        // ─────────────────────────────────────────────────────────────────────

        // Code attribution
        // This method was taken from GeeksforGeeks
        // https://www.geeksforgeeks.org/kotlin/android-create-a-pie-chart-with-kotlin/


        setupPieChartConfiguration()

        tvDateFrom.setOnClickListener {
            showDatePicker { y, m, d ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                tvDateFrom.text = "Start: %02d/%02d/%04d".format(d, m + 1, y)
            }
        }

        tvDateTo.setOnClickListener {
            showDatePicker { y, m, d ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                tvDateTo.text = "End: %02d/%02d/%04d".format(d, m + 1, y)
            }
        }

        btnLoad.setOnClickListener { loadChartData() }
    }


    // Code attribution
    // This method was taken from GeeksforGeeks
    // Android - Create a Pie Chart with Kotlin
    // https://www.geeksforgeeks.org/kotlin/android-create-a-pie-chart-with-kotlin/
    // Adaptation note: adapted with Google Gemini

    private fun setupPieChartConfiguration() {
        pieChart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            extraBottomOffset = 10f
            extraTopOffset = 0f
            extraLeftOffset = 10f
            extraRightOffset = 10f

            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f

            setDrawCenterText(true)
            centerText = "Category\nExpenses"
            setCenterTextSize(12f)
            setCenterTextColor(Color.parseColor("#0D47A1"))

            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            setDrawEntryLabels(false) // Hides messy raw text inside overlapping slices

            legend.apply {
                isEnabled = true
                isWordWrapEnabled = true
                textSize = 11f
                formToTextSpace = 6f
                xEntrySpace = 12f
            }
        }
    }

    private fun loadChartData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            val categories = categoryRepo.getCategoriesSuspend(uid)
            val goals      = goalRepo.getAllGoals()



            // Build one bar group per category that has a goal in range
            data class ChartRow(
                val label: String,
                val min: Float,
                val actual: Float,
                val max: Float,
                val isSnoozed: Boolean = false
            )

            val rows = mutableListOf<ChartRow>()

            for (goal in goals) {
                val goalCal = Calendar.getInstance().apply {
                    set(goal.year, goal.month - 1, 1)
                }
                val fromOk = dateFrom?.let { goalCal.timeInMillis >= it } ?: true
                val toOk   = dateTo?.let { goalCal.timeInMillis <= it } ?: true
                if (!fromOk || !toOk) continue

                val category = categories.find { it.firebaseId == goal.categoryId }
                val label    = category?.categoryName ?: "?"

                val allExpenses = expenseRepo.getExpensesByCategory(goal.categoryId)
                val monthSpend  = allExpenses.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.expenseDate }
                    c.get(Calendar.MONTH) + 1 == goal.month &&
                            c.get(Calendar.YEAR) == goal.year
                }.sumOf { it.expenseAmount }.toFloat()

                // Snooze check inside the loop per category
                val snoozedIds = snoozeRepo.getSnoozedExpenseIds(
                    goal.categoryId, currentMonth, currentYear
                )
                val isSnoozed = snoozedIds.isNotEmpty()

                rows.add(ChartRow(
                    label     = label,
                    min       = goal.minGoal?.toFloat() ?: 0f,
                    actual    = monthSpend,
                    max       = goal.maxGoal?.toFloat() ?: 0f,
                    isSnoozed = isSnoozed
                ))
            }

            if (rows.isEmpty()) {
                tvNoData.visibility  = View.VISIBLE
                barChart.visibility  = View.GONE
                pieChart.visibility  = View.GONE
                return@launch
            }

            tvNoData.visibility = View.GONE
            barChart.visibility = View.VISIBLE
            pieChart.visibility = View.VISIBLE

            // ==========================================
            //  COMPUTE AND POPULATE PIE CHART DATASET
            // ==========================================

            // Code attribution
            // This method was taken from GeeksforGeeks
            // Android - Create a Pie Chart with Kotlin
            // https://www.geeksforgeeks.org/kotlin/android-create-a-pie-chart-with-kotlin/
            // Adaptation note: adapted with Google Gemini

            val pieEntries = ArrayList<PieEntry>()
            rows.forEach { row ->
                // Only plot categories that have actual expenses accrued to avoid 0% zero-slices
                if (row.actual > 0f) {
                    pieEntries.add(PieEntry(row.actual, row.label))
                }
            }

            if (pieEntries.isNotEmpty()) {
                pieChart.visibility = View.VISIBLE
                val pieDataSet = PieDataSet(pieEntries, "").apply {
                    // Saturated palette sequence configuration mapping to match application theme design rules
                    colors = arrayListOf(
                        Color.parseColor("#0D47A1"),  // darkest blue
                        Color.parseColor("#1565C0"),  // deep blue
                        Color.parseColor("#1976D2"),  // mid blue
                        Color.parseColor("#42A5F5"),  // light blue
                        Color.parseColor("#90CAF9")   // pale blue
                    )
                    sliceSpace = 3f
                    selectionShift = 5f
                    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                }


                // Calculate the grand total of all current spending to run percentages manually
                val totalSpending = rows.sumOf { it.actual.toDouble() }.toFloat()

                val pieData = PieData(pieDataSet).apply {
                    // Custom formatter mathematically determines percent and binds it with Currency
                            setValueFormatter(object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    val percentage = (value / totalSpending) * 100
                                    // Displays on two lines: e.g. "R800" on top, "(61.5%)" underneath
                                    return "R${value.toInt()}\n(${String.format("%.1f", percentage)}%)"
                                }
                            })
                    //setValueFormatter(PercentFormatter(pieChart))
                    setValueTextSize(11f)
                    setValueTextColor(Color.BLACK)
                }

                pieChart.data = pieData
                pieChart.animateY(800)
                pieChart.invalidate()
            } else {
                // Hide view cleanly if data rows exist but no money was explicitly spent yet
                pieChart.visibility = View.GONE
            }


            //BAR CHART

            // Code attribution
            // This method was taken from GeeksforGeeks
            // Android - Create a BarChart with Kotlin
            // https://www.geeksforgeeks.org/kotlin/android-create-barchart-with-kotlin/

            //Build separate lists for min, actual and max bars

            val minEntries    = ArrayList<BarEntry>()
            val actualEntries = ArrayList<BarEntry>()
            val maxEntries    = ArrayList<BarEntry>()

            rows.forEachIndexed { i, row ->
                minEntries.add(BarEntry(i.toFloat(), row.min))
                actualEntries.add(BarEntry(i.toFloat(), row.actual))
                maxEntries.add(BarEntry(i.toFloat(), row.max))
            }

            // Min spend target - brand_deep_blue
            val minSet = BarDataSet(minEntries, "Min").apply {
                color = Color.parseColor("#176B87")
                valueTextSize = 9f
            }
            // Actual spend - red if over budget and not snoozed, light blue otherwise
            val actualSet = BarDataSet(actualEntries, "Actual").apply {
                colors = rows.map { row ->
                    if (row.max > 0 && row.actual > row.max && !row.isSnoozed)
                        Color.parseColor("#F44336")  // red for over budget
                    else
                        Color.parseColor("#42A5F5")  // light blue for under budget
                }
                valueTextSize = 9f
            }.also {
                //prevents icon drawing errors with actual values list that arises from multi-colour dataset
                it.setDrawIcons(false)
            }
            // Max spend limit - darkest blue
            val maxSet = BarDataSet(maxEntries, "Max").apply {
                color = Color.parseColor("#001C30")
                valueTextSize = 9f
            }

            // Group spacing configuration
            val groupSpace = 0.3f
            val barSpace   = 0.02f
            val barWidth   = 0.2f

            val data = BarData(minSet, actualSet, maxSet).apply {
                this.barWidth = barWidth
            }

            barChart.apply {
                this.data = data
                description.isEnabled = false


                // X axis shows category names centred under each bar group
                xAxis.apply {
                    valueFormatter        = IndexAxisValueFormatter(rows.map { it.label })
                    position              = XAxis.XAxisPosition.BOTTOM
                    granularity           = 1f
                    isGranularityEnabled  = true
                    setCenterAxisLabels(true)
                    textSize              = 11f
                    axisMinimum           = 0f
                    axisMaximum           = rows.size.toFloat()
                }

                axisRight.isEnabled  = false
                axisLeft.granularity = 500f

                // Custom legend entries to allow only one per bar type including the over-budget variant
                legend.apply {
                    isEnabled = true
                    setCustom(listOf(
                        com.github.mikephil.charting.components.LegendEntry().also {
                            it.label = "Min"
                            it.formColor = Color.parseColor("#176B87")
                        },
                        com.github.mikephil.charting.components.LegendEntry().also {
                            it.label = "Actual"
                            it.formColor = Color.parseColor("#42A5F5")
                        },
                        com.github.mikephil.charting.components.LegendEntry().also {
                            it.label = "Actual (Over Budget)"
                            it.formColor = Color.parseColor("#F44336")
                        },
                        com.github.mikephil.charting.components.LegendEntry().also {
                            it.label = "Max"
                            it.formColor = Color.parseColor("#001C30")
                        }
                    ))
                }
                groupBars(0f, groupSpace, barSpace)
                animateY(600)
                invalidate()
            }
        }
    }

    /*
        Shows a date picker dialog and returns the selected date via callback.
     */
    private fun showDatePicker(onDateSelected: (Int, Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> onDateSelected(y, m, d) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}