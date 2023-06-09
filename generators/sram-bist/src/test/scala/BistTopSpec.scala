package srambist

import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.simulator.VerilatorFlags
import chisel3.experimental.VecLiterals._
import chisel3.experimental.BundleLiterals._
import org.chipsalliance.cde.config.Parameters
import org.scalatest.flatspec.AnyFlatSpec

import srambist.analog.SramParams
import srambist.sramharness.SaeSrc
import srambist.programmablebist.ProgrammableBistParams

class BistTopTestHelpers(val c: BistTop) {
  val maxRows = 15;
  val maxCols = 7;
  val readOp = chiselTypeOf(
    c.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bist.OperationType.read,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bist.FlipType.unflipped
  )
  val writeOp = chiselTypeOf(
    c.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bist.OperationType.write,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bist.FlipType.unflipped
  )
  val readFlippedOp = chiselTypeOf(
    c.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bist.OperationType.read,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bist.FlipType.flipped
  )
  val writeFlippedOp = chiselTypeOf(
    c.bist.io.elementSequence(0).operationElement.operations(0)
  ).Lit(
    _.operationType -> c.bist.OperationType.write,
    _.randData -> false.B,
    _.randMask -> false.B,
    _.dataPatternIdx -> 3.U,
    _.maskPatternIdx -> 0.U,
    _.flipped -> c.bist.FlipType.flipped
  )

  // A deterministic operation (R/W) with random data and fixed mask
  def randDataOp(
      write: Boolean = false,
      maskIndex: Int = 0
  ): c.bist.Operation = {
    val operationType =
      if (write) c.bist.OperationType.write else c.bist.OperationType.read
    chiselTypeOf(
      c.bist.io.elementSequence(0).operationElement.operations(0)
    ).Lit(
      _.operationType -> operationType,
      _.randData -> true.B,
      _.randMask -> false.B,
      _.dataPatternIdx -> 0.U,
      _.maskPatternIdx -> maskIndex.U,
      _.flipped -> c.bist.FlipType.unflipped
    )
  }

  // A deterministic operation (R/W) with random data and mask
  def randDataMaskOp(write: Boolean = false): c.bist.Operation = {
    val operationType =
      if (write) c.bist.OperationType.write else c.bist.OperationType.read
    chiselTypeOf(
      c.bist.io.elementSequence(0).operationElement.operations(0)
    ).Lit(
      _.operationType -> operationType,
      _.randData -> true.B,
      _.randMask -> true.B,
      _.dataPatternIdx -> 0.U,
      _.maskPatternIdx -> 0.U,
      _.flipped -> c.bist.FlipType.unflipped
    )
  }

  // A totally random operation (random write enable, data, mask)
  def randOp(): c.bist.Operation = {
    chiselTypeOf(
      c.bist.io.elementSequence(0).operationElement.operations(0)
    ).Lit(
      _.operationType -> c.bist.OperationType.rand,
      _.randData -> true.B,
      _.randMask -> true.B,
      _.dataPatternIdx -> 0.U,
      _.maskPatternIdx -> 0.U,
      _.flipped -> c.bist.FlipType.unflipped
    )
  }

  val opElementList = Vec(4, new c.bist.Operation()).Lit(
    0 -> writeOp,
    1 -> readOp,
    2 -> writeFlippedOp,
    3 -> readFlippedOp
  )
  val opElement =
    chiselTypeOf(c.bist.io.elementSequence(0).operationElement).Lit(
      _.operations -> opElementList,
      _.maxIdx -> 3.U,
      _.dir -> c.bist.Direction.up,
      _.numAddrs -> 0.U
    )
  val opElementListFail = Vec(4, new c.bist.Operation()).Lit(
    0 -> writeOp,
    1 -> readOp,
    2 -> writeFlippedOp,
    3 -> readOp
  )
  val opElementFail =
    chiselTypeOf(c.bist.io.elementSequence(0).operationElement).Lit(
      _.operations -> opElementListFail,
      _.maxIdx -> 3.U,
      _.dir -> c.bist.Direction.up,
      _.numAddrs -> 0.U
    )

