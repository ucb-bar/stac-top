package srambist

import chisel3._
import chiseltest._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

import srambist.SramBistCtrlRegs._

class SramBistCtrlRegSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramBistCtrlReg"
  it should "correctly compute offsets" in {
    println(SCAN_CHAIN_OFFSET)
  }
}
