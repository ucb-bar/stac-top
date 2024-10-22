package srambist

import chisel3._
import chisel3.util._
import freechips.rocketchip.util.SimpleRegIO

import srambist.scanchain.ScanChain
import srambist.SramBistCtrlRegs._

class MmioRegIO extends Bundle {
  val addr = new SimpleRegIO(REG_WIDTH(ADDR))
  val din = new SimpleRegIO(REG_WIDTH(DIN))
  val mask = new SimpleRegIO(REG_WIDTH(MASK))
  val we = new SimpleRegIO(REG_WIDTH(WE))
  val sramId = new SimpleRegIO(REG_WIDTH(SRAM_ID))
  val sramSel = new SimpleRegIO(REG_WIDTH(SRAM_SEL))
  val dlCtl = new SimpleRegIO(REG_WIDTH(DL_CTL))
  val tdcSel = new SimpleRegIO(REG_WIDTH(TDC_SEL))
  val bistRandSeedMmio = Vec(2, new SimpleRegIO(64))
  val bistSigSeed = new SimpleRegIO(REG_WIDTH(BIST_SIG_SEED))
  val bistMaxRowAddr = new SimpleRegIO(REG_WIDTH(BIST_MAX_ROW_ADDR))
  val bistMaxColAddr = new SimpleRegIO(REG_WIDTH(BIST_MAX_COL_ADDR))
  val bistInnerDim = new SimpleRegIO(REG_WIDTH(BIST_INNER_DIM))
  val bistPatternTableMmio = Vec(
    4,
    new SimpleRegIO(
      64
    )
  )
  val bistElementSequenceMmio =
    Vec(16, new SimpleRegIO(64))
  val bistMaxElementIdx =
    new SimpleRegIO(REG_WIDTH(BIST_MAX_ELEMENT_IDX))
  val bistCycleLimit = new SimpleRegIO(REG_WIDTH(BIST_CYCLE_LIMIT))
  val bistStopOnFailure = new SimpleRegIO(
    REG_WIDTH(BIST_STOP_ON_FAILURE)
  )

  val doutMmio = new SimpleRegIO(REG_WIDTH(DOUT))
  val tdcMmio = Vec(4, new SimpleRegIO(64))
  val doneMmio = new SimpleRegIO(REG_WIDTH(DONE))

  val bistFailMmio = new SimpleRegIO(REG_WIDTH(BIST_FAIL))
  val bistFailCycleMmio = new SimpleRegIO(REG_WIDTH(BIST_FAIL_CYCLE))
  val bistExpectedMmio = new SimpleRegIO(REG_WIDTH(BIST_EXPECTED))
  val bistReceivedMmio = new SimpleRegIO(REG_WIDTH(BIST_RECEIVED))
  val bistSignatureMmio = new SimpleRegIO(REG_WIDTH(BIST_SIGNATURE))
}

class ScanChainIntfIO extends Bundle {
  val sramScanMode = Input(Bool())
  val sramScanIn = Input(Bool())
  val sramScanEn = Input(Bool())

  val sramScanOut = Output(Bool())

  val dout = Input(UInt(REG_WIDTH(DOUT).W))
  val tdc = Input(UInt(REG_WIDTH(TDC).W))
  val done = Input(UInt(REG_WIDTH(DONE).W))

  val bistFail = Input(UInt(REG_WIDTH(BIST_FAIL).W))
  val bistFailCycle = Input(UInt(REG_WIDTH(BIST_FAIL_CYCLE).W))
  val bistExpected = Input(UInt(REG_WIDTH(BIST_EXPECTED).W))
  val bistReceived = Input(UInt(REG_WIDTH(BIST_RECEIVED).W))
  val bistSignature = Input(UInt(REG_WIDTH(BIST_SIGNATURE).W))

