package srambist.analog

import chisel3._

class Tdc extends BlackBox {
  val io = IO(new Bundle {
    val a = Input(Bool())
    val b = Input(Bool())
    val reset_b = Input(Bool())
    val dout = Output(UInt(252.W))
  })
}
