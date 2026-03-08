package com.leekleak.trafficlight.model

object HoltWintersTripleExponential {
    fun forecast(
        y: LongArray, alpha: Double, beta: Double,
        gamma: Double, period: Int, m: Int
    ): DoubleArray {
        val seasons = y.size / period
        val a0 = calculateInitialLevel(y)
        val b0 = calculateInitialTrend(y, period)
        val initialSeasonalIndices = calculateSeasonalIndices(y, period, seasons)

        val forecast = calculateHoltWinters(
            y, a0, b0, alpha, beta, gamma,
            initialSeasonalIndices, period, m
        )

        return forecast.map { if (it.isNaN()) 0.0 else it }.toDoubleArray()
    }

    private fun calculateHoltWinters(
        y: LongArray,
        a0: Double,
        b0: Double,
        alpha: Double,
        beta: Double,
        gamma: Double,
        initialSeasonalIndices: DoubleArray,
        period: Int,
        m: Int,
    ): DoubleArray {
        val st = DoubleArray(y.size)
        val bt = DoubleArray(y.size)
        val it = DoubleArray(y.size)
        val ft = DoubleArray(y.size + m)


        //Initialize base values
        st[1] = a0
        bt[1] = b0

        for (i in 0..<period) {
            it[i] = initialSeasonalIndices[i]
        }

        ft[m] = (st[0] + (m * bt[0])) * it[0] //This is actually 0 since Bt[0] = 0
        ft[m + 1] = (st[1] + (m * bt[1])) * it[1] //Forecast starts from period + 2


        //Start calculations
        for (i in 2..<y.size) {
            //Calculate overall smoothing

            if ((i - period) >= 0) {
                st[i] = alpha * y[i] / it[i - period] + (1.0 - alpha) * (st[i - 1] + bt[i - 1])
            } else {
                st[i] = alpha * y[i] + (1.0 - alpha) * (st[i - 1] + bt[i - 1])
            }


            //Calculate trend smoothing
            bt[i] = gamma * (st[i] - st[i - 1]) + (1 - gamma) * bt[i - 1]


            //Calculate seasonal smoothing
            if ((i - period) >= 0) {
                it[i] = beta * y[i] / st[i] + (1.0 - beta) * it[i - period]
            }


            //Calculate forecast
            if (((i + m) >= period)) {
                ft[i + m] = (st[i] + (m * bt[i])) * it[i - period + m]
            }
        }

        return ft
    }

    private fun calculateInitialLevel(y: LongArray): Double = y[0].toDouble()

    private fun calculateInitialTrend(y: LongArray, period: Int): Double {
        var sum = 0.0

        for (i in 0..<period) {
            sum += (y[period + i] - y[i]).toDouble()
        }

        return sum / (period * period)
    }
    private fun calculateSeasonalIndices(y: LongArray, period: Int, seasons: Int): DoubleArray {
        val seasonalAverage = DoubleArray(seasons)
        val seasonalIndices = DoubleArray(period)

        val averagedObservations = DoubleArray(y.size)

        for (i in 0..<seasons) {
            for (j in 0..<period) {
                seasonalAverage[i] += y[(i * period) + j].toDouble()
            }
            seasonalAverage[i] /= period.toDouble()
        }

        for (i in 0..<seasons) {
            for (j in 0..<period) {
                if (seasonalAverage[i] != 0.0) {
                    averagedObservations[(i * period) + j] = y[(i * period) + j] / seasonalAverage[i]
                } else {
                    averagedObservations[(i * period) + j] = 0.0
                }
            }
        }

        for (i in 0..<period) {
            for (j in 0..<seasons) {
                seasonalIndices[i] += averagedObservations[(j * period) + i]
            }
            seasonalIndices[i] /= seasons.toDouble()
        }

        return seasonalIndices
    }
}