  val bistRandSeed = Output(UInt(REG_WIDTH(BIST_RAND_SEED).W))
  val bistPatternTable = Output(UInt(REG_WIDTH(BIST_PATTERN_TABLE).W))
  val bistElementSequence = Output(UInt(REG_WIDTH(BIST_ELEMENT_SEQUENCE).W))

  val mmio = new MmioRegIO
}

class ScanChainIntf extends Module {
  val io = IO(new ScanChainIntfIO)

  val scanChain = Module(new ScanChain(TOTAL_REG_WIDTH))

  scanChain.io.si := io.sramScanIn
  scanChain.io.se := io.sramScanEn
  io.sramScanOut := scanChain.io.so

  Seq(
    (
      SCAN_CHAIN_OFFSET(BIST_RAND_SEED),
      REG_WIDTH(BIST_RAND_SEED),
      io.mmio.bistRandSeedMmio,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_PATTERN_TABLE),
      REG_WIDTH(BIST_PATTERN_TABLE),
      io.mmio.bistPatternTableMmio,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_ELEMENT_SEQUENCE),
      REG_WIDTH(BIST_ELEMENT_SEQUENCE),
      io.mmio.bistElementSequenceMmio,
      true
    ),
    (SCAN_CHAIN_OFFSET(TDC), REG_WIDTH(TDC), io.mmio.tdcMmio, false)
  ).foreach((args: (Int, Int, Vec[SimpleRegIO], Boolean)) => {
    args match {
      case (start, width, regIO, writable) =>
        val vecWidth = ((width - 1) / 64 + 1)
        val q = Wire(Vec(vecWidth, Vec(64, Bool())))
        for (i <- 0 until width) {
          if (writable) {
            scanChain.io.de(start + i) := ~io.sramScanMode & regIO(i / 64).en
            scanChain.io.d(start + i) := regIO(i / 64).d(i % 64)
          }
          q(i / 64)(i % 64) := scanChain.io.q(start + i)
        }
        for (i <- width until vecWidth * 64) {
          q(i / 64)(i % 64) := DontCare
        }

        for (i <- 0 until vecWidth) {
          regIO(i).q := Reverse(Cat(q(i)))
        }
    }
  })

  Seq(
    (
      SCAN_CHAIN_OFFSET(BIST_RAND_SEED),
      REG_WIDTH(BIST_RAND_SEED),
      io.bistRandSeed
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_PATTERN_TABLE),
      REG_WIDTH(BIST_PATTERN_TABLE),
      io.bistPatternTable
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_ELEMENT_SEQUENCE),
      REG_WIDTH(BIST_ELEMENT_SEQUENCE),
      io.bistElementSequence
    )
  ).foreach((args: (Int, Int, UInt)) => {
    args match {
      case (start, width, out) =>
        val q = Wire(Vec(width, Bool()))
        for (i <- 0 until width) {
          q(i) := scanChain.io.q(start + i)
        }
        out := Reverse(Cat(q))
    }
  })

  Seq(
    (SCAN_CHAIN_OFFSET(ADDR), REG_WIDTH(ADDR), io.mmio.addr, true),
    (SCAN_CHAIN_OFFSET(DIN), REG_WIDTH(DIN), io.mmio.din, true),
    (SCAN_CHAIN_OFFSET(MASK), REG_WIDTH(MASK), io.mmio.mask, true),
    (SCAN_CHAIN_OFFSET(WE), REG_WIDTH(WE), io.mmio.we, true),
    (SCAN_CHAIN_OFFSET(SRAM_ID), REG_WIDTH(SRAM_ID), io.mmio.sramId, true),
    (
      SCAN_CHAIN_OFFSET(SRAM_SEL),
      REG_WIDTH(SRAM_SEL),
      io.mmio.sramSel,
      true
    ),
    (SCAN_CHAIN_OFFSET(DL_CTL), REG_WIDTH(DL_CTL), io.mmio.dlCtl, true),
    (SCAN_CHAIN_OFFSET(TDC_SEL), REG_WIDTH(TDC_SEL), io.mmio.tdcSel, true),
    (
      SCAN_CHAIN_OFFSET(BIST_SIG_SEED),
      REG_WIDTH(BIST_SIG_SEED),
      io.mmio.bistSigSeed,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_MAX_ROW_ADDR),
      REG_WIDTH(BIST_MAX_ROW_ADDR),
      io.mmio.bistMaxRowAddr,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_MAX_COL_ADDR),
      REG_WIDTH(BIST_MAX_COL_ADDR),
      io.mmio.bistMaxColAddr,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_INNER_DIM),
      REG_WIDTH(BIST_INNER_DIM),
      io.mmio.bistInnerDim,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_MAX_ELEMENT_IDX),
      REG_WIDTH(BIST_MAX_ELEMENT_IDX),
      io.mmio.bistMaxElementIdx,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_CYCLE_LIMIT),
      REG_WIDTH(BIST_CYCLE_LIMIT),
      io.mmio.bistCycleLimit,
      true
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_STOP_ON_FAILURE),
      REG_WIDTH(BIST_STOP_ON_FAILURE),
      io.mmio.bistStopOnFailure,
      true
    ),
    (SCAN_CHAIN_OFFSET(DOUT), REG_WIDTH(DOUT), io.mmio.doutMmio, false),
    (SCAN_CHAIN_OFFSET(DONE), REG_WIDTH(DONE), io.mmio.doneMmio, false),
    (
      SCAN_CHAIN_OFFSET(BIST_FAIL),
      REG_WIDTH(BIST_FAIL),
      io.mmio.bistFailMmio,
      false
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_FAIL_CYCLE),
      REG_WIDTH(BIST_FAIL_CYCLE),
      io.mmio.bistFailCycleMmio,
      false
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_EXPECTED),
      REG_WIDTH(BIST_EXPECTED),
      io.mmio.bistExpectedMmio,
      false
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_RECEIVED),
      REG_WIDTH(BIST_RECEIVED),
      io.mmio.bistReceivedMmio,
      false
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_SIGNATURE),
      REG_WIDTH(BIST_SIGNATURE),
      io.mmio.bistSignatureMmio,
      false
    )
  ).foreach((args: (Int, Int, SimpleRegIO, Boolean)) => {
    args match {
      case (start, width, regIO, writable) =>
        val q = Wire(Vec(width, Bool()))
        for (i <- 0 to width - 1) {
          if (writable) {
            scanChain.io.de(start + i) := ~io.sramScanMode & regIO.en
            scanChain.io.d(start + i) := regIO.d(i)
          }
          q(i) := scanChain.io.q(start + i)
        }
        regIO.q := Reverse(Cat(q))
    }
  })

  Seq(
    (SCAN_CHAIN_OFFSET(DOUT), REG_WIDTH(DOUT), io.dout),
    (SCAN_CHAIN_OFFSET(TDC), REG_WIDTH(TDC), io.tdc),
    (SCAN_CHAIN_OFFSET(DONE), REG_WIDTH(DONE), io.done),
    (SCAN_CHAIN_OFFSET(BIST_FAIL), REG_WIDTH(BIST_FAIL), io.bistFail),
    (
      SCAN_CHAIN_OFFSET(BIST_FAIL_CYCLE),
      REG_WIDTH(BIST_FAIL_CYCLE),
      io.bistFailCycle
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_EXPECTED),
      REG_WIDTH(BIST_EXPECTED),
      io.bistExpected
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_RECEIVED),
      REG_WIDTH(BIST_RECEIVED),
      io.bistReceived
    ),
    (
      SCAN_CHAIN_OFFSET(BIST_SIGNATURE),
      REG_WIDTH(BIST_SIGNATURE),
      io.bistSignature
    )
  ).foreach((args: (Int, Int, UInt)) => {
    args match {
      case (start, width, in) =>
        for (i <- 0 to width - 1) {
          scanChain.io.de(start + i) := true.B
          scanChain.io.d(start + i) := in(i)
        }
    }
  })
}
