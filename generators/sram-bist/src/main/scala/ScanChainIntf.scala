package srambist

import chisel3._
import chisel3.util._
import freechips.rocketchip.util.SimpleRegIO

import srambist.scanchain.ScanChain

class ScanChainIntfIO extends Bundle {
  val sramScanMode = Input(Bool())
  val sramScanIn = Input(Bool())
  val sramScanEn = Input(Bool())

  val sramScanOut = Output(Bool())

  val addr = new SimpleRegIO(SramBistCtrlRegWidths.ADDR)
  val din = new SimpleRegIO(SramBistCtrlRegWidths.DIN)
  val mask = new SimpleRegIO(SramBistCtrlRegWidths.MASK)
  val we = new SimpleRegIO(SramBistCtrlRegWidths.WE)
  val sramId = new SimpleRegIO(SramBistCtrlRegWidths.SRAM_ID)
  val sramSel = new SimpleRegIO(SramBistCtrlRegWidths.SRAM_SEL)
  val saeCtl = new SimpleRegIO(SramBistCtrlRegWidths.SAE_CTL)
  val saeSel = new SimpleRegIO(SramBistCtrlRegWidths.SAE_SEL)
  val bistRandSeed = new SimpleRegIO(SramBistCtrlRegWidths.BIST_RAND_SEED)
  val bistSigSeed = new SimpleRegIO(SramBistCtrlRegWidths.BIST_SIG_SEED)
  val bistMaxRowAddr = new SimpleRegIO(SramBistCtrlRegWidths.BIST_MAX_ROW_ADDR)
  val bistMaxColAddr = new SimpleRegIO(SramBistCtrlRegWidths.BIST_MAX_COL_ADDR)
  val bistInnerDim = new SimpleRegIO(SramBistCtrlRegWidths.BIST_INNER_DIM)
  val bistPatternTable = new SimpleRegIO(
    SramBistCtrlRegWidths.BIST_PATTERN_TABLE
  )
  val bistElementSequence =
    new SimpleRegIO(SramBistCtrlRegWidths.BIST_ELEMENT_SEQUENCE)
  val bistMaxElementIdx =
    new SimpleRegIO(SramBistCtrlRegWidths.BIST_MAX_ELEMENT_IDX)
  val bistCycleLimit = new SimpleRegIO(SramBistCtrlRegWidths.BIST_CYCLE_LIMIT)

  val doutMmio = new SimpleRegIO(SramBistCtrlRegWidths.DOUT)
  val tdcMmio = new SimpleRegIO(SramBistCtrlRegWidths.TDC)
  val doneMmio = new SimpleRegIO(SramBistCtrlRegWidths.DONE)

  val bistFailMmio = new SimpleRegIO(SramBistCtrlRegWidths.BIST_FAIL)
  val bistFailCycleMmio = new SimpleRegIO(SramBistCtrlRegWidths.BIST_FAIL_CYCLE)
  val bistExpectedMmio = new SimpleRegIO(SramBistCtrlRegWidths.BIST_EXPECTED)
  val bistReceivedMmio = new SimpleRegIO(SramBistCtrlRegWidths.BIST_RECEIVED)
  val bistSignatureMmio = new SimpleRegIO(SramBistCtrlRegWidths.BIST_SIGNATURE)

  val dout = Input(UInt(SramBistCtrlRegWidths.DOUT.W))
  val tdc = Input(UInt(SramBistCtrlRegWidths.TDC.W))
  val done = Input(UInt(SramBistCtrlRegWidths.DONE.W))

  val bistFail = Input(UInt(SramBistCtrlRegWidths.BIST_FAIL.W))
  val bistFailCycle = Input(UInt(SramBistCtrlRegWidths.BIST_FAIL_CYCLE.W))
  val bistExpected = Input(UInt(SramBistCtrlRegWidths.BIST_EXPECTED.W))
  val bistReceived = Input(UInt(SramBistCtrlRegWidths.BIST_RECEIVED.W))
  val bistSignature = Input(UInt(SramBistCtrlRegWidths.BIST_SIGNATURE.W))
}

class ScanChainIntf extends Module {
  val io = new ScanChainIntfIO

  val scanChain = new ScanChain(SramBistCtrlRegWidths.TOTAL)

  scanChain.io.si := io.sramScanIn
  scanChain.io.se := io.sramScanEn
  io.sramScanOut := scanChain.io.so

