package srambist.sramharness

import chisel3._
import chisel3.util._

class SramHarnessParams(

  )

class SramHarness(params: SramHarnessParams) extends Module {
  val io = IO(new Bundle {
    val sramEn = Input(Bool())
    val bistWen = Input(Bool())
    val row = Input(UInt(10.W))
    val col = Input(UInt(3.W))
    val bistData = Input(64.W)
    val bistMask = Input(64.W)

    val we = Input(Bool())
    val addr = Input(UInt(10.W))
    val data = Input(64.W)
    val mask = Input(64.W)
  })

}
