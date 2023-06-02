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
  val waitElement = chiselTypeOf(c.bist.io.elementSequence(0).waitElement)
    .Lit(_.cyclesToWait -> 0.U)
  val march = chiselTypeOf(c.bist.io.elementSequence(0)).Lit(
    _.operationElement -> opElement,
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
    while (
      !c.io.bistDone.peek().litToBoolean && !c.io.bistFail.peek().litToBoolean
    ) {
      c.clock.step()
    }
    c.io.bistEn.poke(false.B)
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
      val testSramMethod = (executeFn: () => Unit) => {
        // Test write.
        testhelpers.populateSramRegisters(
          0.U,
          "habcdabcd".U,
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
        testhelpers.c.io.dout.expect("habcdabcd".U)

        // Test write with mask.
        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          5.U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        testhelpers.c.io.dout.expect(
          "habcdabcd".U
        ) // Dout should retain its value while registers are being set up.
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
        testhelpers.c.io.dout.expect("hab00ab00".U)
        testhelpers.c.io.checkClk.expect(false.B)

        // Test write to second SRAM.
        testhelpers.populateSramRegisters(
          0.U,
          "h12345678".U,
          "hf".U,
          true.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("h12345678".U)

        // Test that first SRAM retains original value.
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
        testhelpers.c.io.dout.expect("hab00ab00".U)

        // Test writing to extreme addresses of both SRAMs. Verify that original data doesn't change.
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
        testhelpers.c.io.dout.expect("h87654321".U)

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
        testhelpers.c.io.dout.expect("hab00ab00".U)

        testhelpers.populateSramRegisters(
          1023.U,
          "hdeadbeef".U,
          "hf".U,
          true.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          1023.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("hdeadbeef".U)

        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("h12345678".U)
      }

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.reset.poke(true.B)
            c.clock.step()
            c.reset.poke(false.B)
            c.clock.step()
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
        // TODO: Use correct cycle limit.
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
  it should "work with chiseltest SRAMs" in {
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

      // TODO: Test on SRAMs with smaller than 32 data width to make sure we aren't checking too many bits.
      val testhelpers = new BistTopTestHelpers(c)
      c.clock.setTimeout(
        (testhelpers.maxCols + 1) * (testhelpers.maxRows + 1) * 4 * 4 * 3
      )
      val testSramMethod = (executeFn: () => Unit) => {
        // Test write.
        testhelpers.populateSramRegisters(
          0.U,
          "habcdabcd".U,
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
        testhelpers.c.io.dout.expect("habcdabcd".U)

        // Test write with mask.
        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          5.U,
          true.B,
          0.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        testhelpers.c.io.dout.expect(
          "habcdabcd".U
        ) // Dout should retain its value while registers are being set up.
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
        testhelpers.c.io.dout.expect("hab00ab00".U)

        // Test write to second SRAM.
        testhelpers.populateSramRegisters(
          0.U,
          "h12345678".U,
          "hf".U,
          true.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("h12345678".U)

        // Test that first SRAM retains original value.
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
        testhelpers.c.io.dout.expect("hab00ab00".U)

        // Test writing to extreme addresses of both SRAMs. Verify that original data doesn't change.
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
        testhelpers.c.io.dout.expect("h87654321".U)

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
        testhelpers.c.io.dout.expect("hab00ab00".U)

        testhelpers.populateSramRegisters(
          1023.U,
          "hdeadbeef".U,
          "hf".U,
          true.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()

        testhelpers.populateSramRegisters(
          1023.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("hdeadbeef".U)

        testhelpers.populateSramRegisters(
          0.U,
          0.U,
          0.U,
          false.B,
          1.U,
          SramSrc.mmio,
          SaeSrc.int
        )
        executeFn()
        testhelpers.c.io.dout.expect("h12345678".U)
      }

      val testBistMethod = (scanChain: Boolean) => {

        val maybeReset = () => {
          if (scanChain) {
            c.reset.poke(true.B)
            c.clock.step()
            c.reset.poke(false.B)
            c.clock.step()
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
        // TODO: Use correct cycle limit.
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
            c.reset.poke(true.B)
            c.clock.step()
            c.reset.poke(false.B)
            c.clock.step()
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
        // TODO: Use correct cycle limit.
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
            c.reset.poke(true.B)
            c.clock.step()
            c.reset.poke(false.B)
            c.clock.step()
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
        // TODO: Use correct cycle limit.
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
}
