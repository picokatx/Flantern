package com.picobyte.flantern

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.picobyte.flantern.databinding.FragmentMessageGraphBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker


class MessageGraphFragment : Fragment() {
    val PUSH_CHARS = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMessageGraphBinding.inflate(inflater, container, false)
        val groupUID = arguments?.getString("group_uid")
        binding.messageDataGet.setOnClickListener {
            val day: Int = binding.messageDataDate.dayOfMonth
            val month: Int = binding.messageDataDate.month
            val year: Int = binding.messageDataDate.year
            val dayKey = getTimeKeysFromDate(day, month, year, null)
            for (i in 0 until 24) {
                val hourKey = getTimeKeysFromDate(day, month, year, i)
                Log.e("Flantern", "${dayKey.first} ${dayKey.second}")
                (context as MainActivity).rtDatabase.getReference("group_messages/$groupUID/live")
                    .orderByKey().startAt(hourKey.first).endAt(hourKey.second).get()
                    .addOnCompleteListener(context as MainActivity, object:  OnCompleteListener<DataSnapshot> {
                        override fun onComplete(p0: Task<DataSnapshot>) {
                            Log.e("Flantern", p0.result.children.count().toString())
                        }

                    })
            }

        }
        return binding.root
    }

    /*private fun decode(id: String) {
        val subId = id.substring(0,8);
        var timestamp = 0;
        for (var i=0; i < id.length; i++) {
            var c = id.charAt(i);
            timestamp = timestamp * 64 + PUSH_CHARS.indexOf(c);
        }
        return timestamp;
    }*/
    private fun getTimeKeysFromDate(
        day: Int,
        month: Int,
        year: Int,
        hour: Int?
    ): Pair<String, String> {
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()

        startTime.set(Calendar.YEAR, year)
        startTime.set(Calendar.MONTH, month)
        startTime.set(Calendar.DAY_OF_MONTH, day)
        startTime.set(Calendar.HOUR_OF_DAY, 0)
        startTime.set(Calendar.MINUTE, 0)
        startTime.set(Calendar.SECOND, 0)
        startTime.set(Calendar.MILLISECOND, 0)

        endTime.set(Calendar.YEAR, year)
        endTime.set(Calendar.MONTH, month)
        endTime.set(Calendar.DAY_OF_MONTH, day)
        endTime.set(Calendar.HOUR_OF_DAY, 23)
        endTime.set(Calendar.MINUTE, 59)
        endTime.set(Calendar.SECOND, 59)
        endTime.set(Calendar.MILLISECOND, 999)

        if (hour != null) {
            startTime.set(Calendar.HOUR_OF_DAY, hour)
            endTime.set(Calendar.HOUR_OF_DAY, hour)
        }

        var newTimeStamp = startTime.timeInMillis
        var startKey = ""
        for (i in 0 until 8) {
            val temp = newTimeStamp % 64
            newTimeStamp = (newTimeStamp - temp) / 64
            startKey += PUSH_CHARS[temp.toInt()]
        }

        newTimeStamp = endTime.timeInMillis
        var endKey = ""
        for (i in 0 until 8) {
            val temp = newTimeStamp % 64
            newTimeStamp = (newTimeStamp - temp) / 64
            endKey += PUSH_CHARS[temp.toInt()]
        }
        return Pair(startKey.reversed(), endKey.reversed())
    }

    /*private fun getData(data: Iterable<DataSnapshot>, startTime: Long, endTime: Long): LineData {
        val values: ArrayList<Entry> = ArrayList()
        var count: Int = 0
        var total = data.count()
        var temp = data.first().key!!
        var upper = temp.substring(0, 7) + temp[7].inc()
        for (i in data) {
            count += 1

            values.add(Entry(count, i.key!!))
        }

        // create a dataset and give it a type
        val set1 = LineDataSet(values, "DataSet 1")
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);
        set1.lineWidth = 1.75f
        set1.circleRadius = 5f
        set1.circleHoleRadius = 2.5f
        set1.color = Color.WHITE
        set1.setCircleColor(Color.WHITE)
        set1.highLightColor = Color.WHITE
        set1.setDrawValues(false)

        // create a data object with the data sets
        return LineData(set1)
    }*/

    private fun setupChart(chart: LineChart, data: LineData, color: Int) {
        (data.getDataSetByIndex(0) as LineDataSet).circleHoleColor = color
        chart.description.isEnabled = false

        // chart.setDrawHorizontalGrid(false);
        //
        // enable / disable grid background
        chart.setDrawGridBackground(false)
        //        chart.getRenderer().getGridPaint().setGridColor(Color.WHITE & 0x70FFFFFF);

        chart.setTouchEnabled(true)

        chart.isDragEnabled = true
        chart.setScaleEnabled(true)

        chart.setPinchZoom(false)
        chart.setBackgroundColor(color)

        chart.setViewPortOffsets(10f, 0f, 10f, 0f)

        chart.data = data

        val l = chart.legend
        l.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.axisLeft.spaceTop = 40f
        chart.axisLeft.spaceBottom = 40f
        chart.axisRight.isEnabled = false
        chart.xAxis.isEnabled = false

        chart.animateX(2500)
    }
}