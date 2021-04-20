package com.example.covidtraker

enum class Metric{
    NEGATIVE,POSITIVE,DEATH
}
enum class TimeScale(val numDay:Int){
    WEEK(7),
    MONTH(30),
    MAX(-1)
}
