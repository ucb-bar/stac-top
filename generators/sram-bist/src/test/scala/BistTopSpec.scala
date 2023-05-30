package srambist

import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.stage.PrintFullStackTraceAnnotation
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

import srambist.analog.SramParams
import srambist.sramharness.SaeSrc

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
      Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
      new ProgrammableBistParams()
      ))(
    new WithChiseltestSrams(ChiseltestSramFailureMode.none)
)
      ).withAnnotations(Seq(WriteVcdAnnotation)) { c => 
        // Scan chain -> SRAM
        
        c.io.sramExtEn.poke(true.B)
        c.io.sramScanMode.poke(true.B)
        c.io.sramEn.poke(false.B)
        c.io.bistEn.poke(false.B)

        val populateRegisters = (addr: UInt, din: UInt, mask: UInt, we: Bool, sramId: UInt, sramSel: SramSrc.Type, saeSel: SaeSrc.Type) => {
          // Interleave with clock steps to make sure that operations do not occur while registers are being set up.
          c.io.addr.poke(addr)
          c.clock.step()
          c.io.din.poke(din)
          c.clock.step()
          c.io.mask.poke(mask)
          c.clock.step()
          c.io.we.poke(we)
          c.clock.step()
          c.io.sramId.poke(sramId)
          c.clock.step()
          c.io.sramSel.poke(sramSel)
          c.clock.step()
          c.io.saeSel.poke(saeSel)
          c.clock.step()
        }

        val executeScanChainOp = () => {
          // Once all registers are set up, enable SRAMs for one cycle.
          c.io.sramEn.poke(true.B)
          c.clock.step()
          c.io.sramEn.poke(false.B)
          c.clock.step()
        }

        populateRegisters(0.U, "habcdabcd".U, "hf".U, true.B, 0.U, SramSrc.mmio, SaeSrc.int)
        executeScanChainOp()

        populateRegisters(0.U, 0.U, 0.U, false.B, 0.U, SramSrc.mmio, SaeSrc.int)
        executeScanChainOp()

        c.io.dout.expect("habcdabcd".U)
        
        // Scan chain -> BIST
        
        // MMIO -> SRAM
        
        // MMIO -> BIST
    
    }
  }
}

