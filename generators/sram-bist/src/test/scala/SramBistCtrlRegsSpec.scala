package srambist

import chisel3._
import chiseltest._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

import srambist.SramBistCtrlRegs._

class SramBistCtrlRegsSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramBistCtrlReg"
  it should "correctly compute offsets" in {
    println(s"Scan chain offsets: ${REG_WIDTH.keys.map(key => (key, SCAN_CHAIN_OFFSET(key)))}")
    println(s"Reg map offsets: ${REG_WIDTH.keys.map(key => (key, REGMAP_OFFSET(key)))}")
  }
}
