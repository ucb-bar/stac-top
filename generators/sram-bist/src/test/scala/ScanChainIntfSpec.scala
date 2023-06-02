package srambist

import chisel3._
import chiseltest._
import chisel3.stage.PrintFullStackTraceAnnotation

import org.scalatest.flatspec.AnyFlatSpec

import srambist.SramBistCtrlRegs._

class ScanChainIntfSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ScanChainIntf"
  it should "work" in {
    test(new ScanChainIntf)
      .withAnnotations(Seq(WriteVcdAnnotation, PrintFullStackTraceAnnotation)) {
        d =>
          d.io.sramScanMode.poke(false.B)
          d.io.sramScanIn.poke(false.B)
          d.io.sramScanEn.poke(false.B)

          // Test writing to writable MMIO register.
          d.io.addr.d.poke(15.U)
          d.io.addr.en.poke(true.B)
          d.clock.step()
          d.io.addr.q.expect(15.U)
          d.io.addr.d.poke(20.U)
          d.clock.step()
          d.io.addr.q.expect(20.U)
          d.io.addr.d.poke(21.U)
          d.io.addr.en.poke(false.B)
          d.clock.step()
          d.io.addr.q.expect(20.U)

          // Test updating output registers from control logic and reading over MMIO interface.
          d.io.dout.poke(60.U)
          d.clock.step()
          d.io.doutMmio.q.expect(60.U)

          // Test writing to read-only MMIO register.
          d.io.doutMmio.d.poke(50.U)
          d.io.doutMmio.en.poke(true.B)
          d.clock.step()
          d.io.doutMmio.q.expect(60.U)
          d.io.doutMmio.en.poke(false.B)

          // Test writing via MMIO while in scan chain mode.
          d.io.sramScanMode.poke(true.B)
          d.io.addr.d.poke(21.U)
          d.io.addr.en.poke(true.B)
          d.clock.step()
          d.io.addr.q.expect(20.U)
          d.io.addr.en.poke(false.B)

          // Test reading output registers via scan out.
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

          // Test updating registers via scan in.

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
          d.io.addr.q.expect(25.U)
          d.io.din.q.expect(20.U)
          d.io.mask.q.expect(15.U)
          d.io.we.q.expect(0.U)

          d.clock.step()
          d.io.addr.q.expect(25.U)
          d.io.din.q.expect(20.U)
          d.io.mask.q.expect(15.U)
          d.io.we.q.expect(0.U)
      }
  }
}
