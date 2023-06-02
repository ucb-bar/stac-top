package srambist

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.prci._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util._

import srambist.analog.SramParams
import srambist.sramharness.SaeSrc
import srambist.SramBistCtrlRegs._

class SramBistTopIO extends Bundle {
  val sramExtEn = Input(Bool())
  val sramScanMode = Input(Bool())
  val sramEn = Input(Bool())
  val sramScanIn = Input(Bool())
  val sramScanEn = Input(Bool())
  val sramSaeClk = Input(Bool())
  val bistEn = Input(Bool())
  val sramScanOut = Output(Bool())
  val bistDone = Output(Bool())
}

class SramBistIO extends Bundle {
  val top = new SramBistTopIO
  val ex = Flipped(Decoupled(Bool()))
  val mmio = new MmioRegIO
}

class SramBist()(implicit p: Parameters) extends Module {
  val io = IO(new SramBistIO)
    val scanChainIntf = Module(new ScanChainIntf)
    val bistTopParams = new BistTopParams
    val bistTop = Module(new BistTop(bistTopParams))

    val ready = RegNext(bistTop.io.done)

    scanChainIntf.io.sramScanMode := io.top.sramScanMode
    scanChainIntf.io.sramScanIn := io.top.sramScanIn
    scanChainIntf.io.sramScanEn := io.top.sramScanEn
    io.top.sramScanOut := scanChainIntf.io.sramScanOut

    bistTop.io.sramExtEn := io.top.sramExtEn
    bistTop.io.sramScanMode := io.top.sramScanMode
    bistTop.io.sramEn := io.top.sramEn
    bistTop.io.bistEn := io.top.bistEn
    bistTop.io.saeClk := io.top.sramSaeClk
    bistTop.io.ex := io.ex.valid & io.ex.bits
    io.ex.ready := ready // TODO: have someone verify that this is correct.

    bistTop.io.addr := scanChainIntf.io.mmio.addr.q
    bistTop.io.din := scanChainIntf.io.mmio.din.q
    bistTop.io.mask := scanChainIntf.io.mmio.mask.q
    bistTop.io.we := scanChainIntf.io.mmio.we.q.asBool
    bistTop.io.sramId := scanChainIntf.io.mmio.sramId.q
    bistTop.io.sramSel := scanChainIntf.io.mmio.sramSel.q.asTypeOf(SramSrc())
    bistTop.io.saeCtl := scanChainIntf.io.mmio.saeCtl.q
    bistTop.io.saeSel := scanChainIntf.io.mmio.saeSel.q.asTypeOf(SaeSrc())
    bistTop.io.bistRandSeed := scanChainIntf.io.bistRandSeed
    bistTop.io.bistSigSeed := scanChainIntf.io.mmio.bistSigSeed.q
    bistTop.io.bistMaxRowAddr := scanChainIntf.io.mmio.bistMaxRowAddr.q
    bistTop.io.bistMaxColAddr := scanChainIntf.io.mmio.bistMaxColAddr.q
    bistTop.io.bistInnerDim := scanChainIntf.io.mmio.bistInnerDim.q
      .asTypeOf(bistTop.bist.Dimension())
    bistTop.io.bistPatternTable := scanChainIntf.io.bistPatternTable.asTypeOf(
      Vec(
        bistTopParams.bistParams.patternTableLength,
        UInt(bistTopParams.bistParams.dataWidth.W)
      )
    )
    bistTop.io.bistElementSequence := scanChainIntf.io.bistElementSequence
      .asTypeOf(
        Vec(
          bistTopParams.bistParams.elementTableLength,
          new bistTop.bist.Element()
        )
      )
    bistTop.io.bistMaxElementIdx := scanChainIntf.io.mmio.bistMaxElementIdx.q
    bistTop.io.bistCycleLimit := scanChainIntf.io.mmio.bistCycleLimit.q
    bistTop.io.bistStopOnFailure := scanChainIntf.io.mmio.bistStopOnFailure.q.asBool

    scanChainIntf.io.dout := bistTop.io.dout
    scanChainIntf.io.tdc := bistTop.io.tdc
    scanChainIntf.io.done := bistTop.io.done.asUInt

