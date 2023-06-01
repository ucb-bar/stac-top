package srambist.scanchain

import chisel3._
import chisel3.util._

class ScanIO(val n: Int) extends Bundle {
  val d: Vec[Bool] = Input(Vec(n, Bool()))
  val de: Vec[Bool] = Input(Vec(n, Bool()))
  val se: Bool = Input(Bool())
  val si: Bool = Input(Bool())
  val so: Bool = Output(Bool())
  val q: Vec[Bool] = Output(Vec(n, Bool()))
}

class ScanChain(val width: Int, val seed: Option[BigInt] = Some(0))
    extends Module {
  require(width > 0, s"Width must be greater than zero! (Found '$width')")

  val io: ScanIO = IO(new ScanIO(width))

  protected def resetValue: Vec[Bool] = seed match {
    case Some(s) => VecInit(s.U(width.W).asBools)
    case None    => WireDefault(Vec(width, Bool()), DontCare)
  }

  val state: Vec[Bool] = RegInit(resetValue)

  val scanNext = io.si +: state.dropRight(1)

  when(io.se) {
    state := scanNext
  }.otherwise {
    for (i <- 0 to width - 1) {
      when(io.de(i)) {
        state(i) := io.d(i)
      }
    }
  }

  io.q := state
  io.so := state(width - 1)
}
