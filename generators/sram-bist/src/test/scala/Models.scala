package srambist

import srambist.misr.MISR
import org.scalatest.flatspec.AnyFlatSpec

class MaxPeriodFibonacciXORMISRModel(
    width: Int,
    seed: Option[BigInt] = Some(1)
) {
  val taps = MISR.tapsMaxPeriod(width).head
  var state: BigInt = seed.get

  def forcePositive(in: BigInt): BigInt = {
    BigInt(Array[Byte](0) ++ in.toByteArray)
  }

  def add(data: BigInt): Unit = {
    var pdata = forcePositive(data)

    val newBit = taps.toList.map(tap => state.testBit(tap - 1)).reduce(_ ^ _)
    state = state << 1
    if (newBit) {
      state += 1
    }

    state = state ^ pdata
    state = forcePositive(state.clearBit(width))
  }
}

class MaxPeriodFibonacciXORMISRModelSpec extends AnyFlatSpec {
  behavior of "MaxPeriodFibonacciXORMISRModel"
  it should "work with width 5 and seed 5" in {
    var c = new MaxPeriodFibonacciXORMISRModel(5, Some(5))
    assert(c.state == 5)
    c.add(20)
    assert(c.state == 31)
  }
}
