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

        return forecast
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
        val St = DoubleArray(y.size)
        val Bt = DoubleArray(y.size)
        val It = DoubleArray(y.size)
        val Ft = DoubleArray(y.size + m)


        //Initialize base values
        St[1] = a0
        Bt[1] = b0

        for (i in 0..<period) {
            It[i] = initialSeasonalIndices[i]
        }

        Ft[m] = (St[0] + (m * Bt[0])) * It[0] //This is actually 0 since Bt[0] = 0
        Ft[m + 1] = (St[1] + (m * Bt[1])) * It[1] //Forecast starts from period + 2


        //Start calculations
        for (i in 2..<y.size) {
            //Calculate overall smoothing

            if ((i - period) >= 0) {
                St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1])
            } else {
                St[i] = alpha * y[i] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1])
            }


            //Calculate trend smoothing
            Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1]


            //Calculate seasonal smoothing
            if ((i - period) >= 0) {
                It[i] = beta * y[i] / St[i] + (1.0 - beta) * It[i - period]
            }


            //Calculate forecast
            if (((i + m) >= period)) {
                Ft[i + m] = (St[i] + (m * Bt[i])) * It[i - period + m]
            }
        }

        return Ft
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
