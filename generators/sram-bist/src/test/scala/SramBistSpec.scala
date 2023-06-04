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
val elementSequence = Vec(4, new d.Element())
          .Lit(0 -> march, 1 -> march, 2 -> march, 3 -> march)
val zeros = 0.U(32.W)
val ones = "hffffffff".U(32.W)

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
      val h = SramBistTestHelpers(d)
      println(h.elementSequence.asBools)

      d.io.top.sramExtEn.poke(true.B)
      d.io.top.sramScanMode.poke(true.B)
      d.io.top.sramEn.poke(false.B)
      d.io.top.bistEn.poke(false.B)
      d.io.top.bistStart.poke(false.B)

      scanClear()
    }
  }
}
