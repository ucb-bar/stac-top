package srambist.programmablebist

import srambist.{ProgrammableBistParams}
import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chiseltest._
import chisel3.experimental.VecLiterals._
import chisel3.experimental.BundleLiterals._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

class ProgrammableBistSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ProgrammableBist"
  it should "work" in {
    test(new ProgrammableBist(new ProgrammableBistParams(patternTableLength = 4, elementTableLength = 4, operationsPerElement = 4))).withAnnotations(Seq(PrintFullStackTraceAnnotation)) { d => 
      val maxRows = 15;
      val maxCols = 7;
      val operation = chiselTypeOf(d.io.elementSequence(0).operationElement.operations(0)).Lit(_.operationType -> d.OperationType.read, _.randData -> false.B, _.randMask -> false.B, _.dataPatternIdx -> 0.U, _.maskPatternIdx -> 0.U, _.flipped -> d.FlipType.unflipped)
    val opElementList = Vec(4, new d.Operation()).Lit(0 -> operation, 1 -> operation, 2 -> operation, 3 -> operation)
      val opElement = chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(_.operations -> opElementList, _.maxIdx -> 3.U, _.dir -> d.Direction.up, _.mask -> 0.U, _.numAddrs -> 0.U)
      val waitElement = chiselTypeOf(d.io.elementSequence(0).waitElement).Lit(_.cyclesToWait -> 0.U)
      val march = chiselTypeOf(d.io.elementSequence(0)).Lit(_.operationElement -> opElement, _.waitElement -> waitElement, _.elementType -> d.ElementType.rwOp)
      println(march)
      // val march = chiselTypeOf(d.io.elementSequence).Lit(_.operationElement -> opElement, _.waitElement -> waitElement, _.elementType -> d.ElementType.rwOp)
      val zeroData = 0.U(32.W)

      d.io.start.poke(true.B)
      d.io.en.poke(true.B)
      d.io.maxRowAddr.poke(maxRows.U)
      d.io.maxColAddr.poke(maxCols.U)
      d.io.innerDim.poke(d.Dimension.col)
      d.io.numElements.poke(1.U)
      d.io.seed.poke(1.U)
      d.io.patternTable.poke(Vec.Lit(zeroData, zeroData, zeroData, zeroData))
      d.io.elementSequence.poke(Vec(4, new d.Element()).Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march))
      d.clock.step()
    }
  }
}
