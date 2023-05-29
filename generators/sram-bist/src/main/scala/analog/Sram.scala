package srambist.analog

import chisel3._
import chisel3.util._

case class SramParams(
    maskGranularity: Int,
    muxRatio: Int,
    numWords: Int,
    dataWidth: Int
)

class Sram(params: SramParams) extends BlackBox {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val wmask = Input(UInt((params.dataWidth / params.maskGranularity).W))
    val addr = Input(UInt(log2Ceil(params.numWords).W))
    val din = Input(UInt(params.dataWidth.W))
    val saeMuxed = Input(Bool())
    val dout = Output(UInt(params.dataWidth.W))
    val saeInt = Output(Bool())
  })
}
