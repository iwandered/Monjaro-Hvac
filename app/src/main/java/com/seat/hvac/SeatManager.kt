package com.seat.hvac

import android.util.Log

class SeatManager(private val climateThread: ClimateThreadManager.ClimateThread) {

    /**
     * 设置座椅加热
     * zone: 1=主驾驶, 4=副驾驶
     * level: 0=关闭, 1-3=档位
     */
    fun setSeatHeating(zone: Int, level: Int) {
        val actualZone = when (zone) {
            1 -> IdNames.SEAT_ROW_1_LEFT
            4 -> IdNames.SEAT_ROW_1_RIGHT
            16 -> IdNames.SEAT_ROW_2_LEFT
            64 -> IdNames.SEAT_ROW_2_RIGHT
            else -> IdNames.SEAT_ROW_1_LEFT
        }

        val mappedLevel = when (level) {
            0 -> IdNames.SEAT_HEATING_OFF
            1 -> IdNames.SEAT_HEATING_LEVEL_1
            2 -> IdNames.SEAT_HEATING_LEVEL_2
            3 -> IdNames.SEAT_HEATING_LEVEL_3
            else -> IdNames.SEAT_HEATING_OFF
        }

        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_SEAT_HEATING, actualZone, mappedLevel)
    }

    /**
     * 设置座椅通风
     * zone: 1=主驾驶, 4=副驾驶
     * level: 0=关闭, 1-3=档位
     */
    fun setSeatVentilation(zone: Int, level: Int) {
        val actualZone = when (zone) {
            1 -> IdNames.SEAT_ROW_1_LEFT
            4 -> IdNames.SEAT_ROW_1_RIGHT
            16 -> IdNames.SEAT_ROW_2_LEFT
            64 -> IdNames.SEAT_ROW_2_RIGHT
            else -> IdNames.SEAT_ROW_1_LEFT
        }

        val mappedLevel = when (level) {
            0 -> IdNames.SEAT_VENTILATION_OFF
            1 -> IdNames.SEAT_VENTILATION_LEVEL_1
            2 -> IdNames.SEAT_VENTILATION_LEVEL_2
            3 -> IdNames.SEAT_VENTILATION_LEVEL_3
            else -> IdNames.SEAT_VENTILATION_OFF
        }

        climateThread.setFunctionValueChecked(IdNames.HVAC_FUNC_SEAT_VENTILATION, actualZone, mappedLevel)
    }
}
