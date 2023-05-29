package srambist.scanchain

import chisel3._
import chiseltest._
import chisel3.experimental.VecLiterals._

import org.scalatest.flatspec.AnyFlatSpec

class ScanChainSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ScanChain"
  it should "work" in {
    test(new ScanChain(4)) { d => 
      d.io.se.poke(false.B)
      d.io.d.poke(Vec.Lit(true.B, false.B, true.B, true.B))
      d.io.si.poke(false.B)
      d.io.q.expect(Vec.Lit(false.B, false.B, false.B, false.B))
      d.io.so.expect(false.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, false.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, false.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.io.d.poke(Vec.Lit(false.B, true.B, true.B, false.B))
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, true.B, true.B, false.B))
      d.io.so.expect(false.B)
      d.io.se.poke(true.B)
      d.io.q.expect(Vec.Lit(false.B, true.B, true.B, false.B))
      d.io.so.expect(false.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, false.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.io.si.poke(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, false.B, false.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, true.B, false.B, false.B))
      d.io.so.expect(false.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, true.B, true.B, false.B))
      d.io.so.expect(false.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, true.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(true.B, true.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.io.si.poke(false.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, true.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, false.B, true.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, false.B, false.B, true.B))
      d.io.so.expect(true.B)
      d.clock.step()
      d.io.q.expect(Vec.Lit(false.B, false.B, false.B, false.B))
      d.io.so.expect(false.B)
    }
  }
}

