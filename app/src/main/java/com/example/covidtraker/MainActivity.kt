package com.example.covidtraker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import org.angmarch.views.NiceSpinner
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


private const val BASE_URL ="https://covidtracking.com/api/v1/"
private const val TAG="MainActivity"
private const val ALL_STATES = "All (Nationwide)"
class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adaptor: CovidSparkAdaptor
    private lateinit var perStatesDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_description)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
         val covidService = retrofit.create(CovidService::class.java)
        //fetch the national data
        covidService.getNationalData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val nationalData = response.body()
                if (nationalData==null){
                    Log.w(TAG,"did not receice a valid responds body")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG,"update graph with natioanl data")
                updateDisplayWithDate(nationalDailyData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }

        })
        covidService.getStstesData().enqueue(object: Callback<List<CovidData>>{
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val statesdata = response.body()
                if (statesdata==null){
                    Log.w(TAG,"did not receice a valid responds body")
                    return
                }
                perStatesDailyData = statesdata.reversed().groupBy { it.state }
                Log.i(TAG,"update spinner with states data")

                updateSpinnerwithStateData(perStatesDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }

        })
    }

    private fun updateSpinnerwithStateData(stateNames: Set<String>) {
        var stateAbbreciationList = stateNames.toMutableList()
        stateAbbreciationList.sort()
        stateAbbreciationList.add(0, ALL_STATES)

        var spinner:NiceSpinner = findViewById(R.id.spinner)
        spinner.attachDataSource(stateAbbreciationList)
        spinner.setOnSpinnerItemSelectedListener{parent,_,position,_->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStatesDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithDate(selectedData)

        }
    }

    private fun setupEventListeners() {
        var tickerView:TickerView = findViewById(R.id.tickerView)
        tickerView.setCharacterLists(TickerUtils.provideNumberList())
        val sparkView:SparkView = findViewById(R.id.sparkView)
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData->
            if(itemData is CovidData){
                updateInfoForDate(itemData)
            }
        }
        val RGTS: RadioGroup = findViewById(R.id.RadioGroupTimeSelection)
        RGTS.setOnCheckedChangeListener { _, checkedId ->
            adaptor.days = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adaptor.notifyDataSetChanged()
        }
        val RGMS: RadioGroup = findViewById(R.id.radioGroupMetricSelection)
        RGMS.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.radioButtonPos->updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonNeg->updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonDeath->updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        var sparkView: SparkView = findViewById(R.id.sparkView)
        var tcMetricLabel:TickerView = findViewById(R.id.tickerView)
        val colorRes = when(metric){
            Metric.DEATH->R.color.Death
            Metric.NEGATIVE->R.color.Neg_Case
            Metric.POSITIVE->R.color.Pos_Case
        }
        @ColorInt val colorInt= ContextCompat.getColor(this, colorRes)
        sparkView.lineColor =colorInt

        tcMetricLabel.setTextColor(colorInt)


        adaptor.metric=metric
        adaptor.notifyDataSetChanged()

        updateInfoForDate(currentlyShownData.last())
    }


    private fun updateDisplayWithDate(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
       val sparkView:SparkView = findViewById(R.id.sparkView)
        adaptor = CovidSparkAdaptor(dailyData)
        sparkView.adapter = adaptor
        val radioButtonPositive: RadioButton = findViewById(R.id.radioButtonPos)
        val radioButtonMax: RadioButton = findViewById(R.id.radioButtonMax)
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when(adaptor.metric){
            Metric.NEGATIVE->covidData.negativeIncrease
            Metric.DEATH->covidData.deathIncrease
            Metric.POSITIVE->covidData.positiveIncrease
        }
        val tvDateLabel: TextView = findViewById(R.id.textViewDateLabel)
        val tvMetricLabel: TickerView = findViewById(R.id.tickerView)
        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        tvDateLabel.text= outputDateFormat.format(covidData.dateChecked)

    }
}