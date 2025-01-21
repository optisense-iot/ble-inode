package optisense.ble.enode

import cats.syntax.all._
import math._
import optisense.ble.enode.InodeParser.CareSensor1.flagsCodec
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
}

object INode {
  object CareSensor1 {

    val SensorType = 0x91.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        position: Acceleration,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    )
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
    )
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
    )
  }

  object CareSensorT {
    val SensorType = 0x9a.toInt

    case class Data(
        battery: BatteryInfo,
        alarms: Alarms,
        temperature: Double,
        time: Long,
        signature: ByteVector,
    )
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
    )
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
    )
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
    )
  }

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
