package com.example.covidtraker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdaptor(private  val dailyData: List<CovidData>) :SparkAdapter() {
   var metric = Metric.POSITIVE
    var days = TimeScale.MAX

    override fun getCount() =dailyData.size

    override fun getItem(index: Int) =  dailyData[index]

    override fun getY(index: Int): Float {
        val chosenDailyData = dailyData[index]
        return when(metric){
            Metric.POSITIVE->chosenDailyData.positiveIncrease.toFloat()
            Metric.NEGATIVE->chosenDailyData.negativeIncrease.toFloat()
            Metric.DEATH->chosenDailyData.deathIncrease.toFloat()
        }

    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (days != TimeScale.MAX) {
            bounds.left = count - days.numDay.toFloat()
        }
        return bounds
    }

}
