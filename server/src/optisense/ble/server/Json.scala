package optisense.ble.server

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import optisense.ble.enode.INode
import scodec.bits.ByteVector

import java.util.HexFormat

object Json {
  given JsonValueCodec[ByteVector] = new JsonValueCodec {

    override def decodeValue(in: JsonReader, default: ByteVector): ByteVector = {
      val str = in.readString("")
      ByteVector
        .fromHex(str)
        .getOrElse(throw IllegalArgumentException(s"invalid hex string: $str"))
    }

    override def encodeValue(x: ByteVector, out: JsonWriter): Unit =
      out.writeVal(x.toHex)

    override def nullValue: ByteVector = ByteVector.empty

  }
  given JsonValueCodec[FlatSensorData] = JsonCodecMaker.make

}
