package hu.bme.solarboat.onboardsystem.jsonData


import com.google.gson.annotations.SerializedName

data class ArduinoData(
    val acceleration: Acceleration,
    val battery: Battery,
    val compass: Compass,
    val error: Error,
    @SerializedName("extra_temps")
    val extraTemps: List<Int>,
    val motor: Motor,
    val tilt: Tilt
) {
    data class Acceleration(
        val x: Double,
        val y: Double,
        val z: Double
    )

    data class Battery(
        @SerializedName("in")
        val inX: Int,
        val `out`: Int,
        @SerializedName("SoC")
        val soC: Int,
        val temp: Int
    )

    data class Compass(
        val x: Int,
        val y: Int,
        val z: Int
    )

    data class Error(
        val message: String,
        val source: String
    )

    data class Motor(
        @SerializedName("RpM")
        val rpM: Int,
        val temp: Int
    )

    data class Tilt(
        val x: Double,
        val y: Double,
        val z: Double
    )
}