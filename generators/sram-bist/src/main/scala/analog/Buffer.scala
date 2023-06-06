package srambist.analog

import chisel3._
import chisel3.util.HasBlackBoxResource

class BufferTree extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val X = Output(Bool())
    val A = Input(Bool())
  })

  override val desiredName = "buffer_tree"
  addResource(s"/vsrc/$desiredName.v")
}