  val randOpElementList = Vec(4, new c.bist.Operation()).Lit(
    0 -> writeOp,
    1 -> randOp(),
    2 -> randDataOp(true),
    3 -> randDataMaskOp()
  )
  val randOpElement =
    chiselTypeOf(c.bist.io.elementSequence(0).operationElement).Lit(
      _.operations -> randOpElementList,
      _.maxIdx -> 3.U,
      _.dir -> c.bist.Direction.up,
      _.numAddrs -> 0.U
    )
  val waitElement = chiselTypeOf(c.bist.io.elementSequence(0).waitElement)
    .Lit(_.cyclesToWait -> 0.U)
  val march = chiselTypeOf(c.bist.io.elementSequence(0)).Lit(
    _.operationElement -> opElement,
    _.waitElement -> waitElement,
    _.elementType -> c.bist.ElementType.rwOp
  )
  val marchFail = chiselTypeOf(c.bist.io.elementSequence(0)).Lit(
    _.operationElement -> opElementFail,
    _.waitElement -> waitElement,
    _.elementType -> c.bist.ElementType.rwOp
  )
  val marchRand = chiselTypeOf(c.bist.io.elementSequence(0)).Lit(
    _.operationElement -> randOpElement,
    _.waitElement -> waitElement,
    _.elementType -> c.bist.ElementType.rwOp
  )
  val zeros = 0.U(32.W)
  val ones = "hffffffff".U(32.W)

