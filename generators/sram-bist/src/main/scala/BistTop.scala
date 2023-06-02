package srambist

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

import srambist.analog.{Tdc, DelayLine, Sram, SramParams}
import srambist.sramharness.{SramHarness, SramHarnessParams, SaeSrc}
import srambist.programmablebist.{ProgrammableBist, ProgrammableBistParams}
import srambist.misr.MaxPeriodFibonacciMISR

case class BistTopParams(
    srams: Seq[SramParams] =
      Seq(new SramParams(8, 4, 64, 32), new SramParams(8, 8, 1024, 32)),
    bistParams: ProgrammableBistParams = new ProgrammableBistParams()
)

object SramSrc extends ChiselEnum {
  val bist, mmio = Value
}

class BistTop(params: BistTopParams)(implicit p: Parameters) extends Module {

  val bist = Module(
    new ProgrammableBist(
      params.bistParams
    )
  )
  val misr = Module(
    new MaxPeriodFibonacciMISR(
      32
    )
  )

  val io = IO(new Bundle {
    // Pins
    val sramExtEn = Input(Bool())
    val sramScanMode = Input(Bool())
    val sramEn = Input(Bool())
    val bistEn = Input(Bool())

    // MMIO registers
    val addr = Input(UInt(13.W))
    val din = Input(UInt(32.W))
    val mask = Input(UInt(32.W))
    val we = Input(Bool())
    val sramId = Input(UInt(4.W))
    val sramSel = Input(SramSrc())
    val saeCtl = Input(UInt(7.W))
    val saeSel = Input(SaeSrc())
    val saeClk = Input(Bool())
    val bistRandSeed = Input(UInt(params.bistParams.seedWidth.W))
    val bistSigSeed = Input(UInt(32.W))
    val bistMaxRowAddr = Input(UInt(params.bistParams.maxRowAddrWidth.W))
    val bistMaxColAddr = Input(UInt(params.bistParams.maxColAddrWidth.W))
    val bistInnerDim = Input(bist.Dimension())
    val bistPatternTable = Input(
      Vec(
        params.bistParams.patternTableLength,
        UInt(params.bistParams.dataWidth.W)
      )
    )
    val bistElementSequence =
      Input(Vec(params.bistParams.elementTableLength, new bist.Element()))
    val bistMaxElementIdx =
      Input(UInt(log2Ceil(params.bistParams.elementTableLength).W))
    val bistCycleLimit = Input(UInt(32.W))
    val bistStopOnFailure = Input(Bool())
    val ex = Input(Bool())

    val dout = Output(UInt(32.W))
    val tdc = Output(UInt(252.W))
    val done = Output(Bool())

    val bistDone = Output(Bool())
    val bistFail = Output(Bool())
    val bistFailCycle = Output(UInt(32.W))
    val bistExpected = Output(UInt(32.W))
    val bistReceived = Output(UInt(32.W))
    val bistSignature = Output(UInt(32.W))
  })

  object State extends ChiselEnum {
    val idle, setupBist, executeOp, delay = Value
  }

  val state = RegInit(State.idle)

  val fsmSramEn = Wire(Bool())
  val fsmBistEn = Wire(Bool())

  val bistSramEnPrev = RegNext(bist.io.sramEn)
  val bistSramWenPrev = RegNext(bist.io.sramWen)
  val bistDataPrev = RegNext(bist.io.data)
  val bistEnPrev = RegNext(bist.io.en)
  val bistCheckEnPrev = RegNext(bist.io.checkEn)
  val bistCyclePrev = RegNext(bist.io.cycle)
  val bistFail = RegInit(false.B)
  val bistFailCycle = Reg(UInt(32.W))
  val bistExpected = Reg(UInt(32.W))
  val bistReceived = Reg(UInt(32.W))

  io.bistFail := bistFail
  io.bistFailCycle := bistFailCycle
  io.bistExpected := bistExpected
  io.bistReceived := bistReceived

  misr.io.en := bistSramEnPrev & ~bistSramWenPrev
  misr.io.seed.valid := bist.io.resetHash
  misr.io.seed.bits := io.bistSigSeed.asBools

