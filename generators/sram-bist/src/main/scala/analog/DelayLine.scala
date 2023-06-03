package srambist.analog

import chisel3._
import chisel3.util.HasBlackBoxResource

class DelayLine extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk_in = Input(Bool())
    val ctl = Input(UInt(128.W))
    val ctl_b = Input(UInt(128.W))
    val clk_out = Output(Bool())
  })
  override val desiredName = "tristate_inv_delay_line_128"
  addResource(s"/vsrc/$desiredName.v")
}