  def populateSramRegisters(
      addr: UInt,
      din: UInt,
      mask: UInt,
      we: Bool,
      sramId: UInt,
      sramSel: SramSrc.Type,
      saeSel: SaeSrc.Type
  ): Unit = {
    // Interleave with clock steps to make sure that operations do not occur while registers are being set up.
    // Will be tested more thoroughly with scan chain module.
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

  def executeScanChainSramOp(): Unit = {
    // Once all registers are set up, enable SRAMs for one cycle.
    c.io.sramEn.poke(true.B)
    c.clock.step()
    c.io.sramEn.poke(false.B)
    c.clock.step()
  }

  def executeMmioOp(): Unit = {
    // Once all registers are set up, enable SRAMs for one cycle.
    c.io.ex.poke(true.B)
    c.clock.step()
    c.io.done.expect(false.B)
    c.io.ex.poke(false.B)
    c.clock.step()
    while (!c.io.done.peek().litToBoolean) {
      c.clock.step()
    }
  }

  def populateBistRegisters(
      bistRandSeed: UInt,
      bistSigSeed: UInt,
      bistMaxRowAddr: UInt,
      bistMaxColAddr: UInt,
      bistInnerDim: c.bist.Dimension.Type,
      bistPatternTable: Vec[UInt],
      bistElementSequence: Vec[c.bist.Element],
      bistMaxElementIdx: UInt,
      bistCycleLimit: UInt,
      bistStopOnFailure: Bool,
      sramId: UInt,
      sramSel: SramSrc.Type,
      saeSel: SaeSrc.Type
  ): Unit = {
    // Interleave with clock steps to make sure that operations do not occur while registers are being set up.
    // Will be tested more thoroughly with scan chain module.
    c.io.bistRandSeed.poke(bistRandSeed)
    c.clock.step()
    c.io.bistSigSeed.poke(bistSigSeed)
    c.clock.step()
    c.io.bistMaxRowAddr.poke(bistMaxRowAddr)
    c.clock.step()
    c.io.bistMaxColAddr.poke(bistMaxColAddr)
    c.clock.step()
    c.io.bistInnerDim.poke(bistInnerDim)
    c.clock.step()
    c.io.bistPatternTable.poke(bistPatternTable)
    c.clock.step()
    c.io.bistElementSequence.poke(bistElementSequence)
    c.clock.step()
    c.io.bistMaxElementIdx.poke(bistMaxElementIdx)
    c.clock.step()
    c.io.bistCycleLimit.poke(bistCycleLimit)
    c.clock.step()
    c.io.bistStopOnFailure.poke(bistStopOnFailure)
    c.clock.step()
    c.io.sramId.poke(sramId)
    c.clock.step()
    c.io.sramSel.poke(sramSel)
    c.clock.step()
    c.io.saeSel.poke(saeSel)
    c.clock.step()
  }

  def executeScanChainBistOp(): Unit = {
    // Once all registers are set up, enable the BIST until the test completes.
    c.io.bistEn.poke(true.B)
    c.clock.step()
    while (!c.io.bistDone.peek().litToBoolean) {
      c.clock.step()
    }
    c.io.bistEn.poke(false.B)
  }

  def testSramMethodFull(executeFn: () => Unit): Unit = {
    // Test write.
    populateSramRegisters(
      0.U,
      "habcdabcd".U,
      "hf".U,
      true.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()

    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("habcdabcd".U)

    // Test write with mask.
    populateSramRegisters(
      0.U,
      0.U,
      5.U,
      true.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    c.io.dout.expect(
      "habcdabcd".U
    ) // Dout should retain its value while registers are being set up.
    executeFn()

    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("hab00ab00".U)

    // Test write to second SRAM.
    populateSramRegisters(
      0.U,
      "h12345678".U,
      "hf".U,
      true.B,
      1.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()

    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      1.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("h12345678".U)

    // Test that first SRAM retains original value.
    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("hab00ab00".U)

    // Test writing to extreme addresses of both SRAMs. Verify that original data doesn't change.
    populateSramRegisters(
      63.U,
      "h87654321".U,
      "hf".U,
      true.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()

    populateSramRegisters(
      63.U,
      0.U,
      0.U,
      false.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("h87654321".U)

    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      0.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("hab00ab00".U)

    populateSramRegisters(
      1023.U,
      "hdeadbeef".U,
      "hf".U,
      true.B,
      1.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()

    populateSramRegisters(
      1023.U,
      0.U,
      0.U,
      false.B,
      1.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("hdeadbeef".U)

    populateSramRegisters(
      0.U,
      0.U,
      0.U,
      false.B,
      1.U,
      SramSrc.mmio,
      SaeSrc.int
    )
    executeFn()
    c.io.dout.expect("h12345678".U)
  }

  def testBistMethodFull(scanChain: Boolean): Unit = {

    val maybeReset = () => {
      if (scanChain) {
        c.io.bistStart.poke(true.B)
        c.clock.step()
        c.io.bistStart.poke(false.B)
        c.clock.step()
        c.io.bistDone.expect(false.B)
        c.io.bistFail.expect(false.B)
      }
    }
    val executeOp = () => {
      if (scanChain) {
        executeScanChainBistOp()
      } else {
        executeMmioOp()
      }
    }

    populateBistRegisters(
      1.U,
      1.U,
      maxRows.U,
      maxCols.U,
      c.bist.Dimension.col,
      Vec.Lit(
        ones,
        ones,
        zeros,
        zeros
      ),
      Vec(4, new c.bist.Element())
        .Lit(
          0 -> march,
          1 -> march,
          2 -> march,
          3 -> march
        ),
      3.U,
      0.U,
      true.B,
      0.U,
      SramSrc.bist,
      SaeSrc.int
    )
    maybeReset()
    executeOp()
    c.io.bistDone.expect(true.B)
    c.io.bistFail.expect(false.B)

  }
}

class BistTopSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "BistTop"
  it should "work with hardmacro SRAMs" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new freechips.rocketchip.subsystem.WithClockGateModel
      )
    ).withAnnotations(Seq(VcsBackendAnnotation, WriteVcdAnnotation)) { c =>
      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )

      // ******************
      // SCAN CHAIN -> SRAM
      // ******************

      testhelpers.c.io.sramExtEn.poke(true.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.sramEn.poke(false.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testhelpers.testSramMethodFull(testhelpers.executeScanChainSramOp)

      // ************
      // MMIO -> SRAM
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testhelpers.testSramMethodFull(testhelpers.executeMmioOp)

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testhelpers.testBistMethodFull(true)

      // ************
      // MMIO -> BIST
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testhelpers.testBistMethodFull(false)

    }
  }
  it should "execute scan chain operations" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4
      )

      // ******************
      // SCAN CHAIN -> SRAM
      // ******************

      testhelpers.c.io.sramExtEn.poke(true.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.sramEn.poke(false.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testhelpers.testSramMethodFull(testhelpers.executeScanChainSramOp)
    }
  }
  it should "execute MMIO operations" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4
      )

      // ************
      // MMIO -> SRAM
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testhelpers.testSramMethodFull(testhelpers.executeMmioOp)
    }
  }
  it should "start the BIST via scan chain" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 8
      )

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testhelpers.testBistMethodFull(true)
    }
  }
  it should "start the BIST via MMIO" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )

      // ************
      // MMIO -> BIST
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testhelpers.testBistMethodFull(false)

    }
  }

  it should "correctly compute the signature of a basic BIST sequence" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testhelpers.testBistMethodFull(false)

      val misrModel = new MaxPeriodFibonacciXORMISRModel(32)
      for (
        i <- 1 to (testhelpers.maxRows + 1) * (testhelpers.maxCols + 1) * 4
      ) {
        misrModel.add(0)
        misrModel.add(0xffffffffL)
      }

      testhelpers.c.io.bistSignature.expect(misrModel.state.U)
    }
  }

  it should "correctly compute the signature of a basic BIST sequence with a custom seed" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 2 * 2
      )

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          55.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros,
            testhelpers.ones
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          1.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(false.B)
      }

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)

      val misrModel = new MaxPeriodFibonacciXORMISRModel(32, Some(55))
      for (
        i <- 1 to (testhelpers.maxRows + 1) * (testhelpers.maxCols + 1) * 2
      ) {
        misrModel.add(0xffffffffL)
        misrModel.add(0)
      }

      println(misrModel.state)
      testhelpers.c.io.bistSignature.expect(misrModel.state.U)

      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testBistMethod(true)

      testhelpers.c.io.bistSignature.expect(misrModel.state.U)
    }
  }

  it should "assert BIST fail with the correct cycle, expected, and received values" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 2 * 2
      )

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          55.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.marchFail,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(true.B)
        testhelpers.c.io.bistFailCycle.expect(4.U)
        testhelpers.c.io.bistExpected.expect(0.U)
        testhelpers.c.io.bistReceived.expect(0xffffffffL.U)
      }

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)
    }
  }

  it should "return consistent hashes on random BIST runs with the same seed" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      testhelpers.c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 2 * 2
      )

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.marchRand,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          1.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(false.B)
      }

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)

      val signature = testhelpers.c.io.bistSignature.peek();

      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)
      testBistMethod(true)

      testhelpers.c.io.bistSignature.expect(signature);
    }
  }
  it should "work with 16 data bit chiseltest SRAMs" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(16, 8, 64, 16)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.none)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )
      val testSramMethod = (executeFn: () => Unit) => {
        // Test write to small SRAM.
        testhelpers.populateSramRegisters(
          0.U,
          "hab6f6bbd".U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        assert((testhelpers.c.io.dout.peek().litValue & 0xffff) == 0x6bbd)

        // Test writing to extreme addresses of the small SRAM.
        testhelpers.populateSramRegisters(
          63.U,
          "h87654321".U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          63.U,
          0.U,
          0.U,
          false.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        // testhelpers.c.io.dout(15, 0).expect("h4321".U)
        assert((testhelpers.c.io.dout.peek().litValue & 0xffff) == 0x4321)
        // Verify that original write didn't change.
        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        // testhelpers.c.io.dout(15, 0).expect("h6bbd".U)
        assert((testhelpers.c.io.dout.peek().litValue & 0xffff) == 0x6bbd)

      }

      val testBistMethod = (scanChain: Boolean) => {
        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        maybeReset()
        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.row,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(false.B)
      }

      // ******************
      // SCAN CHAIN -> SRAM
      // ******************

      testhelpers.c.io.sramExtEn.poke(true.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.sramEn.poke(false.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testSramMethod(testhelpers.executeScanChainSramOp)

      // ************
      // MMIO -> SRAM
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testSramMethod(testhelpers.executeMmioOp)

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testBistMethod(true)

      // ************
      // MMIO -> BIST
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)

    }
  }

  it should "detect stuck at faults in chiseltest SRAMs" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.stuckAt)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )
      val testSramMethod = (executeFn: () => Unit) => {
        // Test write.
        testhelpers.populateSramRegisters(
          29.U,
          0.U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          29.U,
          "hffffffff".U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          29.U,
          0.U,
          0.U,
          false.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("hffffffdf".U)
      }

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(true.B)
        testhelpers.c.io.bistExpected.expect("hffffffff".U)
        testhelpers.c.io.bistReceived.expect("hffffffdf".U)
      }

      // ******************
      // SCAN CHAIN -> SRAM
      // ******************

      testhelpers.c.io.sramExtEn.poke(true.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.sramEn.poke(false.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testSramMethod(testhelpers.executeScanChainSramOp)

      // ************
      // MMIO -> SRAM
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testSramMethod(testhelpers.executeMmioOp)

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testBistMethod(true)

      // ************
      // MMIO -> BIST
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)

    }
  }

  it should "detect transition faults in chiseltest SRAMs" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.transition)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )
      val testSramMethod = (executeFn: () => Unit) => {
        testhelpers.populateSramRegisters(
          15.U,
          "hffffffff".U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          15.U,
          0.U,
          "hf".U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          15.U,
          0.U,
          0.U,
          false.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("h00000001".U)
      }

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            testhelpers.c.io.bistDone.expect(false.B)
            testhelpers.c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          testhelpers.maxCols.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(true.B)
        testhelpers.c.io.bistExpected.expect("h00000000".U)
        testhelpers.c.io.bistReceived.expect("h00000001".U)
      }

      // ******************
      // SCAN CHAIN -> SRAM
      // ******************

      testhelpers.c.io.sramExtEn.poke(true.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.sramEn.poke(false.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testSramMethod(testhelpers.executeScanChainSramOp)

      // ************
      // MMIO -> SRAM
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testSramMethod(testhelpers.executeMmioOp)

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testBistMethod(true)

      // ************
      // MMIO -> BIST
      // ************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(false.B)

      testBistMethod(false)

    }
  }

  it should "detect faults after first failure using a cycle limit" in {
    test(
      new BistTop(
        new BistTopParams(
          Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
          new ProgrammableBistParams(
            patternTableLength = 4,
            elementTableLength = 4,
            operationsPerElement = 4
          )
        )
      )(
        new WithChiseltestSrams(ChiseltestSramFailureMode.stuckAt)
      )
    ).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )
      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.io.bistStart.poke(true.B)
            c.clock.step()
            c.io.bistStart.poke(false.B)
            c.clock.step()
            c.io.bistDone.expect(false.B)
            c.io.bistFail.expect(false.B)
          }
        }
        val executeOp = () => {
          if (scanChain) {
            testhelpers.executeScanChainBistOp()
          } else {
            testhelpers.executeMmioOp()
          }
        }

        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          3.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          0.U,
          true.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(true.B)
        testhelpers.c.io.bistFailCycle.expect(120.U)
        testhelpers.c.io.bistExpected.expect("hffffffff".U)
        testhelpers.c.io.bistReceived.expect("hffffffdf".U)

        testhelpers.populateBistRegisters(
          1.U,
          1.U,
          testhelpers.maxRows.U,
          3.U,
          testhelpers.c.bist.Dimension.col,
          Vec.Lit(
            testhelpers.ones,
            testhelpers.ones,
            testhelpers.zeros,
            testhelpers.zeros
          ),
          Vec(4, new testhelpers.c.bist.Element())
            .Lit(
              0 -> testhelpers.march,
              1 -> testhelpers.march,
              2 -> testhelpers.march,
              3 -> testhelpers.march
            ),
          3.U,
          376.U,
          false.B,
          0.U,
          SramSrc.bist,
          SaeSrc.int
        )
        maybeReset()
        executeOp()
        testhelpers.c.io.bistDone.expect(true.B)
        testhelpers.c.io.bistFail.expect(true.B)
        testhelpers.c.io.bistFailCycle.expect(376.U)
        testhelpers.c.io.bistExpected.expect("hffffffff".U)
        testhelpers.c.io.bistReceived.expect("hffffffdf".U)
      }

      // ******************
      // SCAN CHAIN -> BIST
      // ******************

      testhelpers.c.io.sramExtEn.poke(false.B)
      testhelpers.c.io.sramScanMode.poke(true.B)
      testhelpers.c.io.bistEn.poke(false.B)

      testBistMethod(true)
    }
  }
}
