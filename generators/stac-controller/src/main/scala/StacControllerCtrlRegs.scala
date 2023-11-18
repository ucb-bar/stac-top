package staccontroller

import chisel3._
import chisel3.util._
import freechips.rocketchip.util.SimpleRegIO

import scala.collection.mutable.LinkedHashMap

object StacControllerCtrlRegs extends Enumeration {
  type Type = Value
  val SRAM_EXT_EN, SRAM_SCAN_MODE, SRAM_EN, SRAM_SCAN_IN, SRAM_SCAN_EN, SRAM_BIST_EN, 
      SRAM_BIST_START, SRAM_SCAN_OUT, SRAM_BIST_DONE, CLK_SEL, PLL_SEL, PLL_SCAN_EN, PLL_SCAN_RST,
      PLL_SCAN_CLK, PLL_SCAN_IN, PLL_SCAN_OUT, PLL_ARSTB, CUSTOM_BOOT = Value

  val REG_WIDTH = LinkedHashMap(
    SRAM_EXT_EN -> 1,
    SRAM_SCAN_MODE -> 1,
    SRAM_EN -> 1,
    SRAM_SCAN_IN -> 1,
    SRAM_SCAN_EN -> 1,
    SRAM_BIST_EN -> 1,
    SRAM_BIST_START -> 1,
    SRAM_SCAN_OUT -> 1,
    SRAM_BIST_DONE -> 1,
    CLK_SEL -> 2,
    PLL_SEL -> 1,
    PLL_SCAN_EN -> 1,
    PLL_SCAN_RST -> 1,
    PLL_SCAN_CLK -> 1,
    PLL_SCAN_IN -> 1,
    PLL_SCAN_OUT -> 1,
    PLL_ARSTB -> 1,
    CUSTOM_BOOT -> 1
  )
  val TOTAL_REG_WIDTH = REG_WIDTH.values.sum

  val SCAN_CHAIN_OFFSET =
    REG_WIDTH.keys.zip(REG_WIDTH.values.scanLeft(0)(_ + _).dropRight(1)).toMap

  val SCAN_OUT_OFFSET =
    REG_WIDTH.keys.zip(REG_WIDTH.values.scanRight(0)(_ + _).drop(1)).toMap

  val REGMAP_OFFSET =
    (REG_WIDTH.keys)
      .zip(
        REG_WIDTH.values.scanLeft(0)((acc, n) => acc + ((n - 1) / 64 + 1) * 8)
      )
      .toMap
}
import StacControllerCtrlRegs._

class StacControllerMmioRegIO extends Bundle {
  val sramExtEn = new SimpleRegIO(REG_WIDTH(SRAM_EXT_EN))
  val sramScanMode = new SimpleRegIO(REG_WIDTH(SRAM_SCAN_MODE))
  val sramEn = new SimpleRegIO(REG_WIDTH(SRAM_EN))
  val sramScanIn = new SimpleRegIO(REG_WIDTH(SRAM_SCAN_IN))
  val sramScanEn = new SimpleRegIO(REG_WIDTH(SRAM_SCAN_EN))
  val sramBistEn = new SimpleRegIO(REG_WIDTH(SRAM_BIST_EN))
  val sramBistStart = new SimpleRegIO(REG_WIDTH(SRAM_BIST_START))
  val clkSel = new SimpleRegIO(REG_WIDTH(CLK_SEL))
  val pllSel = new SimpleRegIO(REG_WIDTH(PLL_SEL))
  val pllScanEn = new SimpleRegIO(REG_WIDTH(PLL_SCAN_EN))
  val pllScanRst = new SimpleRegIO(REG_WIDTH(PLL_SCAN_RST))
  val pllScanClk = new SimpleRegIO(REG_WIDTH(PLL_SCAN_CLK))
  val pllScanIn = new SimpleRegIO(REG_WIDTH(PLL_SCAN_IN))
  val pllArstb = new SimpleRegIO(REG_WIDTH(PLL_ARSTB))
  val customBoot = new SimpleRegIO(REG_WIDTH(CUSTOM_BOOT))
  val sramScanOut = new SimpleRegIO(REG_WIDTH(SRAM_SCAN_OUT))
  val sramBistDone = new SimpleRegIO(REG_WIDTH(SRAM_BIST_DONE))
  val pllScanOut = new SimpleRegIO(REG_WIDTH(PLL_SCAN_OUT))
}
