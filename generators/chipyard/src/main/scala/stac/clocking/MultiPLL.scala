package chipyard.stac.clocking

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.HasBlackBoxInline

class MultiPLLIO extends Bundle {
  val io_scan_in = Input(Bool())
  val io_scan_clk = Input(Bool())
  val io_scan_en = Input(Bool())
  val io_scan_rst = Input(Bool())
  val io_pll_sel = Input(Bool())
  val io_arstb = Input(Bool())
  val io_scan_out = Output(Bool())
  val io_pll_clk_out = Output(Bool())
  val io_pll_div_out = Output(Bool())

  // guarded IO-facing nets
  val clock = Analog(1.W) // input
  val io_pll_clk_out_gr = Analog(1.W) // output
  val io_pll_div_out_gr = Analog(1.W) // output
}

class MultiPLL extends BlackBox with HasBlackBoxInline {
  val io = IO(new MultiPLLIO())

  override val desiredName: String = "MultiPLLTop"

  setInline("MultiPLLTop.bb.v",
    """
      |module MultiPLLTop (
      |  inout clock,
      |  input io_scan_in,
      |  input io_scan_clk,
      |  input io_scan_en,
      |  input io_scan_rst,
      |  input io_pll_sel,
      |  input io_arstb,
      |  output io_scan_out,
      |  output io_pll_clk_out,
      |  output io_pll_div_out,
      |  inout io_pll_clk_out_gr,
      |  inout io_pll_div_out_gr
      |);
      |endmodule
      |""".stripMargin)
}
