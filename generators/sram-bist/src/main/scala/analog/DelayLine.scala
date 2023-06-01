package srambist.analog

import chisel3._

class DelayLine extends BlackBox {
  val io = IO(new Bundle {
    val clk_in = Input(Bool())
    val ctl = Input(UInt(128.W))
    val ctl_b = Input(UInt(128.W))
    val clk_out = Output(Bool())
  })
}
