package srambist.analog

import chisel3._
import chisel3.util._
import chiseltest._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

import srambist.{WithChiseltestSrams, ChiseltestSramFailureMode}

class SramSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Sram"
  it should "work with chiseltest configuration" in {
    test(new Sram(new SramParams(8, 4, 64, 32))(
    new WithChiseltestSrams(ChiseltestSramFailureMode.none)
)
      ).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
        c.io.we.poke(true.B)
        c.io.wmask.poke("hf".U)
        c.io.addr.poke(0.U)
        c.io.din.poke("habcdabcd".U)
        c.clock.step()
        c.io.we.poke(false.B)
        c.clock.step()
        c.io.dout.expect("habcdabcd".U)

        c.io.we.poke(true.B)
        c.io.wmask.poke("hf".U)
        c.io.addr.poke(63.U)
        c.io.din.poke("h12345678".U)
        c.clock.step()
        c.io.we.poke(false.B)
        c.clock.step()
        c.io.dout.expect("h12345678".U)

        c.io.we.poke(false.B)
        c.io.addr.poke(0.U)
        c.clock.step()
        c.io.dout.expect("habcdabcd".U)
    }
  }
}

