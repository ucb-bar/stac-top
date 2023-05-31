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
  it should "work for deterministic March test with cols in inner loop" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 4,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>
        val maxRows = 15;
        val maxCols = 7;
        val readOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.read,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val writeOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val readFlippedOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.read,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.flipped
        )
        val writeFlippedOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.flipped
        )
      val opElementList = Vec(4, new d.Operation())
        .Lit(0 -> writeOp, 1 -> readOp, 2 -> writeFlippedOp, 3 -> readFlippedOp)
      val opElement =
        chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
          _.operations -> opElementList,
          _.maxIdx -> 3.U,
          _.dir -> d.Direction.up,
          _.numAddrs -> 0.U
        )
      val waitElement = chiselTypeOf(d.io.elementSequence(0).waitElement)
        .Lit(_.cyclesToWait -> 0.U)
      val march = chiselTypeOf(d.io.elementSequence(0)).Lit(
        _.operationElement -> opElement,
        _.waitElement -> waitElement,
        _.elementType -> d.ElementType.rwOp
      )
      val zeros = 0.U(32.W)
      val ones = "hffffffff".U(32.W)

      d.clock.setTimeout((maxCols + 1) * (maxRows + 1) * 4 * 4)
      d.io.start.poke(true.B)
      d.io.en.poke(true.B)
      d.io.maxRowAddr.poke(maxRows.U)
      d.io.maxColAddr.poke(maxCols.U)
      d.io.innerDim.poke(d.Dimension.col)
      d.io.maxElementIdx.poke(3.U)
      d.io.seed.poke(1.U)
      d.io.patternTable.poke(Vec.Lit(ones, ones, zeros, zeros))
      d.io.elementSequence.poke(
        Vec(4, new d.Element())
          .Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march)
      )
      d.clock.step()
      d.clock.step()
      d.clock.step()
      d.io.start.poke(false.B)

      for (opNum <- 0 until 4) {
        for (i <- 0 to maxRows) {
          for (j <- 0 to maxCols) {
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(false.B)
            d.io.checkEn.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(ones)
            d.io.sramWen.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(ones)
            d.io.sramWen.expect(false.B)
            d.io.checkEn.expect(true.B)
            d.clock.step()
          }
        }
      }

      d.io.sramEn.expect(false.B)
      d.io.sramWen.expect(false.B)
      d.io.done.expect(true.B)
    }
  }

  it should "work for deterministic March test with rows in inner loop" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 4,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>
        val maxRows = 15;
        val maxCols = 7;
        val readOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.read,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val writeOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val readFlippedOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.read,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.flipped
        )
        val writeFlippedOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.flipped
        )
      val opElementList = Vec(4, new d.Operation())
        .Lit(0 -> writeOp, 1 -> readOp, 2 -> writeFlippedOp, 3 -> readFlippedOp)
      val opElement =
        chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
          _.operations -> opElementList,
          _.maxIdx -> 3.U,
          _.dir -> d.Direction.up,
          _.numAddrs -> 0.U
        )
      val waitElement = chiselTypeOf(d.io.elementSequence(0).waitElement)
        .Lit(_.cyclesToWait -> 0.U)
      val march = chiselTypeOf(d.io.elementSequence(0)).Lit(
        _.operationElement -> opElement,
        _.waitElement -> waitElement,
        _.elementType -> d.ElementType.rwOp
      )
      println(march)
      val zeros = 0.U(32.W)
      val ones = "hffffffff".U(32.W)

      d.clock.setTimeout((maxCols + 1) * (maxRows + 1) * 4 * 4)
      d.io.start.poke(true.B)
      d.io.en.poke(true.B)
      d.io.maxRowAddr.poke(maxRows.U)
      d.io.maxColAddr.poke(maxCols.U)
      d.io.innerDim.poke(d.Dimension.row)
      d.io.maxElementIdx.poke(3.U)
      d.io.seed.poke(1.U)
      d.io.patternTable.poke(Vec.Lit(ones, ones, zeros, zeros))
      d.io.elementSequence.poke(
        Vec(4, new d.Element())
          .Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march)
      )
      d.clock.step()
      d.io.start.poke(false.B)

      for (opNum <- 0 until 4) {
        for (j <- 0 to maxCols) {
          for (i <- 0 to maxRows) {
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(false.B)
            d.io.checkEn.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(ones)
            d.io.sramWen.expect(true.B)
            d.clock.step()
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(ones)
            d.io.sramWen.expect(false.B)
            d.io.checkEn.expect(true.B)
            d.clock.step()
          }
        }
      }

      d.io.sramEn.expect(false.B)
      d.io.sramWen.expect(false.B)
      d.io.done.expect(true.B)
    }
  }

  it should "work for single march element with single operation" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 4,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>
        val maxRows = 15;
        val maxCols = 7;
        val writeOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val opElementList = Vec(4, new d.Operation())
          .Lit(0 -> writeOp, 1 -> writeOp, 2 -> writeOp, 3 -> writeOp)
        val opElement =
          chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
            _.operations -> opElementList,
            _.maxIdx -> 0.U,
            _.dir -> d.Direction.up,
            _.numAddrs -> 0.U
          )
        val waitElement = chiselTypeOf(d.io.elementSequence(0).waitElement)
          .Lit(_.cyclesToWait -> 0.U)
        val march = chiselTypeOf(d.io.elementSequence(0)).Lit(
          _.operationElement -> opElement,
          _.waitElement -> waitElement,
          _.elementType -> d.ElementType.rwOp
        )
        val zeros = 0.U(32.W)
        val ones = "hffffffff".U(32.W)

        d.clock.setTimeout((maxRows + 1) * (maxCols + 1) + 100)
        d.io.start.poke(true.B)
        d.io.en.poke(true.B)
        d.io.maxRowAddr.poke(maxRows.U)
        d.io.maxColAddr.poke(maxCols.U)
        d.io.innerDim.poke(d.Dimension.col)
        d.io.maxElementIdx.poke(0.U)
        d.io.seed.poke(1.U)
        d.io.patternTable.poke(Vec.Lit(ones, ones, zeros, zeros))
        d.io.elementSequence.poke(
          Vec(4, new d.Element())
            .Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march)
        )
        d.clock.step()
        d.io.start.poke(false.B)

        for (i <- 0 to maxRows) {
          for (j <- 0 to maxCols) {
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(true.B)
            d.io.checkEn.expect(false.B)
            d.clock.step()
          }
        }

        d.io.sramEn.expect(false.B)
        d.io.sramWen.expect(false.B)
        d.io.done.expect(true.B)
    }
  }

  it should "work for background fill followed by pseudorandom test" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 4,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>
        val maxRows = 15;
        val maxCols = 7;
        val writeOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.write,
          _.randData -> false.B,
          _.randMask -> false.B,
          _.dataPatternIdx -> 3.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val opElementList = Vec(4, new d.Operation())
          .Lit(0 -> writeOp, 1 -> writeOp, 2 -> writeOp, 3 -> writeOp)
        val opElement =
          chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
            _.operations -> opElementList,
            _.maxIdx -> 0.U,
            _.dir -> d.Direction.up,
            _.numAddrs -> 0.U
          )
        val waitElement = chiselTypeOf(d.io.elementSequence(0).waitElement)
          .Lit(_.cyclesToWait -> 0.U)
        val march = chiselTypeOf(d.io.elementSequence(0)).Lit(
          _.operationElement -> opElement,
          _.waitElement -> waitElement,
          _.elementType -> d.ElementType.rwOp
        )
        val randomOp = chiselTypeOf(
          d.io.elementSequence(0).operationElement.operations(0)
        ).Lit(
          _.operationType -> d.OperationType.rand,
          _.randData -> true.B,
          _.randMask -> true.B,
          _.dataPatternIdx -> 0.U,
          _.maskPatternIdx -> 0.U,
          _.flipped -> d.FlipType.unflipped
        )
        val randOpElementList = Vec(4, new d.Operation())
          .Lit(0 -> randomOp, 1 -> writeOp, 2 -> writeOp, 3 -> writeOp)
        val randOpElement =
          chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
            _.operations -> randOpElementList,
            _.maxIdx -> 0.U,
            _.dir -> d.Direction.rand,
            _.numAddrs -> 100.U
          )
        val randElement = chiselTypeOf(d.io.elementSequence(0)).Lit(
          _.operationElement -> randOpElement,
          _.waitElement -> waitElement,
          _.elementType -> d.ElementType.rwOp
        )
        val zeros = 0.U(32.W)
        val ones = "hffffffff".U(32.W)

        d.clock.setTimeout((maxRows + 1) * (maxCols + 1) + 200)
        d.io.start.poke(true.B)
        d.io.en.poke(true.B)
        d.io.maxRowAddr.poke(maxRows.U)
        d.io.maxColAddr.poke(maxCols.U)
        d.io.innerDim.poke(d.Dimension.col)
        d.io.maxElementIdx.poke(1.U)
        d.io.seed.poke(1.U)
        d.io.patternTable.poke(Vec.Lit(ones, ones, zeros, zeros))
        d.io.elementSequence.poke(
          Vec(4, new d.Element())
            .Lit(0 -> march, 1 -> randElement, 2 -> march, 3 -> march)
        )
        d.clock.step()
        d.io.start.poke(false.B)

        for (i <- 0 to maxRows) {
          for (j <- 0 to maxCols) {
            d.io.sramEn.expect(true.B)
            d.io.row.expect(i.U)
            d.io.col.expect(j.U)
            d.io.data.expect(zeros)
            d.io.sramWen.expect(true.B)
            d.io.checkEn.expect(false.B)
            d.clock.step()
          }
        }

        for (i <- 0 until 100) {
          d.io.sramEn.expect(true.B)
          d.io.checkEn.expect(false.B)
          d.clock.step()
        }

        d.io.sramEn.expect(false.B)
        d.io.sramWen.expect(false.B)
        d.io.done.expect(true.B)
    }
  }
}
