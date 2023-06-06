package srambist

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters

import srambist.analog.{Tdc, DelayLine, Sram, SramParams}
import srambist.sramharness.{SramHarness, SramHarnessParams, SaeSrc}
import srambist.programmablebist.{ProgrammableBist, ProgrammableBistParams}
import srambist.misr.MaxPeriodFibonacciMISR
import chisel3.experimental.VecLiterals._
import srambist.analog.{Tdc, DelayLine, BufferTree}

case class BistTopParams(
    srams: Seq[SramParams] = Seq(
      new SramParams(8, 8, 2048, 32),
      new SramParams(8, 4, 256, 32),
      new SramParams(8, 4, 64, 32),
      new SramParams(24, 4, 64, 24),
      new SramParams(8, 8, 1024, 32),
      new SramParams(32, 8, 1024, 32),
      new SramParams(32, 4, 512, 32),
      new SramParams(8, 4, 512, 32),
    ),
    bistParams: ProgrammableBistParams = new ProgrammableBistParams()
)

object SramSrc extends ChiselEnum {
  val mmio = Value(0.U(1.W))
  val bist = Value(1.U(1.W))
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
    val bistStart = Input(Bool())

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
  val fsmBistStart = Wire(Bool())

  val bistShouldStop = Wire(Bool())
  val bistSramEnPrev = RegNext(bist.io.sramEn)
  val bistSramWenPrev = RegNext(bist.io.sramWen)
  val bistDataPrev = RegNext(bist.io.data)
  val bistEnPrev = RegNext(bist.io.en)
  val bistCheckEnPrev = RegNext(bist.io.checkEn)
  val bistCyclePrev = RegNext(bist.io.cycle)
  val bistDonePrev = RegNext(bist.io.done)
  val bistFail = RegInit(false.B)
  val bistFailCycle = Reg(UInt(32.W))
  val bistExpected = Reg(UInt(32.W))
  val bistReceived = Reg(UInt(32.W))
  val sramMasks = RegInit(
    Vec(params.srams.length, UInt(32.W)).Lit(params.srams.zipWithIndex.map {
      case (p, i) => i -> ((1L << p.dataWidth) - 1).U(32.W)
    }: _*)
  )
  val sramMask = Wire(UInt(32.W))
  val maskedIOOut = Wire(chiselTypeOf(io.dout))

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
  fsmBistStart := false.B
  switch(state) {
    is(State.idle) {
      when(io.ex) {
        bistFail := false.B
        switch(io.sramSel) {
          is(SramSrc.bist) {
            fsmBistStart := true.B
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
      fsmBistStart := true.B
      state := State.executeOp
    }
    is(State.executeOp) {
      switch(io.sramSel) {
        is(SramSrc.mmio) {
          fsmSramEn := true.B
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

  bistShouldStop := bistFail & io.bistStopOnFailure

  sramMask := sramMasks(io.sramId)
  maskedIOOut := io.dout & sramMask
  when(
    bistEnPrev & bistCheckEnPrev & ((bistDataPrev & sramMask) =/= maskedIOOut)
  ) {
    when(~bistShouldStop) {
      bistFail := true.B
      bistFailCycle := bistCyclePrev
      bistExpected := bistDataPrev
      bistReceived := io.dout
    }
    when(io.bistStopOnFailure & ~io.sramScanMode) {
      state := State.idle
    }
  }

  when(bist.io.start) {
    bistFail := false.B
  }

  bist.io.en := Mux(io.sramScanMode, io.bistEn & ~bistShouldStop, fsmBistEn)
  bist.io.start := Mux(io.sramScanMode, io.bistStart, fsmBistStart)
  bist.io.maxRowAddr := io.bistMaxRowAddr
  bist.io.maxColAddr := io.bistMaxColAddr
  bist.io.innerDim := io.bistInnerDim
  bist.io.seed := io.bistRandSeed
  bist.io.patternTable := io.bistPatternTable
  bist.io.elementSequence := io.bistElementSequence
  bist.io.maxElementIdx := io.bistMaxElementIdx
  bist.io.cycleLimit := io.bistCycleLimit

  io.bistDone := bistDonePrev | bistShouldStop

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

  misr.io.in := maskedIOOut.asBools

  val (delay_lines, tdcs, b_buffers) = (0 until (((srams.length - 1) >> 2) + 1)).map { i =>
    val delay_line = Module(new DelayLine)
    delay_line.io.clk_in := clock.asBool
    val saeCtlOH = UIntToOH(io.saeCtl)
    delay_line.io.ctl := saeCtlOH
    delay_line.io.ctl_b := ~saeCtlOH
    val tdc = Module(new Tdc)

    val aBuffered = Wire(chiselTypeOf(tdc.io.a))
    val bBuffered = Wire(chiselTypeOf(tdc.io.b))

    val bufA = Module(new BufferTree)
    val bufB = Module(new BufferTree)
    bufA.io.A := clock.asBool
    aBuffered := bufA.io.X
    bufB.io.A := false.B
    bBuffered := bufB.io.X

    tdc.io.a := aBuffered
    tdc.io.b := bBuffered
    tdc.io.reset_b := ~reset.asBool

    (delay_line, tdc, bufB)
  }.unzip3

  harnesses.zipWithIndex.foreach { case (harness, i) =>
    harness.io.delayLineIn := delay_lines(i >> 2).io.clk_out
    when (i.U === io.sramId) {
      b_buffers(i >> 2).io.A := harness.io.saeMuxed
    }
  }

  io.tdc := MuxCase(
    0.U,
    tdcs.zipWithIndex.map { case (tdc, i) =>
      (i.U === io.sramId >> 2) -> tdc.io.dout
    }
  )

  io.bistSignature := misr.io.out.asUInt

}
