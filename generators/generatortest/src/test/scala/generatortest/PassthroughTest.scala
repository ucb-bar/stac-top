package generatortest

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PassthroughUnitTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Passthrough"
  it should "work" in {
    test(new Passthrough) { c => 
      c.io.in.poke(0.U)     // Set our input to value 0
      c.io.out.expect(0.U)  // Assert that the output correctly has 0
      c.io.in.poke(1.U)     // Set our input to value 1
      c.io.out.expect(1.U)  // Assert that the output correctly has 1
      c.io.in.poke(2.U)     // Set our input to value 2
      c.io.out.expect(2.U)  // Assert that the output correctly has 2
    }
  }
}

