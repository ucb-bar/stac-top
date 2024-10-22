package srambist.analog

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import srambist.{WithChiseltestSramsKey, ChiseltestSramFailureMode}

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
    val dout = Output(UInt(params.dataWidth.W))
  })

  val chiseltestCfg = p(WithChiseltestSramsKey)

  chiseltestCfg match {
    case (Some(failureMode)) => {
      val mem = SyncReadMem(
        params.numWords,
        Vec(wmaskWidth, UInt(params.maskGranularity.W))
      )
      io.dout := DontCare
      val rdPort = mem(io.addr)
      val wrPort = mem(io.addr)
      when(io.we) {
        val toWrite = Wire(Vec(params.dataWidth, Bool()))
        toWrite := io.din.asBools
        failureMode match {
          case ChiseltestSramFailureMode.stuckAt => {
            when(io.addr === 29.U) {
              toWrite(5) := false.B
            }
          }
          case ChiseltestSramFailureMode.transition => {
            when(io.addr === 15.U) {
              when(rdPort(0)(0) & ~io.din(0)) {
                toWrite(0) := true.B
              }
            }
          }
          case _ => {}
        }
        for (i <- 0 to wmaskWidth - 1) {
          when(io.wmask(i)) {
            wrPort(i) := toWrite.asUInt(
              params.maskGranularity * (i + 1) - 1,
              params.maskGranularity * i
            )
          }
        }
      }.otherwise {
        var out = rdPort(0)
        for (i <- 1 to wmaskWidth - 1) {
          out = Cat(rdPort(i), out)
        }
        io.dout := out
      }
    }
    case None => {
      val inner = Module(new SramBlackBox(params))
      inner.io.clk := clock
      inner.io.we := io.we
      inner.io.wmask := io.wmask
      inner.io.addr := io.addr
      inner.io.din := io.din
      io.dout := inner.io.dout
    }
  }

}

class SramBlackBox(params: SramParams)
    extends BlackBox
    with HasBlackBoxResource {
  val wmaskWidth = params.dataWidth / params.maskGranularity
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val we = Input(Bool())
    val wmask = Input(UInt(wmaskWidth.W))
    val addr = Input(UInt(log2Ceil(params.numWords).W))
    val din = Input(UInt(params.dataWidth.W))
    val dout = Output(UInt(params.dataWidth.W))
  })

  override val desiredName =
    s"sram22_${params.numWords}x${params.dataWidth}m${params.muxRatio}w${params.maskGranularity}"

  addResource(s"/vsrc/$desiredName.v")
}
