package optisense.ble.enode

import cats.syntax.all._
import math._
import optisense.ble.enode.InodeParser.CareSensor1.flagsCodec
import scodec._
import scodec.bits._
import scodec.codecs._

enum INode {
  case CareSensor1(flags: SensorFlags, data: CareSensorData)
  case CareSensor2(flags: SensorFlags, data: CareSensorData)
  case CareSensor3(flags: SensorFlags, data: CareSensorData)
  case CareSensorT(flags: SensorFlags, data: CareSensorData)

  def flags: SensorFlags
}

case class CareSensorData(
    battery: BatteryInfo,
    alarms: Alarms,
    position: Acceleration,
    temperature: Double,
    humidity: Double,
    time: Long,
    signature: ByteVector,
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

  // 2. Typ sensora (1 bajt)
  val sensorTypeCodec: Codec[Int] = uint8L

  object CareSensor1 {

    val SensorType = 0x91.toInt

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

    val humidityCodec = uint16L.xmap(_.toDouble, _.toInt)

    val codec: Codec[CareSensorData] =
      (batteryCodec :: alarmsCodec ::
        accelerationCodec :: temperatureCodec :: humidityCodec :: timeCodec :: signatureCodec).as[CareSensorData]

    val fullCodec: Codec[INode.CareSensor1] = (flagsCodec :: sensorTypeCodec :: codec)
      .xmap(
        { case (f, _, c) => INode.CareSensor1(f, c) },
        cs => (cs.flags, SensorType, cs.data),
      )

  }

  object CareSensor2 {
    val SensorType = 0x92.toInt

    val temperatureCodec: Codec[Double] = (uint8L :: uint8L).xmap(
      (msb, lsb) => {
        var temperature = msb * 0.0625 + 16 * (lsb & 0x0f)

        // Check for negative adjustment
        if ((lsb & 0x10) != 0) {
          temperature -= 256
        }

        // Clamp the value within the allowed range
        if (temperature < -30) {
          temperature = -30
        } else if (temperature > 70) {
          temperature = 70
        }

        temperature
      },
      { temp =>
        // Clamp the temperature within the range
        var clampedTemp = math.max(math.min(temp, 70), -30)

        // Calculate lsb and msb based on temperature
        val msb = ((clampedTemp + 30) / 0.0625).toInt & 0xff
        val lsb = (clampedTemp - msb * 0.0625) * 16 + 0.5 // Round to nearest integer

        // Encode the MSB and LSB as uint8
        (msb.toByte, lsb.toByte)
      },
    )
    val codec: Codec[CareSensorData] =
      (CareSensor1.batteryCodec :: CareSensor1.alarmsCodec ::
        CareSensor1.accelerationCodec :: CareSensor2.temperatureCodec :: CareSensor1.humidityCodec :: CareSensor1.timeCodec :: CareSensor1.signatureCodec)
        .as[CareSensorData]

    val fullCodec: Codec[INode.CareSensor2] = (flagsCodec :: sensorTypeCodec :: codec)
      .xmap(
        { case (f, _, c) => INode.CareSensor2(f, c) },
        cs => (cs.flags, SensorType, cs.data),
      )
  }

  object CareSensor3 {
    val SensorType = 0x93.toInt

    val temperatureCodec: Codec[Double] = uint16L.xmap(
      rawValue => {
        val temp = (175.72 * rawValue * 4 / 65536) - 46.85
        if (temp < -30) -30
        else if (temp > 70) 70
        else temp
      },
      { temp =>
        // Encode logic: Convert temperature to rawValue
        val clampedTemp = if (temp < -30) -30 else if (temp > 70) 70 else temp

        // Reverse the formula: rawValue = ((clampedTemp + 46.85) * 65536) / (175.72 * 4)
        val rawValue = ((clampedTemp + 46.85) * 65536 / (175.72 * 4)).toInt

        // Ensure the rawValue fits in 16 bits
        rawValue & 0xffff
      },
    )

    val humidityCodec: Codec[Double] = uint16L.xmap(
      { rawValue =>
        val h = (125 * rawValue * 4 / 65536) - 6
        if h < 1 then 1
        else if h > 100 then 100
        else h
      },
      { humidity =>
        // Clamp the humidity within the valid range [1, 100]
        val clampedHumidity = if humidity < 1 then 1 else if humidity > 100 then 100 else humidity

        // Reverse the formula: rawValue = ((clampedHumidity + 6) * 65536) / (125 * 4)
        val rawValue = ((clampedHumidity + 6) * 65536 / (125 * 4)).toInt

        // Ensure the rawValue fits in 16 bits
        rawValue & 0xffff
      },
    )
    val codec: Codec[CareSensorData] =
      (CareSensor1.batteryCodec :: CareSensor1.alarmsCodec ::
        CareSensor1.accelerationCodec :: temperatureCodec :: humidityCodec :: CareSensor1.timeCodec :: CareSensor1.signatureCodec)
        .as[CareSensorData]

    val fullCodec: Codec[INode.CareSensor3] = (flagsCodec :: sensorTypeCodec :: codec)
      .xmap(
        { case (f, _, c) => INode.CareSensor3(f, c) },
        cs => (cs.flags, SensorType, cs.data),
      )

  }

  object CareSensorT {
    val SensorType = 0x9a.toInt
    val codec: Codec[CareSensorData] =
      (CareSensor1.batteryCodec :: CareSensor1.alarmsCodec ::
        CareSensor1.accelerationCodec :: CareSensor2.temperatureCodec :: CareSensor1.humidityCodec :: CareSensor1.timeCodec :: CareSensor1.signatureCodec)
        .as[CareSensorData]

    val fullCodec: Codec[INode.CareSensorT] = (flagsCodec :: sensorTypeCodec :: codec)
      .xmap(
        { case (f, _, c) => INode.CareSensorT(f, c) },
        cs => (cs.flags, SensorType, cs.data),
      )
  }

  private def careSensor1Codec(flags: SensorFlags): Codec[INode.CareSensor1] =
    (provide(flags) :: CareSensor1.codec)
      .xmap(INode.CareSensor1.apply, data => (data.flags, data.data))

  private def careSensor2Codec(flags: SensorFlags): Codec[INode.CareSensor2] =
    (provide(flags) :: CareSensor2.codec)
      .xmap(INode.CareSensor2.apply, data => (data.flags, data.data))

  private def careSensor3Codec(flags: SensorFlags): Codec[INode.CareSensor3] =
    (provide(flags) :: CareSensor3.codec)
      .xmap(INode.CareSensor3.apply, data => (data.flags, data.data))

  private def careSensorTCodec(flags: SensorFlags): Codec[INode.CareSensorT] =
    (provide(flags) :: CareSensorT.codec)
      .xmap(INode.CareSensorT.apply, data => (data.flags, data.data))

  val codec: Codec[INode] =
    flagsCodec
      .consume(flags =>
        discriminated[INode]
          .by(sensorTypeCodec)
          .typecase(CareSensor1.SensorType, careSensor1Codec(flags))
          .typecase(CareSensor2.SensorType, careSensor2Codec(flags))
          .typecase(CareSensor3.SensorType, careSensor3Codec(flags))
          .typecase(CareSensorT.SensorType, careSensorTCodec(flags))
          .tuple
      )(_._1.flags)
      .xmap(_._1, Tuple(_))
}
