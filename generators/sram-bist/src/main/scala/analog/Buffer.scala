package srambist.analog

import chisel3._
import chisel3.util.HasBlackBoxResource

class Buffer extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val X = Output(Bool())
    val A = Input(Bool())
  })

  override val desiredName = "sky130_fd_sc_hd__bufbuf_16"
  addResource(s"/vsrc/$desiredName.v")
}

class BufferTree extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val X = Output(Bool())
    val A = Input(Bool())
  })

  override val desiredName = "buffer_tree"
  addResource(s"/vsrc/$desiredName.v")
}
