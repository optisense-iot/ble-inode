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
      hex"0215a7ae2eb3cf45403e8e37017b6780af120c002e9b".toBitVector
    val parsed = BleCodec.bleCodec.decode(rawData)

    matches(parsed) { case Successful(DecodeResult(data, _)) =>
      expect(
        data ==
          BleData("0215a7ae2eb3cf45403e8e37017b6780", 4481.8, 307.2)
      )
    }

  }
}
