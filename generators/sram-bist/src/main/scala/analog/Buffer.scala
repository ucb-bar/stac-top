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

class BufferTree extends Module {
  val io = IO(new Bundle {
    val X = Output(Bool())
    val A = Input(Bool())
  })

  val buf1 = Module(new Buffer)
  buf1.io.A := io.A

  for (i <- 1 to 4) {
    val buf2 = Module(new Buffer)
    buf2.io.A := buf1.io.X
    io.X := buf2.io.X
  }
}
