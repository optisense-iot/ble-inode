package optisense.ble.enode

import math._
import scodec._
import scodec.bits._
import scodec.codecs._

case class INodeCareSensorData(
    flags: SensorFlags,   // Flagi (rtto, lowBattery)
    sensorType: Int,      // Typ sensora (0x91 dla Care Sensor #1)
    battery: BatteryInfo, // Grupy i napięcie baterii
    alarms: Alarms,       // Alarmy (w postaci bitowej)
    rawPosition: Acceleration,
    rawTemperature: Double, // Surowa temperatura (skalibrowana poniżej)
    rawUnused: Int,
    time: Long,
    signature: ByteVector, // Cyfrowy podpis AES128
)

case class Acceleration(
    motion: Boolean, // Ruch (detekcja ruchu)
    accX: Double,    // Przyspieszenie w osi X
    accY: Double,    // Przyspieszenie w osi Y
    accZ: Double,    // Przyspieszenie w osi Z
)

case class BatteryInfo(
    level: Int,     // Poziom baterii (%)
    voltage: Double, // Napięcie baterii (V)
)

case class SensorFlags(rtto: Boolean, lowBattery: Boolean)

case class Battery(batteryLevel: Int, batteryVoltage: Double)

case class Alarms(
    moveAccelerometer: Boolean,
    levelAccelerometer: Boolean,
    levelTemperature: Boolean,
    levelHumidity: Boolean,
    contactChange: Boolean,
    moveStopped: Boolean,
    moveGTimer: Boolean,
    levelAccelerometerChange: Boolean,
    levelMagnetChange: Boolean,
    levelMagnetTimer: Boolean,
)

object InodeParser {

  object CareSensor1 {
    val temperatureCodec: Codec[Double] = uint16L.xmap(
      rawT => {
        val temp = if (rawT > 127) rawT - 8192 else rawT
        max(min(temp.toDouble, 70), -30)
      },
      temp => {
        val clampedTemp = max(min(temp, 70), -30)
        if (clampedTemp < 0) (clampedTemp + 8192).toInt else clampedTemp.toInt
      },
    )

    val accelerationCodec: Codec[Acceleration] = uint16L.xmap(
      rawP => {
        val motion = (rawP & 0x8000) != 0
        val accX   = adjustAcceleration((rawP >> 10) & 0x1f)
        val accY   = adjustAcceleration((rawP >> 5) & 0x1f)
        val accZ   = adjustAcceleration(rawP & 0x1f)
        Acceleration(motion, accX, accY, accZ)
      },
      acc => {
        val motionBit = if (acc.motion) 0x8000 else 0
        val rawAccX   = inverseAdjustAcceleration(acc.accX) << 10
        val rawAccY   = inverseAdjustAcceleration(acc.accY) << 5
        val rawAccZ   = inverseAdjustAcceleration(acc.accZ)
        motionBit | rawAccX | rawAccY | rawAccZ
      },
    )

    def adjustAcceleration(raw: Int): Double =
      if ((raw & 0x10) != 0) -(raw & 0xf) else raw.toDouble

    def inverseAdjustAcceleration(acc: Double): Int =
      if (acc < 0) (acc.abs.toInt & 0xf) | 0x10 else acc.toInt

    val rawValueCodec: Codec[Int] = uint16L

    val signatureCodec: Codec[ByteVector] = bytes(8)

    val batteryCodec: Codec[BatteryInfo] = uint16L.xmap(
      rawBattery => {
        val batteryLevel = (rawBattery >> 12) & 0xf match {
          case 1     => 100
          case level => 10 * (min(level, 11) - 1)
        }
        val voltage = (batteryLevel - 10) * 1.2 / 100 + 1.8
        BatteryInfo(batteryLevel, voltage)
      },
      battery => {
        val batteryBits = ((battery.level / 10) + 1) << 12
        batteryBits
      },
    )
    // 1. Flagi (1 bajt)
    val flagsCodec: Codec[SensorFlags] = byte.xmap(
      flags =>
        SensorFlags(
          rtto = (flags & 0x04) != 0,
          lowBattery = (flags & 0x08) != 0,
        ),
      flags => {
        var byte = 0
        if (flags.rtto) byte |= 0x04
        if (flags.lowBattery) byte |= 0x08
        byte.toByte
      },
    )
    // 2. Typ sensora (1 bajt)
    val sensorTypeCodec: Codec[Int] = uint8L

    // Time Codec
    val timeCodec: Codec[Long] = (uint16L :: uint16L).xmap(
      { case (t1, t2) => (t1.toLong << 16) | t2.toLong },
      time => ((time >> 16).toInt, (time & 0xffff).toInt),
    )

    // 4. Alarmy (2 bajty, uint16le)
    val alarmsCodec: Codec[Alarms] = uint16L.xmap(
      value =>
        Alarms(
          moveAccelerometer = (value & 0x01) != 0,
          levelAccelerometer = (value & 0x02) != 0,
          levelTemperature = (value & 0x04) != 0,
          levelHumidity = (value & 0x08) != 0,
          contactChange = (value & 0x10) != 0,
          moveStopped = (value & 0x20) != 0,
          moveGTimer = (value & 0x40) != 0,
          levelAccelerometerChange = (value & 0x80) != 0,
          levelMagnetChange = (value & 0x100) != 0,
          levelMagnetTimer = (value & 0x200) != 0,
        ),
      alarms => {
        var value = 0
        if (alarms.moveAccelerometer) value |= 0x01
        if (alarms.levelAccelerometer) value |= 0x02
        if (alarms.levelTemperature) value |= 0x04
        if (alarms.levelHumidity) value |= 0x08
        if (alarms.contactChange) value |= 0x10
        if (alarms.moveStopped) value |= 0x20
        if (alarms.moveGTimer) value |= 0x40
        if (alarms.levelAccelerometerChange) value |= 0x80
        if (alarms.levelMagnetChange) value |= 0x100
        if (alarms.levelMagnetTimer) value |= 0x200
        value
      },
    )

    val codec: Codec[INodeCareSensorData] =
      (flagsCodec :: sensorTypeCodec :: batteryCodec :: alarmsCodec ::
        accelerationCodec :: temperatureCodec :: uint16L :: timeCodec :: signatureCodec).as[INodeCareSensorData]
  }
}