    scanChainIntf.io.bistFail := bistTop.io.bistFail.asUInt
    scanChainIntf.io.bistFailCycle := bistTop.io.bistFailCycle
    scanChainIntf.io.bistExpected := bistTop.io.bistExpected
    scanChainIntf.io.bistReceived := bistTop.io.bistReceived
    scanChainIntf.io.bistSignature := bistTop.io.bistSignature

    io.top.bistDone := bistTop.io.bistDone
    io.mmio <> scanChainIntf.io.mmio
}

abstract class SramBistRouter(busWidthBytes: Int, params: SramBistParams)(implicit
    p: Parameters
) extends IORegisterRouter(
      RegisterRouterParams(
        name = "SramBist",
        compat = Seq(),
        base = params.address,
        beatBytes = busWidthBytes
      ),
      new SramBistTopIO
    ) {

  lazy val module = new LazyModuleImp(this) {
    val io = ioNode.bundle

    val sramBist = Module(new SramBist())

    io <> sramBist.io.top
    val ex = Wire(new DecoupledIO(UInt(params.width.W)))
    sramBist.io.ex.valid := ex.valid
    sramBist.io.ex.bits := ex.bits
    ex.ready := sramBist.io.ex.ready

    regmap(
      REGMAP_OFFSET(ADDR) -> Seq(
        RegField.rwReg(REG_WIDTH(ADDR), sramBist.io.mmio.addr)
      ),
      REGMAP_OFFSET(DIN) -> Seq(
        RegField.rwReg(REG_WIDTH(DIN), sramBist.io.mmio.din)
      ),
      REGMAP_OFFSET(MASK) -> Seq(
        RegField.rwReg(REG_WIDTH(MASK), sramBist.io.mmio.mask)
      ),
      REGMAP_OFFSET(WE) -> Seq(
        RegField.rwReg(REG_WIDTH(WE), sramBist.io.mmio.we)
      ),
      REGMAP_OFFSET(SRAM_ID) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_ID), sramBist.io.mmio.sramId)
      ),
      REGMAP_OFFSET(SRAM_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SEL), sramBist.io.mmio.sramSel)
      ),
      REGMAP_OFFSET(SAE_CTL) -> Seq(
        RegField.rwReg(REG_WIDTH(SAE_CTL), sramBist.io.mmio.saeCtl)
      ),
      REGMAP_OFFSET(SAE_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(SAE_SEL), sramBist.io.mmio.saeSel)
      ),
      REGMAP_OFFSET(DOUT) -> Seq(
        RegField.rwReg(REG_WIDTH(DOUT), sramBist.io.mmio.doutMmio)
      ),
      REGMAP_OFFSET(TDC) -> (0 until 4).map { i =>
        RegField.rwReg(64, sramBist.io.mmio.tdcMmio(i))
      },
      REGMAP_OFFSET(DONE) -> Seq(
        RegField.rwReg(REG_WIDTH(DONE), sramBist.io.mmio.doneMmio)
      ),
      REGMAP_OFFSET(BIST_RAND_SEED) -> (0 until 2).map { i =>
        RegField.rwReg(
          64,
          sramBist.io.mmio.bistRandSeedMmio(i)
        )
      },
      REGMAP_OFFSET(BIST_SIG_SEED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_SIG_SEED),
          sramBist.io.mmio.bistSigSeed
        )
      ),
      REGMAP_OFFSET(BIST_MAX_ROW_ADDR) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_ROW_ADDR),
          sramBist.io.mmio.bistMaxRowAddr
        )
      ),
      REGMAP_OFFSET(BIST_MAX_COL_ADDR) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_COL_ADDR),
          sramBist.io.mmio.bistMaxColAddr
        )
      ),
      REGMAP_OFFSET(BIST_INNER_DIM) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_INNER_DIM),
          sramBist.io.mmio.bistInnerDim
        )
      ),
      REGMAP_OFFSET(BIST_ELEMENT_SEQUENCE) -> (0 until 9).map { i =>
        RegField.rwReg(
          64,
          sramBist.io.mmio.bistElementSequenceMmio(i)
        )
      },
      REGMAP_OFFSET(BIST_PATTERN_TABLE) -> (0 until 4).map { i =>
        RegField.rwReg(
          64,
          sramBist.io.mmio.bistPatternTableMmio(i)
        )
      },
      REGMAP_OFFSET(BIST_MAX_ELEMENT_IDX) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_ELEMENT_IDX),
          sramBist.io.mmio.bistMaxElementIdx
        )
      ),
      REGMAP_OFFSET(BIST_CYCLE_LIMIT) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_CYCLE_LIMIT),
          sramBist.io.mmio.bistCycleLimit
        )
      ),
      REGMAP_OFFSET(BIST_STOP_ON_FAILURE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_STOP_ON_FAILURE),
          sramBist.io.mmio.bistStopOnFailure
        )
      ),
      REGMAP_OFFSET(BIST_FAIL) -> Seq(
        RegField
          .rwReg(REG_WIDTH(BIST_FAIL), sramBist.io.mmio.bistFailMmio)
      ),
      REGMAP_OFFSET(BIST_FAIL_CYCLE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_FAIL_CYCLE),
          sramBist.io.mmio.bistFailCycleMmio
        )
      ),
      REGMAP_OFFSET(BIST_EXPECTED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_EXPECTED),
          sramBist.io.mmio.bistExpectedMmio
        )
      ),
      REGMAP_OFFSET(BIST_RECEIVED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_RECEIVED),
          sramBist.io.mmio.bistReceivedMmio
        )
      ),
      REGMAP_OFFSET(BIST_SIGNATURE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_SIGNATURE),
          sramBist.io.mmio.bistSignatureMmio
        )
      )
      REGMAP_OFFSET(EX) -> Seq(
        RegField.w(
          1,
          ex
        )
      )
    )
  }
}

