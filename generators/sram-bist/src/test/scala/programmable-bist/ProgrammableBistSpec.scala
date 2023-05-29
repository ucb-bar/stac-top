package srambist.programmablebist

import srambist.{ProgrammableBistParams}
import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.flatspec.AnyFlatSpec

class ProgrammableBistSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ProgrammableBist"
  it should "work" in {
    test(new ProgrammableBist(new ProgrammableBistParams())) { d => 
      d.io.start.poke(true.B)
    }
  }
}
