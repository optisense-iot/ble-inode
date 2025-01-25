package optisense.ble.server

import optisense.ble.inode.INode
import optisense.ble.server.BleFrame
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import weaver.SimpleIOSuite

object BleParserSpec extends SimpleIOSuite {

  val expectedFlagsFrame = BleFrame.Flags(hex"06".toBitVector)
  val expectedManufactureFrame =
    BleFrame.ManufactureData(hex"94 9B FF E7 00 00 00 00 E6 19 D1 12 16 01 0C 18 5D 8B A1 E0 14 74 B8 14".toBitVector)

  pureTest("parse Flags frame") {
    val rawData: BitVector =
      hex"02 01 06".toBitVector

    val parsed = BleFrame.frameCodec.decode(rawData)

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expectedFlagsFrame
      )
    }
  }

  pureTest("parse Manufacture Data frame") {
    val rawData: BitVector =
      hex"19 FF 94 9B FF E7 00 00 00 00 E6 19 D1 12 16 01 0C 18 5D 8B A1 E0 14 74 B8 14".toBitVector

    val parsed = BleFrame.frameCodec.decode(rawData)

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == expectedManufactureFrame
      )
    }
  }

  pureTest("should parse multiple frames") {
    val rawData: BitVector =
      hex"02 01 06 19 FF 94 9B FF E7 00 00 00 00 E6 19 D1 12 16 01 0C 18 5D 8B A1 E0 14 74 B8 14".toBitVector

    val parsed = BleFrame.framesCodec.decode(rawData)

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data == Vector(expectedFlagsFrame, expectedManufactureFrame)
      )
    }
  }

}
