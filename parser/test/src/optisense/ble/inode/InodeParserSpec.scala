package optisense.ble.enode

import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import weaver.SimpleIOSuite

object MySuite extends SimpleIOSuite {

  pureTest("should parse care sensor 1 data") {
    val rawData: BitVector =
      hex"92 91 01 b0 00 00 17 00 a8 19 00 00 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = InodeParser.codec.decode(rawData)
    val expected = INodeCareSensorData.CareSensor1(
      SensorFlags(false, false),
      CareSensorData(
        BatteryInfo(100, 2.88),
        Alarms(false, false, false, false, false, false, false, false, false, false),
        Acceleration(false, 0.0, 0.0, -7.0),
        -30.0,
        0,
        310260,
        hex"0xce6e77a00b97d1b5".toBitVector.bytes,
      ),
    )

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expected
      )
    }
  }

  pureTest("should parse care sensor 2 data") {
    val rawData: BitVector =
      hex"92 92 01 b0 00 00 17 00 a8 19 00 00 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = InodeParser.codec.decode(rawData)
    val expected = INodeCareSensorData.CareSensor2(
      SensorFlags(false, false),
      CareSensorData(
        BatteryInfo(100, 2.88),
        Alarms(false, false, false, false, false, false, false, false, false, false),
        Acceleration(false, 0.0, 0.0, -7.0),
        -30.0,
        0,
        310260,
        hex"0xce6e77a00b97d1b5".toBitVector.bytes,
      ),
    )

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expected
      )
    }
  }

  pureTest("should parse care sensor 3 data") {
    val rawData: BitVector =
      hex"92 93 01 b0 00 00 17 00 a8 19 e8 18 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = InodeParser.codec.decode(rawData)
    val expected =
      INodeCareSensorData.CareSensor3(
        SensorFlags(false, false),
        CareSensorData(
          BatteryInfo(100, 2.88),
          Alarms(false, false, false, false, false, false, false, false, false, false),
          Acceleration(false, 0.0, 0.0, -7.0),
          23.592441406249996,
          42.0,
          310260,
          hex"0xce6e77a00b97d1b5",
        ),
      )

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expected
      )
    }
  }
}
