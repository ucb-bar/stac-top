package srambist.programmablebist

import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chiseltest._
import chisel3.experimental.VecLiterals._
import chisel3.experimental.BundleLiterals._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

object Direction extends Enumeration {
  type Direction = Value
  val Up, Down, Rand = Value
}

import Direction._

class ProgrammableBistHelpers(val d: ProgrammableBist) {
  val maxRows = 15;
  val maxCols = 7;

  def readOp(index: Int, flipped: Boolean = false): d.Operation = {
    val flip = if (flipped) d.FlipType.flipped else d.FlipType.unflipped
    chiselTypeOf(
      d.io.elementSequence(0).operationElement.operations(0)
    ).Lit(
      _.operationType -> d.OperationType.read,
      _.randData -> false.B,
      _.randMask -> false.B,
      _.dataPatternIdx -> index.U,
      _.maskPatternIdx -> 0.U,
      _.flipped -> flip
    )
  }

  val emptyOp = readOp(0)

  def writeOp(
      dataIndex: Int,
      maskIndex: Int,
      flipped: Boolean = false
  ): d.Operation = {
    val flip = if (flipped) d.FlipType.flipped else d.FlipType.unflipped
    chiselTypeOf(
      d.io.elementSequence(0).operationElement.operations(0)
    ).Lit(
      _.operationType -> d.OperationType.write,
      _.randData -> false.B,
      _.randMask -> false.B,
      _.dataPatternIdx -> dataIndex.U,
      _.maskPatternIdx -> maskIndex.U,
      _.flipped -> flip
    )
  }

  def opList(ops: Seq[d.Operation]): Vec[d.Operation] = {
    val elts: Seq[(Int, d.Operation)] =
      (0 until d.params.operationsPerElement).map(i =>
        if (i < ops.size) { (i, ops(i)) }
        else { (i, emptyOp) }
      )
    Vec(d.params.operationsPerElement, new d.Operation())
      .Lit(elts: _*)
  }

  def waitElementInner(cycles: Int) = {
    chiselTypeOf(d.io.elementSequence(0).waitElement)
      .Lit(_.cyclesToWait -> cycles.U)
  }

  val opElementList = Vec(4, new d.Operation())
    .Lit(
      0 -> writeOp(2, 3),
      1 -> readOp(2),
      2 -> writeOp(2, 3, true),
      3 -> readOp(2, true)
    )

