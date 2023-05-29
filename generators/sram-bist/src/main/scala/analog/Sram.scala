package srambist.analog

import chisel3._

case class SramParams(
    maskGranularity: Int,
    muxRatio: Int,
    numWords: Int,
    dataWidth: Int
)

class Sram(params: SramParams) extends BlackBox {
  val io = IO(new Bundle {
    val we = Input(Bool())
    val wmask = Input(UInt((params.data_width / params.wmask_granularity).W))
    val addr = Input(UInt(log2Ceil(params.num_words).W))
    val din = Input(UInt(params.data_width.W))
    val saeMuxed = Input(Bool())
    val dout = Output(UInt(params.data_width.W))
    val saeInt = Output(Bool())
  })
}
