package srambist.sramharness

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.util.ClockGate

import srambist.analog.{Tdc, DelayLine}

case class SramHarnessParams(
  rowWidth: Int,
  colWidth: Int,
  dataWidth: Int,
  maskWidth: Int,

  )

object SaeSrc extends ChiselEnum {
  val int, clk, ext = Value
}

class SramHarness(params: SramHarnessParams) (implicit p: Parameters) extends Module {

  val io = IO(new Bundle {
    val sramEn = Input(Bool())
    val row = Input(UInt(10.W))
    val col = Input(UInt(3.W))
    val bistData = Input(UInt(64.W))
    val bistMask = Input(UInt(64.W))
    val saeInt = Input(Bool())
    val saeSel = Input(SaeSrc())
    val saeClk = Input(Bool()) // TODO: should this be a Clock?
    val saeCtlBinary = Input(UInt(7.W))

    val sramClk = Output(Clock())
    val addr = Output(UInt((params.rowWidth + params.colWidth).W))
    val data = Output(UInt(params.dataWidth.W))
    val mask = Output(UInt(params.maskWidth.W))
    val saeMuxed = Output(Bool())
    val saeOut = Output(UInt(252.W) )
  })

  io.sramClk := ClockGate(clock, io.sramEn)
  io.addr := Cat(io.row(params.rowWidth - 1, 0), io.col(params.colWidth - 1, 0))
  io.data := io.bistData(params.dataWidth-1, 0)
  io.mask := io.bistMask(params.maskWidth - 1, 0)

  val delay_line = Module(new DelayLine)
  delay_line.io.clk_in := clock.asBool
  val saeCtlOH = UIntToOH(io.saeCtlBinary)
  delay_line.io.ctl := saeCtlOH
  delay_line.io.ctl_b := ~saeCtlOH
  io.saeMuxed := io.saeInt; // TODO: verify default
  switch (io.saeSel) {
    is (SaeSrc.clk) {
      io.saeMuxed := io.saeClk
    }
    is (SaeSrc.ext) {
      io.saeMuxed := delay_line.io.clk_out
    }
  }
  
  val tdc = Module(new Tdc)
  tdc.io.a := clock.asBool
  // TODO: insert large buffer before tdc.io.b
  tdc.io.b := io.saeMuxed
  tdc.io.reset_b := ~reset.asBool
  io.saeOut := tdc.io.dout

}