  def opElement(
      ops: Seq[d.Operation],
      dir: Direction,
      numAddrs: Int = 0
  ): d.Element = {
    val hwDir = dir match {
      case Up   => d.Direction.up
      case Down => d.Direction.down
      case Rand => d.Direction.rand
    }
    val operations: Vec[d.Operation] = opList(ops)
    val inner = chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
      _.operations -> operations,
      _.maxIdx -> (ops.size - 1).U,
      _.dir -> hwDir,
      _.numAddrs -> numAddrs.U
    )
    chiselTypeOf(d.io.elementSequence(0)).Lit(
      _.operationElement -> inner,
      _.waitElement -> waitElementInner(0),
      _.elementType -> d.ElementType.rwOp
    )
  }

  val emptyElement = opElement(Seq(readOp(0)), Rand, 2)

  def waitElement(
      cycles: Int
  ): d.Element = {
    val operations: Vec[d.Operation] = opList(Seq())
    val inner = chiselTypeOf(d.io.elementSequence(0).operationElement).Lit(
      _.operations -> operations,
      _.maxIdx -> 0.U,
      _.dir -> d.Direction.up,
      _.numAddrs -> 0.U
    )
    chiselTypeOf(d.io.elementSequence(0)).Lit(
      _.operationElement -> inner,
      _.waitElement -> waitElementInner(cycles),
      _.elementType -> d.ElementType.waitOp
    )
  }

  def elementSequence(elements: Seq[d.Element]): Vec[d.Element] = {
    val elts: Seq[(Int, d.Element)] =
      (0 until d.params.elementTableLength).map(i =>
        if (i < elements.size) { (i, elements(i)) }
        else { (i, emptyElement) }
      )
    Vec(d.params.elementTableLength, new d.Element())
      .Lit(elts: _*)
  }

  val march = opElement(
    Seq(writeOp(2, 3), readOp(2), writeOp(2, 3, true), readOp(2, true)),
    Up
  )

  val wait100 = waitElement(100)

  def marchCm(patternIdx: Int, onesIdx: Int): Vec[d.Element] = {
    // w0
    val m1 = opElement(Seq(writeOp(patternIdx, onesIdx)), Up)
    // r0, w1
    val m2 =
      opElement(Seq(readOp(patternIdx), writeOp(patternIdx, onesIdx, true)), Up)
    // r1, w0
    val m3 =
      opElement(Seq(readOp(patternIdx, true), writeOp(patternIdx, onesIdx)), Up)
    // r0, w1
    val m4 = opElement(
      Seq(readOp(patternIdx), writeOp(patternIdx, onesIdx, true)),
      Down
    )
    // r1, w0
    val m5 = opElement(
      Seq(readOp(patternIdx, true), writeOp(patternIdx, onesIdx)),
      Down
    )
    // r0
    val m6 = opElement(Seq(readOp(patternIdx)), Up)

    elementSequence(Seq(m1, m2, m3, m4, m5, m6))
  }

  val zeros = 0.U(32.W)
  val ones = "hffffffff".U(32.W)
  val bp0 = "h5f1a950d".U(32.W)
  val bp0f = "ha0e56af2".U(32.W)

  val simpleMarchElements = elementSequence(Seq(march, march, march, march))
  val marchWaitMarch = elementSequence(Seq(march, wait100, march))

  val io = d.io
  val clock = d.clock

  def expectRead(i: Int, j: Int, pattern: UInt) = {
    d.io.row.expect(i.U)
    d.io.col.expect(j.U)
    d.io.data.expect(pattern)
    d.io.sramEn.expect(true.B)
    d.io.sramWen.expect(false.B)
    d.io.checkEn.expect(true.B)
    d.io.done.expect(false.B)
    d.clock.step()
  }
  def expectWrite(i: Int, j: Int, pattern: UInt) = {
    d.io.row.expect(i.U)
    d.io.col.expect(j.U)
    d.io.data.expect(pattern)
    d.io.sramEn.expect(true.B)
    d.io.sramWen.expect(true.B)
    d.io.checkEn.expect(false.B)
    d.io.done.expect(false.B)
    d.clock.step()
  }
  def expectDone() = {
    d.io.sramEn.expect(false.B)
    d.io.sramWen.expect(false.B)
    d.io.done.expect(true.B)
  }

  def expectWait(cycles: Int) = {
    for (i <- 0 until cycles) {
      d.io.sramEn.expect(false.B)
      d.io.sramWen.expect(false.B)
      d.io.done.expect(false.B)
      d.clock.step()
    }
  }
}

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

        val h = new ProgrammableBistHelpers(d)

        h.clock.setTimeout((h.maxCols + 1) * (h.maxRows + 1) * 4 * 4)
        h.io.start.poke(true.B)
        h.io.en.poke(true.B)
        h.io.maxRowAddr.poke(h.maxRows.U)
        h.io.maxColAddr.poke(h.maxCols.U)
        h.io.innerDim.poke(h.d.Dimension.col)
        h.io.maxElementIdx.poke(3.U)
        h.io.seed.poke(1.U)
        h.io.patternTable.poke(Vec.Lit(h.ones, h.ones, h.zeros, h.zeros))
        h.io.elementSequence.poke(
          h.simpleMarchElements
        )
        h.clock.step()
        h.clock.step()
        h.clock.step()
        h.io.start.poke(false.B)

        for (opNum <- 0 until 4) {
          for (i <- 0 to h.maxRows) {
            for (j <- 0 to h.maxCols) {
              h.io.sramEn.expect(true.B)
              h.io.row.expect(i.U)
              h.io.col.expect(j.U)
              h.io.sramWen.expect(true.B)
              h.io.data.expect(h.zeros)
              h.clock.step()
              h.io.sramEn.expect(true.B)
              h.io.row.expect(i.U)
              h.io.col.expect(j.U)
              h.io.sramWen.expect(false.B)
              h.io.data.expect(h.zeros)
              h.io.checkEn.expect(true.B)
              h.clock.step()
              h.io.sramEn.expect(true.B)
              h.io.row.expect(i.U)
              h.io.col.expect(j.U)
              h.io.sramWen.expect(true.B)
              h.io.data.expect(h.ones)
              h.clock.step()
              h.io.sramEn.expect(true.B)
              h.io.row.expect(i.U)
              h.io.col.expect(j.U)
              h.io.sramWen.expect(false.B)
              h.io.data.expect(h.ones)
              h.io.checkEn.expect(true.B)
              h.clock.step()
            }
          }
        }

        h.io.sramEn.expect(false.B)
        h.io.sramWen.expect(false.B)
        h.io.done.expect(true.B)
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

  it should "work for a cycle limit of 40" in {
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
      d.io.innerDim.poke(d.Dimension.row)
      d.io.maxElementIdx.poke(3.U)
      d.io.seed.poke(1.U)
      d.io.cycleLimit.poke(40.U(32.W))
      d.io.patternTable.poke(Vec.Lit(ones, ones, zeros, zeros))
      d.io.elementSequence.poke(
        Vec(4, new d.Element())
          .Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march)
      )
      d.clock.step()
      d.io.start.poke(false.B)
      d.io.done.expect(false.B)

      for (i <- 0 until 39) {
        d.clock.step()
        d.io.done.expect(false.B)
        d.io.sramEn.expect(true.B)
      }

      d.clock.step()
      d.io.sramEn.expect(false.B)
      d.io.sramWen.expect(false.B)
      d.io.done.expect(true.B)
      d.io.cycle.expect(40.U)
    }
  }

  it should "work for deterministic March C- test with cols in inner loop" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 8,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>

        val h = new ProgrammableBistHelpers(d)

        h.clock.setTimeout((h.maxCols + 1) * (h.maxRows + 1) * 4 * 4)
        h.io.start.poke(true.B)
        h.io.en.poke(true.B)
        h.io.maxRowAddr.poke(h.maxRows.U)
        h.io.maxColAddr.poke(h.maxCols.U)
        h.io.innerDim.poke(h.d.Dimension.col)
        h.io.maxElementIdx.poke(5.U)
        h.io.seed.poke(1.U)
        h.io.patternTable.poke(Vec.Lit(h.zeros, h.ones, h.bp0, h.zeros))
        h.io.elementSequence.poke(
          h.marchCm(2, 1)
        )
        h.clock.step()
        h.clock.step()
        h.clock.step()
        h.io.start.poke(false.B)

        for (i <- 0 to h.maxRows) {
          for (j <- 0 to h.maxCols) {
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (i <- 0 to h.maxRows) {
          for (j <- 0 to h.maxCols) {
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
          }
        }
        for (i <- 0 to h.maxRows) {
          for (j <- 0 to h.maxCols) {
            h.expectRead(i, j, h.bp0f)
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (i <- (0 to h.maxRows).reverse) {
          for (j <- (0 to h.maxCols).reverse) {
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
          }
        }
        for (i <- (0 to h.maxRows).reverse) {
          for (j <- (0 to h.maxCols).reverse) {
            h.expectRead(i, j, h.bp0f)
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (i <- 0 to h.maxRows) {
          for (j <- 0 to h.maxCols) {
            h.expectRead(i, j, h.bp0)
          }
        }

        h.expectDone()
    }
  }

  it should "work for deterministic March C- test with rows in inner loop" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 8,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>

        val h = new ProgrammableBistHelpers(d)

        h.clock.setTimeout((h.maxCols + 1) * (h.maxRows + 1) * 4 * 4)
        h.io.start.poke(true.B)
        h.io.en.poke(true.B)
        h.io.maxRowAddr.poke(h.maxRows.U)
        h.io.maxColAddr.poke(h.maxCols.U)
        h.io.innerDim.poke(h.d.Dimension.row)
        h.io.maxElementIdx.poke(5.U)
        h.io.seed.poke(1.U)
        h.io.patternTable.poke(Vec.Lit(h.zeros, h.ones, h.bp0, h.zeros))
        h.io.elementSequence.poke(
          h.marchCm(2, 1)
        )
        h.clock.step()
        h.clock.step()
        h.clock.step()
        h.io.start.poke(false.B)

        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
          }
        }
        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectRead(i, j, h.bp0f)
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (j <- (0 to h.maxCols).reverse) {
          for (i <- (0 to h.maxRows).reverse) {
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
          }
        }
        for (j <- (0 to h.maxCols).reverse) {
          for (i <- (0 to h.maxRows).reverse) {
            h.expectRead(i, j, h.bp0f)
            h.expectWrite(i, j, h.bp0)
          }
        }
        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectRead(i, j, h.bp0)
          }
        }

        h.expectDone()
    }
  }

  it should "work for march-wait-march test with rows in inner loop" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 8,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>

        val h = new ProgrammableBistHelpers(d)

        h.clock.setTimeout((h.maxCols + 1) * (h.maxRows + 1) * 4 * 4)
        h.io.start.poke(true.B)
        h.io.en.poke(true.B)
        h.io.maxRowAddr.poke(h.maxRows.U)
        h.io.maxColAddr.poke(h.maxCols.U)
        h.io.innerDim.poke(h.d.Dimension.row)
        h.io.maxElementIdx.poke(2.U)
        h.io.seed.poke(1.U)
        h.io.patternTable.poke(Vec.Lit(h.zeros, h.ones, h.bp0, h.zeros))
        h.io.elementSequence.poke(
          h.marchWaitMarch
        )
        h.clock.step()
        h.clock.step()
        h.clock.step()
        h.io.start.poke(false.B)

        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectWrite(i, j, h.bp0)
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
            h.expectRead(i, j, h.bp0f)
          }
        }

        h.expectWait(100)

        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectWrite(i, j, h.bp0)
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
            h.expectRead(i, j, h.bp0f)
          }
        }

        h.expectDone()
    }
  }

  it should "work for march-wait-march test with cycle limit expiring during wait" in {
    test(
      new ProgrammableBist(
        new ProgrammableBistParams(
          patternTableLength = 4,
          elementTableLength = 8,
          operationsPerElement = 4
        )
      )
    ).withAnnotations(Seq(PrintFullStackTraceAnnotation, WriteVcdAnnotation)) {
      d =>

        val h = new ProgrammableBistHelpers(d)

        h.clock.setTimeout((h.maxCols + 1) * (h.maxRows + 1) * 4 * 4)
        h.io.start.poke(true.B)
        h.io.en.poke(true.B)
        h.io.maxRowAddr.poke(h.maxRows.U)
        h.io.maxColAddr.poke(h.maxCols.U)
        h.io.innerDim.poke(h.d.Dimension.row)
        h.io.maxElementIdx.poke(2.U)
        h.io.seed.poke(1.U)
        h.io.patternTable.poke(Vec.Lit(h.zeros, h.ones, h.bp0, h.zeros))
        h.io.elementSequence.poke(
          h.marchWaitMarch
        )
        val cycleLimit = 4 * (h.maxCols + 1) * (h.maxRows + 1) + 73
        h.io.cycleLimit.poke(cycleLimit.U(32.W))
        h.clock.step()
        h.clock.step()
        h.clock.step()
        h.io.start.poke(false.B)

        for (j <- 0 to h.maxCols) {
          for (i <- 0 to h.maxRows) {
            h.expectWrite(i, j, h.bp0)
            h.expectRead(i, j, h.bp0)
            h.expectWrite(i, j, h.bp0f)
            h.expectRead(i, j, h.bp0f)
          }
        }

        h.expectWait(73)
        h.expectDone()
    }
  }
}