class TLSramBist(busWidthBytes: Int, params: SramBistParams)(implicit
    p: Parameters
) extends SramBistRouter(busWidthBytes, params)
    with HasTLControlRegMap

case class SramBistAttachParams(
    device: SramBistParams,
    controlWhere: TLBusWrapperLocation = PBUS,
    blockerAddr: Option[BigInt] = None,
    controlXType: ClockCrossingType = NoCrossing,
    intXType: ClockCrossingType = NoCrossing
) {
  def attachTo(where: Attachable)(implicit p: Parameters): TLSramBist = {
    val name = s"sram_bist_${SramBist.nextId()}"
    val tlbus = where.locateTLBusWrapper(controlWhere)
    val sramBistClockDomainWrapper = LazyModule(
      new ClockSinkDomain(take = None)
    )
    val sramBist = sramBistClockDomainWrapper {
      LazyModule(new TLSramBist(tlbus.beatBytes, device))
    }
    sramBist.suggestName(name)

    tlbus.coupleTo(s"device_named_$name") { bus =>

      val blockerOpt = blockerAddr.map { a =>
        val blocker = LazyModule(
          new TLClockBlocker(
            BasicBusBlockerParams(a, tlbus.beatBytes, tlbus.beatBytes)
          )
        )
        tlbus.coupleTo(s"bus_blocker_for_$name") {
          blocker.controlNode := TLFragmenter(tlbus) := _
        }
        blocker
      }

      sramBistClockDomainWrapper.clockNode := (controlXType match {
        case _: SynchronousCrossing =>
          tlbus.dtsClk.map(_.bind(sramBist.device))
          tlbus.fixedClockNode
        case _: RationalCrossing =>
          tlbus.clockNode
        case _: AsynchronousCrossing =>
          val sramBistClockGroup = ClockGroup()
          sramBistClockGroup := where.asyncClockGroupsNode
          blockerOpt.map { _.clockNode := sramBistClockGroup }.getOrElse {
            sramBistClockGroup
          }
      })

      (sramBist.controlXing(controlXType)
        := TLFragmenter(tlbus)
        := blockerOpt.map { _.node := bus }.getOrElse { bus })
    }

    sramBist
  }
}

object SramBist {
  val nextId = { var i = -1; () => { i += 1; i } }
}
