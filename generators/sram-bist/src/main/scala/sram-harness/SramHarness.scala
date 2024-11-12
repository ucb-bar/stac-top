package srambist.sramharness

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.util.ClockGate

import srambist.WithChiseltestSramsKey

case class SramHarnessParams(
    rowWidth: Int,
    colWidth: Int,
    dataWidth: Int,
    maskWidth: Int
)

class SramHarness(params: SramHarnessParams)(implicit p: Parameters)
    extends Module {

  val io = IO(new Bundle {
    val inRow = Input(UInt(11.W))
    val inCol = Input(UInt(3.W))
    val inData = Input(UInt(128.W))
    val inMask = Input(UInt(128.W))

    val addr = Output(UInt((params.rowWidth + params.colWidth).W))
    val data = Output(UInt(params.dataWidth.W))
    val mask = Output(UInt(params.maskWidth.W))
  })

  io.addr := Cat(
    io.inRow(params.rowWidth - 1, 0),
    io.inCol(params.colWidth - 1, 0)
  )
  io.data := io.inData(params.dataWidth - 1, 0)
  io.mask := io.inMask(params.maskWidth - 1, 0)
}
