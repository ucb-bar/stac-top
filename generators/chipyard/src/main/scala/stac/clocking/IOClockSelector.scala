package chipyard.stac.clocking

import chipyard.clocking.{ClockSelNode, ResetStretcher}
import chisel3._
import freechips.rocketchip.diplomacy.{BundleBridgeSource, LazyModule, LazyModuleImp}
import org.chipsalliance.cde.config.Parameters

class IOClockSelectorIO(selBits: Int) extends Bundle {
  val sel = Input(UInt(selBits.W))
}

class IOClockSelector(selBits: Int)(implicit p: Parameters) extends LazyModule {
  val clockNode = ClockSelNode()
  val ioNode = BundleBridgeSource(() => new IOClockSelectorIO(selBits))

  override lazy val module = new LazyModuleImp(this) {
    val sel = ioNode.bundle.sel

    require(clockNode.in.size <= (1 << selBits),
      s"Too many input clocks (have $selBits select bits, got ${clockNode.in.size} inputs)")

    val asyncReset = clockNode.in.map(_._1).map(_.reset).head
    val clocks = clockNode.in.map(_._1).map(_.clock)
    val (outClocks, _) = clockNode.out.head
    val (sinkNames, sinks) = outClocks.member.elements.toSeq.unzip

    val mux = testchipip.ClockMutexMux(clocks).suggestName("clkmux")
    mux.io.sel := sel
    mux.io.resetAsync := asyncReset.asAsyncReset

    println()
    println("Clock Mux sources:")
    clockNode.in.zipWithIndex.foreach { case ((_, edge), i) =>
      println(s"  $i: ${edge.source.name}")
    }
    println("Clock Mux sinks:")
    sinks.indices.foreach { i =>
      val sinkName = sinkNames(i)
      println(s"  $i: $sinkName")

      sinks(i).clock := mux.io.clockOut
      // Stretch the reset for 20 cycles, to give time to reset any downstream digital logic
      sinks(i).reset := ResetStretcher(clocks.head, asyncReset, 20).asAsyncReset
    }
    println()
  }
}
