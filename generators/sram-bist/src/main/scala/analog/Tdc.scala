package srambist.analog

import chisel3._
import chisel3.util.HasBlackBoxResource

class Tdc extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val a = Input(Bool())
    val b = Input(Bool())
    val reset_b = Input(Bool())
    val dout = Output(UInt(252.W))
  })

  override val desiredName = "tdc_64"
  addResource(s"/vsrc/$desiredName.v")
}
