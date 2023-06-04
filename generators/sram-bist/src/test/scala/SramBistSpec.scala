package srambist

import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.VecLiterals._
import chisel3.experimental.BundleLiterals._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

import srambist.analog.SramParams
import srambist.sramharness.SaeSrc
import srambist.programmablebist.ProgrammableBistParams
import srambist.SramBistCtrlRegs._

class SramBistTestHelpers(val c: SramBist) {
  val maxRows = 15;
  val maxCols = 3;
  val readOp = chiselTypeOf(
    c.bistTop.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bistTop.bist.OperationType.read,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bistTop.bist.FlipType.unflipped
  )
  val writeOp = chiselTypeOf(
    c.bistTop.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bistTop.bist.OperationType.write,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 1.U,
    _.flipped -> c.bistTop.bist.FlipType.unflipped
  )
  val readFlippedOp = chiselTypeOf(
    c.bistTop.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bistTop.bist.OperationType.read,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bistTop.bist.FlipType.flipped
  )
  val writeFlippedOp = chiselTypeOf(
    c.bistTop.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bistTop.bist.OperationType.write,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 1.U,
    _.flipped -> c.bistTop.bist.FlipType.flipped
  )
  val opElementList = Vec(8, new c.bistTop.bist.Operation()).Lit(
    0 -> writeOp,
    1 -> readOp,
    2 -> writeFlippedOp,
    3 -> readFlippedOp,
    4 -> readOp,
    5 -> readOp,
    6 -> readOp,
    7 -> readOp
  )
  val opElement =
    chiselTypeOf(c.bistTop.bist.io.elementSequence(0).operationElement).Lit(
      _.operations -> opElementList,
      _.maxIdx -> 3.U,
      _.dir -> c.bistTop.bist.Direction.up,
      _.numAddrs -> 0.U
    )
  val opElementFail =
    chiselTypeOf(c.bistTop.bist.io.elementSequence(0).operationElement).Lit(
      _.operations -> opElementList,
      _.maxIdx -> 4.U,
      _.dir -> c.bistTop.bist.Direction.up,
      _.numAddrs -> 0.U
    )
  val waitElement =
    chiselTypeOf(c.bistTop.bist.io.elementSequence(0).waitElement)
      .Lit(_.cyclesToWait -> 0.U)
  val march = chiselTypeOf(c.bistTop.bist.io.elementSequence(0)).Lit(
    _.operationElement -> opElement,
    _.waitElement -> waitElement,
    _.elementType -> c.bistTop.bist.ElementType.rwOp
  )
  val marchFail = chiselTypeOf(c.bistTop.bist.io.elementSequence(0)).Lit(
    _.operationElement -> opElementFail,
    _.waitElement -> waitElement,
    _.elementType -> c.bistTop.bist.ElementType.rwOp
  )
  val zeros = 0.U(32.W)
  val ones = "hffffffff".U(32.W)
  val bp0 = "h5f1a950d".U(32.W)
  val bp0f = "ha0e56af2".U(32.W)
  val patternTable = Vec(8, UInt(32.W))
    .Lit(
      0 -> zeros,
      1 -> ones,
      2 -> bp0,
      3 -> bp0f,
      4 -> zeros,
      5 -> ones,
      6 -> zeros,
      7 -> zeros
    )
  val elementSequence = Vec(8, new c.bistTop.bist.Element())
    .Lit(
      0 -> march,
      1 -> march,
      2 -> march,
      3 -> march,
      4 -> march,
      5 -> march,
      6 -> march,
      7 -> march
    )
  val elementSequenceFail = Vec(8, new c.bistTop.bist.Element())
    .Lit(
      0 -> marchFail,
      1 -> march,
      2 -> march,
      3 -> march,
      4 -> march,
      5 -> march,
      6 -> march,
      7 -> march
    )

