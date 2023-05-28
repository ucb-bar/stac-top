package srambist.sramharness

import chisel3._
import chiseltest._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

class SramHarnessSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramHarness"
  it should "work" in {
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
      c.io.saeSel.poke(c.SaeSrc.int)
      c.io.saeClk.poke(false.B)
      c.io.saeCtlBinary.poke("b1010010".U)
      c.io.addr.expect("b0101101".U)
      c.io.data.expect("habcdabcd".U)
      c.io.mask.expect("hcd".U)
      c.io.saeMuxed.expect(true.B)
    }
  }
}