  io.done := false.B
  fsmSramEn := false.B
  fsmBistEn := false.B
  bist.io.start := false.B
  switch(state) {
    is(State.idle) {
      when(io.ex) {
        bistFail := false.B
        switch(io.sramSel) {
          is(SramSrc.bist) {
            bist.io.start := true.B
            state := State.setupBist
          }
          is(SramSrc.mmio) {
            state := State.executeOp
          }
        }
      }.otherwise {
        io.done := true.B
      }
    }
    is(State.setupBist) {
      bist.io.start := true.B
      state := State.executeOp
    }
    is(State.executeOp) {
      switch(io.sramSel) {
        is(SramSrc.mmio) {
          fsmSramEn := true.B // TODO: verify that this is OK for clock gating.
          state := State.delay
        }
        is(SramSrc.bist) {
          fsmBistEn := true.B
          when(bist.io.done) {
            state := State.idle
          }
        }
      }
    }
    is(State.delay) {
      state := State.idle
    }
  }

  // TODO: does not stop on first failure for scan chain -> BIST mode, does for MMIO.
  when(bistEnPrev & bistCheckEnPrev & bistDataPrev =/= io.dout) {
    when(~(bistFail & io.bistStopOnFailure)) {
      bistFail := true.B
      bistFailCycle := bistCyclePrev
      bistExpected := bistDataPrev
      bistReceived := io.dout
    }
    when(io.bistStopOnFailure & ~io.sramScanMode) {
      state := State.idle
    }
  }

  bist.io.en := Mux(io.sramScanMode, io.bistEn & ~bistFail, fsmBistEn)
  bist.io.maxRowAddr := io.bistMaxRowAddr
  bist.io.maxColAddr := io.bistMaxColAddr
  bist.io.innerDim := io.bistInnerDim
  bist.io.seed := io.bistRandSeed
  bist.io.patternTable := io.bistPatternTable
  bist.io.elementSequence := io.bistElementSequence
  bist.io.maxElementIdx := io.bistMaxElementIdx
  bist.io.cycleLimit := io.bistCycleLimit

  io.bistDone := bist.io.done | bistFail

  var (srams, harnesses) = params.srams.zipWithIndex.map {
    case (sramParams, i) => {
      val numRows = sramParams.numWords / sramParams.muxRatio
      val numCols = sramParams.dataWidth * sramParams.muxRatio
      val maskWidth = sramParams.dataWidth / sramParams.maskGranularity
      val harness = Module(
        new SramHarness(
          new SramHarnessParams(
            log2Ceil(numRows),
            log2Ceil(sramParams.muxRatio),
            sramParams.dataWidth,
            maskWidth
          )
        )
      )

      val sram = withClock(harness.io.sramClk) { Module(new Sram(sramParams)) }

      sram.io.wmask := harness.io.mask
      sram.io.addr := harness.io.addr
      sram.io.din := harness.io.data
      sram.io.saeMuxed := harness.io.saeMuxed

      harness.io.sramEn := false.B
      harness.io.inRow := 0.U
      harness.io.inCol := 0.U
      harness.io.inData := 0.U
      harness.io.inMask := 0.U
      sram.io.we := false.B
      switch(io.sramSel) {
        is(SramSrc.mmio) {
          harness.io.sramEn := i.U === io.sramId & Mux(
            io.sramExtEn,
            io.sramEn,
            fsmSramEn
          )
          harness.io.inRow := io.addr(
            log2Ceil(sramParams.numWords) - 1,
            log2Ceil(sramParams.muxRatio)
          )
          harness.io.inCol := io.addr(log2Ceil(sramParams.muxRatio) - 1, 0)
          harness.io.inData := io.din(sramParams.dataWidth - 1, 0)
          harness.io.inMask := io.mask(maskWidth - 1, 0)
          sram.io.we := io.we
        }
        is(SramSrc.bist) {
          harness.io.sramEn := i.U === io.sramId & bist.io.en & Mux(
            io.sramExtEn,
            io.sramEn,
            bist.io.sramEn
          )
          harness.io.inRow := bist.io.row
          harness.io.inCol := bist.io.col
          harness.io.inData := bist.io.data
          harness.io.inMask := bist.io.mask
          sram.io.we := bist.io.sramWen
        }
      }

      harness.io.saeInt := sram.io.saeInt
      harness.io.saeSel := io.saeSel
      harness.io.saeClk := io.saeClk
      harness.io.saeCtl := io.saeCtl
      (sram, harness)
    }
  }.unzip

  io.dout := MuxCase(
    0.U,
    srams.zipWithIndex.map { case (sram, i) =>
      (i.U === io.sramId) -> sram.io.dout
    }
  )

  misr.io.in := io.dout.asBools

  io.tdc := MuxCase(
    0.U,
    harnesses.zipWithIndex.map { case (harness, i) =>
      (i.U === io.sramId) -> harness.io.saeOut
    }
  )

  io.bistSignature := misr.io.out.asUInt

}
