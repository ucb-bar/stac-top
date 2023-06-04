package chipyard.sky130

import chisel3._

object Sky130FDLevelShifters {
  object defaults {
    val hv2lv = "sky130_fd_sc_hvl__lsbufhv2lv_1"
    val lv2hv = "sky130_fd_sc_hvl__lsbuflv2hv_1"
  }
}

class Sky130FDLevelShifterIO extends Bundle {
  val A = Input(Bool())
  val X = Output(Bool())
}

class Sky130FDLevelShifter(cellName: String) extends BlackBox {
  val io = IO(new Sky130FDLevelShifterIO)

  val in = io.A
  val out = io.X

  override val desiredName = cellName
}