  Seq(
    (SramBistCtrlRegs.ADDR, SramBistCtrlRegWidths.ADDR, io.addr, true),
    (SramBistCtrlRegs.DIN, SramBistCtrlRegWidths.DIN, io.din, true),
    (SramBistCtrlRegs.MASK, SramBistCtrlRegWidths.MASK, io.mask, true),
    (SramBistCtrlRegs.WE, SramBistCtrlRegWidths.WE, io.we, true),
    (SramBistCtrlRegs.SRAM_ID, SramBistCtrlRegWidths.SRAM_ID, io.sramId, true),
    (
      SramBistCtrlRegs.SRAM_SEL,
      SramBistCtrlRegWidths.SRAM_SEL,
      io.sramSel,
      true
    ),
    (SramBistCtrlRegs.SAE_CTL, SramBistCtrlRegWidths.SAE_CTL, io.saeCtl, true),
    (SramBistCtrlRegs.SAE_SEL, SramBistCtrlRegWidths.SAE_SEL, io.saeSel, true),
    (
      SramBistCtrlRegs.BIST_RAND_SEED,
      SramBistCtrlRegWidths.BIST_RAND_SEED,
      io.bistRandSeed,
      true
    ),
    (
      SramBistCtrlRegs.BIST_SIG_SEED,
      SramBistCtrlRegWidths.BIST_SIG_SEED,
      io.bistSigSeed,
      true
    ),
    (
      SramBistCtrlRegs.BIST_MAX_ROW_ADDR,
      SramBistCtrlRegWidths.BIST_MAX_ROW_ADDR,
      io.bistMaxRowAddr,
      true
    ),
    (
      SramBistCtrlRegs.BIST_MAX_COL_ADDR,
      SramBistCtrlRegWidths.BIST_MAX_COL_ADDR,
      io.bistMaxColAddr,
      true
    ),
    (
      SramBistCtrlRegs.BIST_INNER_DIM,
      SramBistCtrlRegWidths.BIST_INNER_DIM,
      io.bistInnerDim,
      true
    ),
    (
      SramBistCtrlRegs.BIST_PATTERN_TABLE,
      SramBistCtrlRegWidths.BIST_PATTERN_TABLE,
      io.bistPatternTable,
      true
    ),
    (
      SramBistCtrlRegs.BIST_ELEMENT_SEQUENCE,
      SramBistCtrlRegWidths.BIST_ELEMENT_SEQUENCE,
      io.bistElementSequence,
      true
    ),
    (
      SramBistCtrlRegs.BIST_MAX_ELEMENT_IDX,
      SramBistCtrlRegWidths.BIST_MAX_ELEMENT_IDX,
      io.bistMaxElementIdx,
      true
    ),
    (
      SramBistCtrlRegs.BIST_CYCLE_LIMIT,
      SramBistCtrlRegWidths.BIST_CYCLE_LIMIT,
      io.bistCycleLimit,
      true
    ),
    (SramBistCtrlRegs.DOUT, SramBistCtrlRegWidths.DOUT, io.doutMmio, false),
    (SramBistCtrlRegs.TDC, SramBistCtrlRegWidths.TDC, io.tdcMmio, false),
    (SramBistCtrlRegs.DONE, SramBistCtrlRegWidths.DONE, io.doneMmio, false),
    (
      SramBistCtrlRegs.BIST_FAIL,
      SramBistCtrlRegWidths.BIST_FAIL,
      io.bistFailMmio,
      false
    ),
    (
      SramBistCtrlRegs.BIST_FAIL_CYCLE,
      SramBistCtrlRegWidths.BIST_FAIL_CYCLE,
      io.bistFailCycleMmio,
      false
    ),
    (
      SramBistCtrlRegs.BIST_EXPECTED,
      SramBistCtrlRegWidths.BIST_EXPECTED,
      io.bistExpectedMmio,
      false
    ),
    (
      SramBistCtrlRegs.BIST_RECEIVED,
      SramBistCtrlRegWidths.BIST_RECEIVED,
      io.bistReceivedMmio,
      false
    ),
    (
      SramBistCtrlRegs.BIST_SIGNATURE,
      SramBistCtrlRegWidths.BIST_SIGNATURE,
      io.bistSignatureMmio,
      false
    )
  ).foreach((args: (Int, Int, SimpleRegIO, Boolean)) => {
    args match { case (start, width, regIO, writable) =>
    for (i <- 0 to width - 1) {
      if (writable) {
        scanChain.io.de(start + i) := ~io.sramScanMode & regIO.en
        scanChain.io.d(start + i) := regIO.d(i)
      }
      regIO.q(i) := scanChain.io.q(start + i)
    }
    }
  })

  Seq(
    (SramBistCtrlRegs.DOUT, SramBistCtrlRegWidths.DOUT, io.dout),
    (SramBistCtrlRegs.TDC, SramBistCtrlRegWidths.TDC, io.tdc),
    (SramBistCtrlRegs.DONE, SramBistCtrlRegWidths.DONE, io.done),
    (SramBistCtrlRegs.BIST_FAIL, SramBistCtrlRegWidths.BIST_FAIL, io.bistFail),
    (
      SramBistCtrlRegs.BIST_FAIL_CYCLE,
      SramBistCtrlRegWidths.BIST_FAIL_CYCLE,
      io.bistFailCycle
    ),
    (
      SramBistCtrlRegs.BIST_EXPECTED,
      SramBistCtrlRegWidths.BIST_EXPECTED,
      io.bistExpected
    ),
    (
      SramBistCtrlRegs.BIST_RECEIVED,
      SramBistCtrlRegWidths.BIST_RECEIVED,
      io.bistReceived
    ),
    (
      SramBistCtrlRegs.BIST_SIGNATURE,
      SramBistCtrlRegWidths.BIST_SIGNATURE,
      io.bistSignature
    )
  ).foreach((args: (Int, Int, UInt)) => {
    args match { case (start, width, in) =>
    for (i <- 0 to width - 1) {
      scanChain.io.de(start + i) := true.B
      scanChain.io.d(start + i) := in(i)
    }
    }
  })
}
