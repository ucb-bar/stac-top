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
    test(new ProgrammableBist(new ProgrammableBistParams(patternTableLength = 4, elementTableLength = 4))) { d => 
      val maxRows = 15;
      val maxCols = 7;
      // val march = chiselTypeOf(d.io.elementSequence).Lit(_.operationElement -> opElement, _.waitElement -> waitElement, _.elementType -> d.ElementType.rwOp)
      val march = new d.Element()
      val zeroData = 0.U(32.W)

      d.io.start.poke(true.B)
      d.io.en.poke(true.B)
      d.io.maxRowAddr.poke(maxRows.U)
      d.io.maxColAddr.poke(maxCols.U)
      d.io.innerDim.poke(d.Dimension.col)
      d.io.numElements.poke(1.U)
      d.io.seed.poke(1.U)
      d.io.patternTable.poke(Vec.Lit(zeroData, zeroData, zeroData, zeroData))
      d.io.elementSequence.poke(Vec.Lit(march, march, march, march))
      d.clock.step()
    }
  }
}
