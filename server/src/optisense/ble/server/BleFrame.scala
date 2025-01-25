package optisense.ble.server

import scodec.bits._
import scodec.bits.BitVector
import scodec.codecs._
import scodec.Codec

enum BleFrame {
  case Flags(bytes: BitVector)
  case ManufactureData(bytes: BitVector)
  case Unknown(bytes: BitVector)

  def bytes: BitVector
}

object BleFrame {

  object Flags {
    val tpe = 0x01.toInt
  }
  object ManufactureData {
    val tpe = 0xff.toInt
  }

  private def knownTypesCodec(length: Int) = discriminated[BleFrame]
    .by(uint8)
    .typecase(BleFrame.Flags.tpe, bits((length - 1) * 8).xmap[Flags](Flags.apply, _.bytes))
    .typecase(
      BleFrame.ManufactureData.tpe,
      bits((length - 1) * 8).xmap[ManufactureData](ManufactureData.apply, _.bytes),
    )

  val frameCodec: Codec[BleFrame] = uint8.consume { length =>
    discriminatorFallback(
      bits((length - 1) * 8).xmap[Unknown](Unknown.apply, _.bytes),
      knownTypesCodec(length),
    ).xmap(
      _.merge,
      {
        case u: BleFrame.Unknown => Left(u)
        case other               => Right(other)
      },
    )
  }(_.bytes.size.toInt)

  val framesCodec: Codec[Vector[BleFrame]] = vector(frameCodec)
}
