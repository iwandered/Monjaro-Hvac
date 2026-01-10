package com.seat.hvac

class IdNames {
    companion object {
        // ============== 基本消息类型 ==============
        val ERROR: Int = -1
        val TOAST: Int = -2
        val UPDATE_UI: Int = -3
        val IGNITION_STATE: Int = 1
        val AMBIENT_TEMPERATURE: Int = 2
        val INT_TEMPERATURE: Int = 3
        val ODOMETER: Int = 1005

        // ============== HVAC功能ID ==============
        // 空调电源相关
        val HVAC_FUNC_POWER: Int = 268501248  // 空调电源控制（根据您提供的信息）

        // 通用值常量
        const val COMMON_VALUE_OFF = 0
        const val COMMON_VALUE_ON = 1

        // 风量相关
        val HVAC_FUNC_FAN_SPEED: Int = 268566784
        val HVAC_FUNC_AUTO_FAN_SETTING: Int = 268567040
        val HVAC_FUNC_AUTO: Int = 268501504

        // 出风模式
        val HVAC_FUNC_BLOWING_MODE: Int = 268894464

        // 其他空调功能
        const val HVAC_FUNC_CIRCULATION = 268632320
        const val HVAC_FUNC_AC = 268501760
        const val HVAC_FUNC_TEMP_SET = 268828928
        const val HVAC_FUNC_SEAT_HEATING = 268763648
        const val HVAC_FUNC_SEAT_VENTILATION = 268763392
        const val HVAC_FUNC_SEAT_MASSAGE = 268764928
        const val HVAC_FUNC_G_CLEAN = 269485056

        // ============== 副驾驶屏幕控制功能ID ==============
        // 根据之前的分析，副驾驶屏幕开关功能ID
        const val FUNC_PSD_SCREEN_SWITCH: Int = 539495936
        // 雨刮器服务位置功能ID (新增)
        const val FUNC_WIPER_SERVICE_POSITION: Int = 537657600

        // 区域ID常量 - 添加类型转换
        const val ZONE_ALL: Int = -2147483648  // 0x80000000 的 Int 表示
        const val ZONE_PSD: Int = 4

        // ============== 循环模式常量 ==============
        const val CIRCULATION_AUTO = 268632323
        const val CIRCULATION_INNER = 268632321
        const val CIRCULATION_OUTSIDE = 268632322
        const val CIRCULATION_OFF = 0

        // ============== G-Clean状态常量 ==============
        const val G_CLEAN_OFF = 0
        const val G_CLEAN_ON = 1

        // ============== UI消息类型 ==============
        // 空调系统
        const val HVAC_FUNC_POWER_MSG = 1018  // 空调电源消息
        const val HVAC_FUNC_FAN_SPEED_MSG = 1009
        const val HVAC_FUNC_AUTO_FAN_SETTING_MSG = 1010
        const val HVAC_FUNC_AUTO_MSG = 1007
        const val HVAC_FUNC_BLOWING_MODE_MSG = 1011
        const val HVAC_FUNC_CIRCULATION_MSG = 1006
        const val HVAC_FUNC_AC_MSG = 1008
        const val HVAC_FUNC_G_CLEAN_MSG = 1015
        const val HVAC_FUNC_TEMP_LEFT = 1005

        // 座椅状态
        const val SEAT_HEATING_DRIVER = 1001
        const val SEAT_HEATING_PASSENGER = 1002
        const val SEAT_VENTILATION_DRIVER = 1003
        const val SEAT_VENTILATION_PASSENGER = 1004

        // 温度传感器
        const val INT_TEMPERATURE_MSG = 1012
        const val AMBIENT_TEMPERATURE_MSG = 1013

        // 座位占用状态
        const val PASSENGER_SEAT_OCCUPATION_MSG = 1014

        // 副驾驶屏幕状态
        const val PSD_STATUS_MSG = 1020
        const val PSD_STATUS_ON = 1
        const val PSD_STATUS_OFF = 0

        // ============== 座椅加热常量 ==============
        const val SEAT_HEATING_OFF = 0
        const val SEAT_HEATING_LEVEL_1 = 268763649
        const val SEAT_HEATING_LEVEL_2 = 268763650
        const val SEAT_HEATING_LEVEL_3 = 268763651

        // ============== 座椅通风常量 ==============
        const val SEAT_VENTILATION_OFF = 0
        const val SEAT_VENTILATION_LEVEL_1 = 268763393
        const val SEAT_VENTILATION_LEVEL_2 = 268763394
        const val SEAT_VENTILATION_LEVEL_3 = 268763395

        // ============== 座椅区域常量 ==============
        const val SEAT_ROW_1_LEFT = 1  // 驾驶员
        const val SEAT_ROW_1_RIGHT = 4 // 副驾驶
        const val SEAT_ROW_2_LEFT = 16
        const val SEAT_ROW_2_RIGHT = 64

        // 座椅区域常量
        const val SEAT_ZONE_FIRST_ROW_LEFT = 1
        const val SEAT_ZONE_FIRST_ROW_RIGHT = 4
        const val SEAT_ZONE_SECOND_ROW_LEFT = 16
        const val SEAT_ZONE_SECOND_ROW_RIGHT = 64

        // ============== 风量级别常量 ==============
        val FAN_SPEED_OFF: Int = 0
        val FAN_SPEED_LEVEL_1: Int = 268566785
        val FAN_SPEED_LEVEL_2: Int = 268566786
        val FAN_SPEED_LEVEL_3: Int = 268566787
        val FAN_SPEED_LEVEL_4: Int = 268566788
        val FAN_SPEED_LEVEL_5: Int = 268566789
        val FAN_SPEED_LEVEL_6: Int = 268566790
        val FAN_SPEED_LEVEL_7: Int = 268566791
        val FAN_SPEED_LEVEL_8: Int = 268566792
        val FAN_SPEED_LEVEL_9: Int = 268566793

        // ============== 自动风量设置常量 ==============
        val AUTO_FAN_SETTING_QUIETER: Int = 268567044   // A1 最静
        val AUTO_FAN_SETTING_SILENT: Int = 268567041    // A2
        val AUTO_FAN_SETTING_NORMAL: Int = 268567042    // A3 正常
        val AUTO_FAN_SETTING_HIGH: Int = 268567043      // A4
        val AUTO_FAN_SETTING_HIGHER: Int = 268567045    // A5 最强

        // ============== 出风方向常量 ==============
        val BLOWING_MODE_OFF: Int = 0
        val BLOWING_MODE_FACE: Int = 268894465
        val BLOWING_MODE_LEG: Int = 268894466
        val BLOWING_MODE_FACE_AND_LEG: Int = 268894467
        val BLOWING_MODE_FRONT_WINDOW: Int = 268894468
        val BLOWING_MODE_FACE_AND_FRONT_WINDOW: Int = 268894469
        val BLOWING_MODE_LEG_AND_FRONT_WINDOW: Int = 268894470
        val BLOWING_MODE_ALL: Int = 268894471
        val BLOWING_MODE_AUTO_SWITCH: Int = 268894472

        // ============== 转向灯相关 ==============
        const val LIGHT_LEFT_TURN = 2001
        const val LIGHT_RIGHT_TURN = 2002
        const val LIGHT_HAZARD_FLASHERS = 2006

        // BCM灯光功能ID
        const val BCM_FUNC_LIGHT_LEFT_TURN_SIGNAL = 553980160
        const val BCM_FUNC_LIGHT_RIGHT_TURN_SIGNAL = 553980416
        const val BCM_FUNC_LIGHT_HAZARD_FLASHERS = 553979648

        // ============== 灯光控制功能ID ==============
        const val SETTING_FUNC_LAMP_BENDINGLIGHT: Int = 537134336      // 转向辅助灯
        const val SETTING_FUNC_LAMP_COURTESY_LIGHT: Int = 537134592    // 迎宾灯
        const val SETTING_FUNC_LAMP_CORNERING_LIGHT: Int = 537135616   // 转弯灯

        // 灯光开关常量
        const val LIGHT_OFF: Int = 0
        const val LIGHT_ON: Int = 1

        // 灯光状态UI消息类型
        const val LAMP_BENDING_MSG = 2010
        const val LAMP_COURTESY_MSG = 2011
        const val LAMP_CORNERING_MSG = 2012

        // ============== 后视镜功能ID (修正) ==============
        // 根据IVehicle.java，倒车后视镜下翻的功能ID
        const val SETTING_FUNC_MIRROR_DIPPING = 537461504
        // 后视镜自动折叠功能ID（保持原状）
        const val SETTING_FUNC_MIRROR_AUTO_FOLDING = 537461248

        // ============== 后视镜下翻模式值 (新增) ==============
        // 根据IVehicle.java中的定义
        const val MIRROR_DIPPING_OFF = 0                    // 关闭
        const val MIRROR_DIPPING_DRIVER = 537461505        // 仅驾驶员侧下翻
        const val MIRROR_DIPPING_PASSENGER = 537461506     // 仅乘客侧下翻
        const val MIRROR_DIPPING_BOTH = 537461507          // 双面都下翻（默认）

        // 后视镜UI消息类型
        const val MIRROR_DIPPING_MSG = 2020
        const val MIRROR_FOLDING_MSG = 2021

        // ============== 车辆数据相关 ==============
        const val CAR_SPEED = 5
        const val SENSOR_RPM = 6
        const val FUEL_LEVEL = 4
        const val SENSOR_OIL_LEVEL = 7
        const val SENSOR_TYPE_GEAR = 12
        const val GEAR_MSG = 1016

        // 挡位常量
        const val GEAR_UNKNOWN = 0
        const val GEAR_PARK = 1
        const val GEAR_REVERSE = 2  // R档值
        const val GEAR_NEUTRAL = 4
        const val GEAR_DRIVE = 8
    }
}