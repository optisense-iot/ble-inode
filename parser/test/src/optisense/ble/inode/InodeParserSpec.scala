package optisense.ble.enode

import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import weaver.SimpleIOSuite

object MySuite extends SimpleIOSuite {

  pureTest("should parse example frame") {
    val rawData: BitVector =
      hex"929101b000001700a81900000400f4bbce6e77a00b97d1b5".toBitVector

    val parsed = InodeParser.CareSensor1.codec.decode(rawData)
    val expected = INodeCareSensorData(
      SensorFlags(false, false),
      145,
      BatteryInfo(100, 2.88),
      Alarms(false, false, false, false, false, false, false, false, false, false),
      Acceleration(false, 0.0, 0.0, -7.0),
      -30.0,
      0,
      310260,
      hex"0xce6e77a00b97d1b5".toBitVector.bytes,
    )

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expected
      )
    }
  }
}
