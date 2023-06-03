package srambist

import srambist.misr.MISR
import org.scalatest.flatspec.AnyFlatSpec

class MaxPeriodFibonacciXORMISRModel(width: Int, seed: Option[BigInt] = Some(1)) {
  val taps = MISR.tapsMaxPeriod(width).head
  var state: BigInt = seed.get

  def add(data: BigInt): Unit = {
    val newBit = taps.map(tap => data.testBit(tap)).reduce(_ ^ _)
    for (i <- width - 1 to 0 by -1) {
      if (state.testBit(i-1)) { state.setBit(i) } else { state.clearBit(i) }
    }
    if (newBit) {
      state.setBit(0)
    } else {
      state.clearBit(0)
    }

    state = state ^ data
  }
}

class MaxPeriodFibonacciXORMISRModelSpec extends AnyFlatSpec {
  behavior of "BistTop"
  it should "work with hardmacro SRAMs" in {
    var c = new MaxPeriodFibonacciXORMISRModel(5, Some(5))
    assert(c.state == 5)
    c.add(20)
    assert(c.state)
  }
}
