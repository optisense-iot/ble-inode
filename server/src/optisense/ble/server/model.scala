package optisense.ble.server

import cats.syntax.all._
import optisense.ble.enode.Acceleration
import optisense.ble.enode.INode
import scodec.bits.ByteVector

case class FlatSensorData(
    `type`: String,
    temperature: Double,
    humidity: Option[Double],
    pressure: Option[Double],
    alarmMoveAccelerometer: Boolean,
    alarmLevelAccelerometer: Boolean,
    alarmLevelTemperature: Boolean,
    alarmLevelHumidity: Boolean,
    alarmContactChange: Boolean,
    alarmMoveStopped: Boolean,
    alarmMoveGTimer: Boolean,
    alarmLevelAccelerometerChange: Boolean,
    alarmLevelMagnetChange: Boolean,
    alarmLevelMagnetTimer: Boolean,
    positionMotion: Option[Boolean],
    positionAccX: Option[Double],
    positionAccY: Option[Double],
    positionAccZ: Option[Double],
    batteryInfoLevel: Int,
    batteryInfoVoltage: Double,
    time: Long,
    rtto: Boolean,
    lowBattery: Boolean,
    signature: ByteVector
) {
  def withPosition(position: Acceleration): FlatSensorData =
    copy(
      positionMotion = position.motion.some,
      positionAccX = position.accX.some,
      positionAccY = position.accY.some,
      positionAccZ = position.accZ.some,
    )
}

object FlatSensorData {
  private def base(inode: INode): String => FlatSensorData =
    tpe =>
      FlatSensorData(
        `type` = tpe,
        temperature = inode.data.temperature,
        humidity = None,
        pressure = None,
        alarmMoveAccelerometer = inode.data.alarms.moveAccelerometer,
        alarmLevelAccelerometer = inode.data.alarms.levelAccelerometer,
        alarmLevelTemperature = inode.data.alarms.levelTemperature,
        alarmLevelHumidity = inode.data.alarms.levelHumidity,
        alarmContactChange = inode.data.alarms.contactChange,
        alarmMoveStopped = inode.data.alarms.moveStopped,
        alarmMoveGTimer = inode.data.alarms.moveGTimer,
        alarmLevelAccelerometerChange = inode.data.alarms.levelAccelerometerChange,
        alarmLevelMagnetChange = inode.data.alarms.levelMagnetChange,
        alarmLevelMagnetTimer = inode.data.alarms.levelMagnetTimer,
        positionMotion = None,
        positionAccX = None,
        positionAccY = None,
        positionAccZ = None,
        batteryInfoLevel = inode.data.battery.level,
        batteryInfoVoltage = inode.data.battery.voltage,
        time = inode.data.time,
        rtto = inode.flags.rtto,
        lowBattery = inode.flags.lowBattery,
        signature = inode.data.signature
      )

  def from(inode: INode): FlatSensorData =
    inode match {
      case INode.CareSensor1(flags, data) =>
        base(inode)("1").withPosition(data.position)
      case INode.CareSensor2(flags, data) =>
        base(inode)("2").withPosition(data.position)
      case INode.CareSensor3(flags, data) =>
        base(inode)("3").withPosition(data.position).copy(humidity = data.humidity.some)
      case INode.CareSensorT(flags, data) =>
        base(inode)("T")
      case INode.CareSensorHT(flags, data) =>
        base(inode)("HT").copy(humidity = data.humidity.some)
      case INode.CareSensorPT(flags, data) =>
        base(inode)("PT").copy(pressure = data.pressure.some)
      case INode.CareSensorPHT(flags, data) =>
        base(inode)("PHT").copy(pressure = data.pressure.some, humidity = data.humidity.some)
    }

}
