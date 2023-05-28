package srambist.misr

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.flatspec.AnyFlatSpec

class MISRSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "MISR"
  it should "work with constant input 0000" in {
    test(new MaxPeriodFibonacciMISR(4)) { c => 
      c.io.in.poke(Vec.Lit(false.B, false.B, false.B, false.B))
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.clock.step()
      c.clock.step()
      // Value should be held when enable is low.
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))

      // Step the MISR a few times
      c.io.en.poke(true.B)
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(false.B, true.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(false.B, false.B, true.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, true.B))
    }
  }

  it should "work with constant input 1010" in {
    test(new MaxPeriodFibonacciMISR(4)) { c => 
      c.io.in.poke(Vec.Lit(true.B, false.B, true.B, false.B))
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.clock.step()
      c.clock.step()
      // Value should be held when enable is low.
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))

      // Step the MISR a few times
      c.io.en.poke(true.B)
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(true.B, true.B, true.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(false.B, true.B, false.B, true.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(false.B, false.B, false.B, false.B))
    }
  }

  it should "work with changing input" in {
    test(new MaxPeriodFibonacciMISR(4)) { c => 
      c.io.in.poke(Vec.Lit(true.B, false.B, true.B, false.B))
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.clock.step()
      c.clock.step()
      // Value should be held when enable is low.
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))

      // Step the MISR a few times
      c.io.en.poke(true.B)
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(true.B, true.B, true.B, false.B))
      c.io.in.poke(Vec.Lit(false.B, false.B, true.B, true.B))
      c.io.out.expect(Vec.Lit(true.B, true.B, true.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(true.B, true.B, false.B, false.B))
      c.io.in.poke(Vec.Lit(true.B, true.B, true.B, false.B))
      c.io.out.expect(Vec.Lit(true.B, true.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.io.in.poke(Vec.Lit(false.B, false.B, false.B, true.B))
      c.io.out.expect(Vec.Lit(true.B, false.B, false.B, false.B))
      c.clock.step()
      c.io.out.expect(Vec.Lit(false.B, true.B, false.B, true.B))
    }
  }
}

