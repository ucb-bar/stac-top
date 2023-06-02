package srambist

import chisel3._
import chiseltest._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

import srambist.SramBistCtrlRegs._

class ScanChainIntfSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ScanChainIntf"
  it should "allow reads from MMIO registers set by control logic" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(false.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          d.io.dout.poke(60.U)
          d.io.tdc.poke(
            "h123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".U
          )
          d.clock.step()
          d.io.mmio.doutMmio.q.expect(60.U)
          d.io.mmio.tdcMmio(0).q.expect("h0123456789abcdef".U)
          d.io.mmio.tdcMmio(1).q.expect("h0123456789abcdef".U)
          d.io.mmio.tdcMmio(2).q.expect("h0123456789abcdef".U)
          d.io.mmio.tdcMmio(3).q.expect("h123456789abcdef".U)
      }
  }
  it should "ignore writes to read-only MMIO registers" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(false.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          d.io.mmio.doutMmio.d.poke(50.U)
          d.io.mmio.doutMmio.en.poke(true.B)
          d.clock.step()
          d.io.mmio.doutMmio.q.expect(60.U)
          d.io.mmio.doutMmio.en.poke(false.B)
      }
  }
  it should "ignore writes via MMIO while in scan chain mode" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(true.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          d.io.mmio.addr.d.poke(21.U)
          d.io.mmio.addr.en.poke(true.B)
          d.clock.step()
          d.io.mmio.addr.q.expect(20.U)
          d.io.mmio.addr.en.poke(false.B)
      }
  }
  it should "allow for scan out of control logic output" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(true.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          d.io.bistFail.poke(true.B)
          d.io.bistFailCycle.poke(50.U)
          d.io.bistExpected.poke(60.U)
          d.io.bistReceived.poke(70.U)
          d.io.bistSignature.poke(0.U)
          d.clock.step()

          d.io.sramScanIn.poke(true.B)
          d.io.sramScanEn.poke(true.B)

          val scanOutAndAssert = (width: Int, value: Int) => {
            var num = 0
            for (i <- 1 to width) {
              var bit = d.io.sramScanOut.peek().litToBoolean
              var digit = if (bit) 1 else 0
              num = num * 2 + digit
              d.clock.step()
            }
            assert(num == value)
          }

          scanOutAndAssert(REG_WIDTH(BIST_SIGNATURE), 0)
          scanOutAndAssert(REG_WIDTH(BIST_RECEIVED), 70)
          scanOutAndAssert(REG_WIDTH(BIST_EXPECTED), 60)
          scanOutAndAssert(REG_WIDTH(BIST_FAIL_CYCLE), 50)
          scanOutAndAssert(REG_WIDTH(BIST_FAIL), 1)
      }
  }
  it should "allow for scan in of input registers" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(true.B)
          d.io.sramScanIn.poke(true.B)
          d.io.sramScanEn.poke(true.B)

          val scanIn = (width: Int, value: Int) => {
            var bitSeq = Seq[Int]()
            var v = value
            for (i <- 1 to width) {
              bitSeq = (v % 2) +: bitSeq
              v /= 2
            }
            for (bit <- bitSeq) {
              d.io.sramScanIn.poke((bit == 1).B)
              d.clock.step()
            }
          }

          scanIn(REG_WIDTH(WE), 0)
          scanIn(REG_WIDTH(MASK), 15)
          scanIn(REG_WIDTH(DIN), 20)
          scanIn(REG_WIDTH(ADDR), 25)

          d.io.sramScanEn.poke(false.B)
          d.io.mmio.addr.q.expect(25.U)
          d.io.mmio.din.q.expect(20.U)
          d.io.mmio.mask.q.expect(15.U)
          d.io.mmio.we.q.expect(0.U)

          d.clock.step()
          d.io.mmio.addr.q.expect(25.U)
          d.io.mmio.din.q.expect(20.U)
          d.io.mmio.mask.q.expect(15.U)
          d.io.mmio.we.q.expect(0.U)
      }
  }
  it should "allow writes to MMIO registers" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(false.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          // Test writing to writable MMIO register.
          d.io.mmio.addr.d.poke(15.U)
          d.io.mmio.addr.en.poke(true.B)
          d.clock.step()
          d.io.mmio.addr.q.expect(15.U)
          d.io.mmio.addr.d.poke(20.U)
          d.clock.step()
          d.io.mmio.addr.q.expect(20.U)
          d.io.mmio.addr.d.poke(21.U)
          d.io.mmio.addr.en.poke(false.B)
          d.clock.step()
          d.io.mmio.addr.q.expect(20.U)
      }
  }
}
