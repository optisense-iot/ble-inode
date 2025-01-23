package optisense.ble.enode

import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import weaver.SimpleIOSuite

object INodeParserSpec extends SimpleIOSuite {

  pureTest("should parse care sensor 1 data") {
    val rawData: BitVector =
      hex"92 91 01 b0 00 00 17 00 a8 19 00 00 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = INodeParser.codec.decode(rawData)
    val expected = INode.CareSensor1(
      SensorFlags(false, false),
      INode.CareSensor1.Data(
        BatteryInfo(100, 2.88),
        Alarms(false, false, false, false, false, false, false, false, false, false),
        Acceleration(false, 0.0, 0.0, -7.0),
        -30.0,
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

    val parsed = INodeParser.codec.decode(rawData)
    val expected = INode.CareSensor2(
      SensorFlags(false, false),
      INode.CareSensor2.Data(
        BatteryInfo(100, 2.88),
        Alarms(false, false, false, false, false, false, false, false, false, false),
        Acceleration(false, 0.0, 0.0, -7.0),
        -30.0,
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

    val parsed = INodeParser.codec.decode(rawData)
    val expected =
      INode.CareSensor3(
        SensorFlags(false, false),
        INode.CareSensor3.Data(
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

  pureTest("should parse care sensor T data") {
    val rawData: BitVector =
      hex"92 9A 01 b0 00 00 00 00 a8 19 00 00 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = INodeParser.codec.decode(rawData)
    val expected =
      INode.CareSensorT(
        SensorFlags(false, false),
        INode.CareSensorT.Data(
          BatteryInfo(100, 2.88),
          Alarms(false, false, false, false, false, false, false, false, false, false),
          -30.0,
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

  pureTest("should parse care sensor HT data") {
    val rawData: BitVector =
      hex"92 9B 01 b0 00 00 00 00 a8 19 00 00 04 00 f4 bb ce 6e 77 a0 0b 97 d1 b5".toBitVector

    val parsed = INodeParser.codec.decode(rawData)
    val expected =
      INode.CareSensorHT(
        SensorFlags(false, false),
        INode.CareSensorHT.Data(
          BatteryInfo(100, 2.88),
          Alarms(false, false, false, false, false, false, false, false, false, false),
          23.592441406249996,
          1.0,
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

  pureTest("should parse care sensor PT data") {
    val rawData: BitVector =
      hex"12 9C 00 B0 00 00 4E 3E 40 D8 00 00 00 00 AC 53 54 65 11 26 68 75 16 3A".toBitVector

    val parsed = INodeParser.codec.decode(rawData)
    val expected =
      INode.CareSensorPT(
        SensorFlags(false, false),
        INode.CareSensorPT.Data(
          BatteryInfo(100, 2.88),
          Alarms(false, false, false, false, false, false, false, false, false, false),
          996.0,
          157.5,
          21420,
          hex"0x546511266875163a",
        ),
      )

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expected
      )
    }
  }
}
