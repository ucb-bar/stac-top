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

object SaeSrc extends ChiselEnum {
  val int = Value(0.U(2.W))
  val clk = Value(1.U(2.W))
  val ext = Value(2.U(2.W))
}

class SramHarness(params: SramHarnessParams)(implicit p: Parameters)
    extends Module {

  val io = IO(new Bundle {
    val sramEn = Input(Bool())
    val inRow = Input(UInt(10.W))
    val inCol = Input(UInt(3.W))
    val inData = Input(UInt(64.W))
    val inMask = Input(UInt(64.W))
    val saeInt = Input(Bool())
    val saeSel = Input(SaeSrc())
    val saeClk = Input(Bool())
    val delayLineIn = Input(Bool())
    val saeCtl = Input(UInt(7.W))

    val sramClk = Output(Clock())
    val addr = Output(UInt((params.rowWidth + params.colWidth).W))
    val data = Output(UInt(params.dataWidth.W))
    val mask = Output(UInt(params.maskWidth.W))
    val saeMuxed = Output(Bool())
  })

  val gatedClock = if (p(WithChiseltestSramsKey).isDefined) {
    (clock.asBool & io.sramEn).asClock
  } else {
    ClockGate(clock, io.sramEn)
  }
  io.sramClk := gatedClock

  io.addr := Cat(
    io.inRow(params.rowWidth - 1, 0),
    io.inCol(params.colWidth - 1, 0)
  )
  io.data := io.inData(params.dataWidth - 1, 0)
  io.mask := io.inMask(params.maskWidth - 1, 0)

  io.saeMuxed := io.saeInt;
  switch(io.saeSel) {
    is(SaeSrc.clk) {
      io.saeMuxed := io.saeClk
    }
    is(SaeSrc.ext) {
      io.saeMuxed := io.delayLineIn
    }
  }
}
