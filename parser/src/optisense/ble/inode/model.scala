package optisense.ble.enode

import cats.syntax.all._
import math._
import scodec._
import scodec.bits._
import scodec.codecs._

enum INode {
  case CareSensor1(flags: SensorFlags, data: INode.CareSensor1.Data)
  case CareSensor2(flags: SensorFlags, data: INode.CareSensor2.Data)
  case CareSensor3(flags: SensorFlags, data: INode.CareSensor3.Data)
  case CareSensorT(flags: SensorFlags, data: INode.CareSensorT.Data)
  case CareSensorHT(flags: SensorFlags, data: INode.CareSensorHT.Data)
  case CareSensorPT(flags: SensorFlags, data: INode.CareSensorPT.Data)
  case CareSensorPHT(flags: SensorFlags, data: INode.CareSensorPHT.Data)

  def flags: SensorFlags
  def data: INode.CommonData
}

object INode {

  trait CommonData {
    def temperature: Double
    def battery: BatteryInfo
    def alarms: Alarms
    def time: Long
    def signature: ByteVector
  }
  object CareSensor1 {

    val SensorType = 0x91.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        position: Acceleration,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensor2 {
    val SensorType = 0x92.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        position: Acceleration,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensor3 {
    val SensorType = 0x93.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        position: Acceleration,
        temperature: Double,
        humidity: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensorT {
    val SensorType = 0x9a.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensorHT {
    val SensorType = 0x9b.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        temperature: Double,
        humidity: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensorPT {

    val SensorType = 0x9c.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        pressure: Double,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

  object CareSensorPHT {

    val SensorType = 0x9d.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        pressure: Double,
        temperature: Double,
        humidity: Double,
        time: Long,
        signature: ByteVector,
    ) extends CommonData
  }

}

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
