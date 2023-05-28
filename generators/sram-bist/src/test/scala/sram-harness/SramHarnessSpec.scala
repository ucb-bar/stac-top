package srambist.sramharness

import chisel3._
import chisel3.util._
import chiseltest._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

class SramHarnessSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramHarness"
  it should "work with a 128x32m8 SRAM" in {
    test(new SramHarness(new SramHarnessParams(
        5, 2, 32, 8
      ))(
    Parameters.empty
)
      ) { c => 
      c.io.sramEn.poke(false.B)
      c.io.row.poke("b1001101011".U)
      c.io.col.poke("b101".U)
      c.io.bistData.poke("habcdabcdabcdabcd".U)
      c.io.bistMask.poke("habcdabcdabcdabcd".U)
      c.io.saeInt.poke(true.B)
      c.io.saeSel.poke(SaeSrc.int)
      c.io.saeClk.poke(false.B)
      c.io.saeCtlBinary.poke("b0000010".U)
      c.io.addr.expect("b0101101".U)
      c.io.data.expect("habcdabcd".U)
      c.io.mask.expect("hcd".U)
      c.io.saeMuxed.expect(true.B)

      c.io.row.poke("b1001010000".U)
      c.io.col.poke("b011".U)
      c.io.bistData.poke("h123456789abcdef0".U)
      c.io.bistMask.poke("h123456789abcdef0".U)
      c.io.addr.expect("b1000011".U)
      c.io.data.expect("h9abcdef0".U)
      c.io.mask.expect("hf0".U)

      c.io.saeInt.poke(false.B)
      c.io.saeMuxed.expect(false.B)
      c.io.saeSel.poke(SaeSrc.clk)
      c.io.saeMuxed.expect(false.B)
      c.io.saeClk.poke(true.B)
      c.io.saeMuxed.expect(true.B)
    }
  }
}