  // def populateSramRegisters(
  //     addr: UInt,
  //     din: UInt,
  //     mask: UInt,
  //     we: Bool,
  //     sramId: UInt,
  //     sramSel: SramSrc.Type,
  //     saeSel: SaeSrc.Type
  // ): Unit = {
  //   // Interleave with clock steps to make sure that operations do not occur while registers are being set up.
  //   // Will be tested more thoroughly with scan chain module.
  //   c.io.addr.poke(addr)
  //   c.clock.step()
  //   c.io.din.poke(din)
  //   c.clock.step()
  //   c.io.mask.poke(mask)
  //   c.clock.step()
  //   c.io.we.poke(we)
  //   c.clock.step()
  //   c.io.sramId.poke(sramId)
  //   c.clock.step()
  //   c.io.sramSel.poke(sramSel)
  //   c.clock.step()
  //   c.io.saeSel.poke(saeSel)
  //   c.clock.step()
  // }

  // def executeScanChainSramOp(): Unit = {
  //   // Once all registers are set up, enable SRAMs for one cycle.
  //   c.io.sramEn.poke(true.B)
  //   c.clock.step()
  //   c.io.sramEn.poke(false.B)
  //   c.clock.step()
  // }

  // def executeMmioOp(): Unit = {
  //   // Once all registers are set up, enable SRAMs for one cycle.
  //   c.io.ex.poke(true.B)
  //   c.clock.step()
  //   c.io.ex.poke(false.B)
  //   c.clock.step()
  //   while (!c.io.done.peek().litToBoolean) {
  //     c.clock.step()
  //   }
  // }

  // def populateBistRegisters(
  //     bistRandSeed: UInt,
  //     bistSigSeed: UInt,
  //     bistMaxRowAddr: UInt,
  //     bistMaxColAddr: UInt,
  //     bistInnerDim: c.bist.Dimension.Type,
  //     bistPatternTable: Vec[UInt],
  //     bistElementSequence: Vec[c.bist.Element],
  //     bistMaxElementIdx: UInt,
  //     bistCycleLimit: UInt,
  //     bistStopOnFailure: Bool,
  //     sramId: UInt,
  //     sramSel: SramSrc.Type,
  //     saeSel: SaeSrc.Type
  // ): Unit = {
  //   // Interleave with clock steps to make sure that operations do not occur while registers are being set up.
  //   // Will be tested more thoroughly with scan chain module.
  //   c.io.bistRandSeed.poke(bistRandSeed)
  //   c.clock.step()
  //   c.io.bistSigSeed.poke(bistSigSeed)
  //   c.clock.step()
  //   c.io.bistMaxRowAddr.poke(bistMaxRowAddr)
  //   c.clock.step()
  //   c.io.bistMaxColAddr.poke(bistMaxColAddr)
  //   c.clock.step()
  //   c.io.bistInnerDim.poke(bistInnerDim)
  //   c.clock.step()
  //   c.io.bistPatternTable.poke(bistPatternTable)
  //   c.clock.step()
  //   c.io.bistElementSequence.poke(bistElementSequence)
  //   c.clock.step()
  //   c.io.bistMaxElementIdx.poke(bistMaxElementIdx)
  //   c.clock.step()
  //   c.io.bistCycleLimit.poke(bistCycleLimit)
  //   c.clock.step()
  //   c.io.bistStopOnFailure.poke(bistStopOnFailure)
  //   c.clock.step()
  //   c.io.sramId.poke(sramId)
  //   c.clock.step()
  //   c.io.sramSel.poke(sramSel)
  //   c.clock.step()
  //   c.io.saeSel.poke(saeSel)
  //   c.clock.step()
  // }

  // def executeScanChainBistOp(): Unit = {
  //   // Once all registers are set up, enable the BIST until the test completes.
  //   c.io.bistEn.poke(true.B)
  //   c.clock.step()
  //   while (
  //     !c.io.bistDone.peek().litToBoolean && !c.io.bistFail.peek().litToBoolean
  //   ) {
  //     c.clock.step()
  //   }
  //   c.io.bistEn.poke(false.B)
  // }
}

class SramBistSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SramBist"
  it should "work with hardmacro SRAMs" in {
    test(
      new SramBist()(
        Parameters.empty
      )
    ) { c => }
  }
  it should "work with Write-Write-Read on chiseltest SRAMs" in {
    test(
      new SramBist(
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(VcsBackendAnnotation, WriteVcdAnnotation)) { d =>
      val scanIn = (width: Int, value: Int) => {
        d.io.top.sramScanEn.poke(true.B)
        var bitSeq = Seq[Int]()
        var v = value
        for (i <- 1 to width) {
          bitSeq = (v % 2) +: bitSeq
          v /= 2
        }
        for (bit <- bitSeq) {
          d.io.top.sramScanIn.poke((bit == 1).B)
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanOutAndAssert = (width: Int, value: Int) => {
        d.io.top.sramScanEn.poke(true.B)
        var num = 0
        for (i <- 1 to width) {
          var bit = d.io.top.sramScanOut.peek().litToBoolean
          var digit = if (bit) 1 else 0
          num = num * 2 + digit
          d.clock.step()
        }
        assert(num == value)
        d.io.top.sramScanEn.poke(false.B)
      }
      val scanOut = (width: Int) => {
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to width) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanClear = () => {
        d.io.top.sramScanIn.poke(false.B)
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to (TOTAL_REG_WIDTH + 2)) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      d.clock.setTimeout(0)

      d.io.top.sramExtEn.poke(true.B)
      d.io.top.sramScanMode.poke(true.B)
      d.io.top.sramEn.poke(false.B)
      d.io.top.bistEn.poke(false.B)
      d.io.top.bistStart.poke(false.B)

      scanClear()
      scanIn(REG_WIDTH(WE), 1)
      scanIn(REG_WIDTH(MASK), 15)
      scanIn(REG_WIDTH(DIN), 20)
      scanIn(REG_WIDTH(ADDR), 25)

      d.io.top.sramEn.poke(true.B)
      d.clock.step()
      d.io.top.sramEn.poke(false.B)

      scanClear()
      scanIn(REG_WIDTH(WE), 1)
      scanIn(REG_WIDTH(MASK), 15)
      scanIn(REG_WIDTH(DIN), 17)
      scanIn(REG_WIDTH(ADDR), 30)

      d.io.top.sramEn.poke(true.B)
      d.clock.step()
      d.io.top.sramEn.poke(false.B)

      scanClear()
      scanIn(REG_WIDTH(WE), 0)
      scanIn(REG_WIDTH(MASK), 15)
      scanIn(REG_WIDTH(DIN), 0)
      scanIn(REG_WIDTH(ADDR), 25)
      d.io.top.sramScanIn.poke(false.B)

      d.io.top.sramEn.poke(true.B)
      d.clock.step()
      d.io.top.sramEn.poke(false.B)
      d.clock.step()

      d.io.top.sramScanEn.poke(true.B)
      scanOut(SCAN_OUT_OFFSET(DOUT))
      scanOutAndAssert(REG_WIDTH(DOUT), 20)
      d.io.top.sramScanEn.poke(false.B)
    }
  }

  it should "work with march BIST on chiseltest SRAMs" in {
    test(
      new SramBist(
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(VcsBackendAnnotation, WriteVcdAnnotation)) { d =>
      val scanIn = (width: Int, value: BigInt) => {
        d.io.top.sramScanEn.poke(true.B)
        var bitSeq = Seq[Int]()
        var v = value
        for (i <- 1 to width) {
          bitSeq = (v % 2).toInt +: bitSeq
          v /= 2
        }
        for (bit <- bitSeq) {
          d.io.top.sramScanIn.poke((bit == 1).B)
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanOutAndAssert = (width: Int, value: BigInt) => {
        d.io.top.sramScanEn.poke(true.B)
        var num: Long = 0
        for (i <- 1 to width) {
          var bit = d.io.top.sramScanOut.peek().litToBoolean
          var digit = if (bit) 1 else 0
          num = num * 2 + digit
          d.clock.step()
        }
        assert(num == value)
        d.io.top.sramScanEn.poke(false.B)
      }
      val scanOut = (width: Int) => {
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to width) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanClear = () => {
        d.io.top.sramScanIn.poke(false.B)
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to (TOTAL_REG_WIDTH + 2)) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      d.clock.setTimeout(0)
      val h = new SramBistTestHelpers(d)
      val eltSeq = h.elementSequence.litValue
      val patTable = h.patternTable.litValue

      d.io.top.sramExtEn.poke(false.B)
      d.io.top.sramScanMode.poke(true.B)
      d.io.top.sramEn.poke(false.B)
      d.io.top.bistEn.poke(false.B)
      d.io.top.bistStart.poke(false.B)

      scanClear()
      scanIn(REG_WIDTH(BIST_STOP_ON_FAILURE), 1)
      scanIn(REG_WIDTH(BIST_CYCLE_LIMIT), 0)
      scanIn(REG_WIDTH(BIST_MAX_ELEMENT_IDX), 3)
      scanIn(REG_WIDTH(BIST_PATTERN_TABLE), patTable)
      scanIn(REG_WIDTH(BIST_ELEMENT_SEQUENCE), eltSeq)
      scanIn(REG_WIDTH(BIST_INNER_DIM), 0)
      scanIn(REG_WIDTH(BIST_MAX_COL_ADDR), 3)
      scanIn(REG_WIDTH(BIST_MAX_ROW_ADDR), 15)
      scanIn(REG_WIDTH(BIST_SIG_SEED), 1)
      scanIn(REG_WIDTH(BIST_RAND_SEED), 1)
      scanIn(REG_WIDTH(DONE), 0)
      scanIn(REG_WIDTH(TDC), 0)
      scanIn(REG_WIDTH(DOUT), 0)
      scanIn(REG_WIDTH(SAE_SEL), 0)
      scanIn(REG_WIDTH(SAE_CTL), 0)
      scanIn(REG_WIDTH(SRAM_SEL), 1)
      scanIn(REG_WIDTH(SRAM_ID), 0)
      scanIn(REG_WIDTH(WE), 0)
      scanIn(REG_WIDTH(MASK), 511)
      scanIn(REG_WIDTH(DIN), 0)
      scanIn(REG_WIDTH(ADDR), 0)
      d.io.top.sramScanIn.poke(false.B)

      println("BIST element sequence register contents:")
      for (i <- 0 until 16) {
        println(d.io.mmio.bistElementSequenceMmio(i).q.peek().litValue);
      }

      d.io.top.bistStart.poke(true.B)
      d.clock.step()
      d.clock.step()
      d.clock.step()
      d.io.top.bistStart.poke(false.B)
      d.io.top.bistEn.poke(true.B)

      for (i <- 0 to 4 * 4 * 16 * 4 + 3) {
        d.clock.step()
      }

      d.io.top.bistDone.expect(true.B)
      d.io.top.bistEn.poke(false.B)

      val misrModel = new MaxPeriodFibonacciXORMISRModel(32)
      for (i <- 1 to (h.maxRows + 1) * (h.maxCols + 1) * 4) {
        misrModel.add(0xa0e56af2L)
        misrModel.add(0x5f1a950dL)
      }

      println(s"Expected signature = ${misrModel.state}")
      scanOutAndAssert(REG_WIDTH(BIST_SIGNATURE), misrModel.state)
      scanOut(REG_WIDTH(BIST_RECEIVED))
      scanOut(REG_WIDTH(BIST_EXPECTED))
      scanOut(REG_WIDTH(BIST_FAIL_CYCLE))
      scanOutAndAssert(REG_WIDTH(BIST_FAIL), 0)
    }
  }
  it should "work with a failed march BIST on chiseltest SRAMs" in {
    test(
      new SramBist(
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(VcsBackendAnnotation, WriteVcdAnnotation)) { d =>
      val scanIn = (width: Int, value: BigInt) => {
        d.io.top.sramScanEn.poke(true.B)
        var bitSeq = Seq[Int]()
        var v = value
        for (i <- 1 to width) {
          bitSeq = (v % 2).toInt +: bitSeq
          v /= 2
        }
        for (bit <- bitSeq) {
          d.io.top.sramScanIn.poke((bit == 1).B)
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanOutAndAssert = (width: Int, value: BigInt) => {
        d.io.top.sramScanEn.poke(true.B)
        var num: Long = 0
        for (i <- 1 to width) {
          var bit = d.io.top.sramScanOut.peek().litToBoolean
          var digit = if (bit) 1 else 0
          num = num * 2 + digit
          d.clock.step()
        }
        assert(num == value)
        d.io.top.sramScanEn.poke(false.B)
      }
      val scanOut = (width: Int) => {
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to width) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      val scanClear = () => {
        d.io.top.sramScanIn.poke(false.B)
        d.io.top.sramScanEn.poke(true.B)
        for (i <- 1 to (TOTAL_REG_WIDTH + 2)) {
          d.clock.step()
        }
        d.io.top.sramScanEn.poke(false.B)
      }

      d.clock.setTimeout(0)
      val h = new SramBistTestHelpers(d)
      val eltSeq = h.elementSequenceFail.litValue
      val patTable = h.patternTable.litValue

      d.io.top.sramExtEn.poke(false.B)
      d.io.top.sramScanMode.poke(true.B)
      d.io.top.sramEn.poke(false.B)
      d.io.top.bistEn.poke(false.B)
      d.io.top.bistStart.poke(false.B)

      scanClear()
      scanIn(REG_WIDTH(BIST_STOP_ON_FAILURE), 1)
      scanIn(REG_WIDTH(BIST_CYCLE_LIMIT), 0)
      scanIn(REG_WIDTH(BIST_MAX_ELEMENT_IDX), 3)
      scanIn(REG_WIDTH(BIST_PATTERN_TABLE), patTable)
      scanIn(REG_WIDTH(BIST_ELEMENT_SEQUENCE), eltSeq)
      scanIn(REG_WIDTH(BIST_INNER_DIM), 0)
      scanIn(REG_WIDTH(BIST_MAX_COL_ADDR), 3)
      scanIn(REG_WIDTH(BIST_MAX_ROW_ADDR), 15)
      scanIn(REG_WIDTH(BIST_SIG_SEED), 1)
      scanIn(REG_WIDTH(BIST_RAND_SEED), 1)
      scanIn(REG_WIDTH(DONE), 0)
      scanIn(REG_WIDTH(TDC), 0)
      scanIn(REG_WIDTH(DOUT), 0)
      scanIn(REG_WIDTH(SAE_SEL), 0)
      scanIn(REG_WIDTH(SAE_CTL), 0)
      scanIn(REG_WIDTH(SRAM_SEL), 1)
      scanIn(REG_WIDTH(SRAM_ID), 0)
      scanIn(REG_WIDTH(WE), 0)
      scanIn(REG_WIDTH(MASK), 511)
      scanIn(REG_WIDTH(DIN), 0)
      scanIn(REG_WIDTH(ADDR), 0)
      d.io.top.sramScanIn.poke(false.B)

      println("BIST element sequence register contents:")
      for (i <- 0 until 16) {
        println(d.io.mmio.bistElementSequenceMmio(i).q.peek().litValue);
      }

      d.io.top.bistStart.poke(true.B)
      d.clock.step()
      d.clock.step()
      d.clock.step()
      d.io.top.bistStart.poke(false.B)
      d.io.top.bistEn.poke(true.B)

      for (i <- 0 to 4 * 4 * 16 * 4 + 3) {
        d.clock.step()
      }

      d.io.top.bistDone.expect(true.B)
      d.io.top.bistEn.poke(false.B)

      val misrModel = new MaxPeriodFibonacciXORMISRModel(32)
      for (i <- 1 to (h.maxRows + 1) * (h.maxCols + 1) * 4) {
        misrModel.add(0xa0e56af2L)
        misrModel.add(0x5f1a950dL)
      }

      scanOut(SCAN_OUT_OFFSET(BIST_SIGNATURE))
      scanOut(REG_WIDTH(BIST_SIGNATURE))
      scanOutAndAssert(REG_WIDTH(BIST_RECEIVED), 0x5f1a950dL)
      scanOutAndAssert(REG_WIDTH(BIST_EXPECTED), 0xa0e56af2L)
      scanOutAndAssert(REG_WIDTH(BIST_FAIL_CYCLE), 5)
      scanOutAndAssert(REG_WIDTH(BIST_FAIL), 1)
    }
  }
}
