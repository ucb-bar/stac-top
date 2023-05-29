package srambist

import chisel3._
import chisel3.util._
import chiseltest._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

import srambist.analog.SramParams

class BistTopSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "BistTop"
  it should "work with hardmacro SRAMs" in {
    test(new BistTop(new BistTopParams(
      Seq(new SramParams(8, 4, 64, 32)),
      new ProgrammableBistParams()
      ))(
    Parameters.empty
)
      ) { c => 
    }
  }
  it should "work with chiseltest SRAMs" in {
    test(new BistTop(new BistTopParams(
      Seq(new SramParams(8, 4, 64, 32)),
      new ProgrammableBistParams()
      ))(
    new WithChiseltestSrams(ChiseltestSramFailureMode.none)
)
      ) { c => 
    }
  }
}

