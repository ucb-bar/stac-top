package srambist.sramharness

import chisel3._
import chisel3.util._
import chiseltest._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

class SramHarnessSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramHarness"
  it should "work with a 128x32m8 SRAM" in {
    test(
      new SramHarness(
        new SramHarnessParams(
          5,
          2,
          32,
          8
        )
      )(
        Parameters.empty
      )
    ) { c =>
      c.io.inRow.poke("b1001101011".U)
      c.io.inCol.poke("b101".U)
      c.io.inData.poke("habcdabcdabcdabcd".U)
      c.io.inMask.poke("habcdabcdabcdabcd".U)
      c.io.addr.expect("b0101101".U)
      c.io.data.expect("habcdabcd".U)
      c.io.mask.expect("hcd".U)

      c.io.inRow.poke("b1001010000".U)
      c.io.inCol.poke("b011".U)
      c.io.inData.poke("h123456789abcdef0".U)
      c.io.inMask.poke("h123456789abcdef0".U)
      c.io.addr.expect("b1000011".U)
      c.io.data.expect("h9abcdef0".U)
      c.io.mask.expect("hf0".U)
    }
  }
}
