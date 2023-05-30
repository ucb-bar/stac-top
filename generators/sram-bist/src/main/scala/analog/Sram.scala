package srambist.analog

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import srambist.WithChiseltestSramsKey

case class SramParams(
    maskGranularity: Int,
    muxRatio: Int,
    numWords: Int,
    dataWidth: Int
)

class Sram(params: SramParams)(implicit p: Parameters) extends Module {
  val wmaskWidth = params.dataWidth / params.maskGranularity
  val io = IO(new Bundle {
    val we = Input(Bool())
    val wmask = Input(UInt(wmaskWidth.W))
    val addr = Input(UInt(log2Ceil(params.numWords).W))
    val din = Input(UInt(params.dataWidth.W))
    val saeMuxed = Input(Bool())
    val dout = Output(UInt(params.dataWidth.W))
    val saeInt = Output(Bool())
  })

  if (p(WithChiseltestSramsKey).isDefined) {
    val mem = SyncReadMem(
      params.numWords,
      Vec(wmaskWidth, UInt((params.dataWidth / wmaskWidth).W))
    )
    io.dout := "h12345678".U
    val rdwrPort = mem(io.addr)
    when(io.we) {
      for (i <- 0 to wmaskWidth - 1) {
        when(io.wmask(i)) {
          rdwrPort(i) := io.din(i)
        }
      }
    }.otherwise { io.dout := rdwrPort.asUInt }
    io.saeInt := clock.asBool
  } else {
    val inner = Module(new SramBlackBox(params))
    inner.io.we := io.we
    inner.io.wmask := io.wmask
    inner.io.addr := io.addr
    inner.io.din := io.din
    inner.io.sae_muxed := io.saeMuxed
    io.dout := inner.io.dout
    io.saeInt := inner.io.sae_int
  }

}

class SramBlackBox(params: SramParams)
    extends BlackBox
    with HasBlackBoxResource {
  val wmaskWidth = params.dataWidth / params.maskGranularity
  val io = IO(new Bundle {
    val we = Input(Bool())
    val wmask = Input(UInt(wmaskWidth.W))
    val addr = Input(UInt(log2Ceil(params.numWords).W))
    val din = Input(UInt(params.dataWidth.W))
    val sae_muxed = Input(Bool())
    val dout = Output(UInt(params.dataWidth.W))
    val sae_int = Output(Bool())
  })

  override val desiredName =
    s"sram22_${params.numWords}x${params.dataWidth}m${params.muxRatio}w${params.maskGranularity}"
}