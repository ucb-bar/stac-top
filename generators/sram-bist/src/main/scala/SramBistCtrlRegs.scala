package srambist

import scala.collection.mutable.LinkedHashMap

object SramBistCtrlRegs extends Enumeration {
  type Type = Value
  val ADDR, DIN, MASK, WE, SRAM_ID, SRAM_SEL, SAE_CTL, SAE_SEL, DOUT, TDC, DONE,
      BIST_RAND_SEED, BIST_SIG_SEED, BIST_MAX_ROW_ADDR, BIST_MAX_COL_ADDR,
      BIST_INNER_DIM, BIST_ELEMENT_SEQUENCE, BIST_PATTERN_TABLE,
      BIST_MAX_ELEMENT_IDX, BIST_CYCLE_LIMIT, BIST_STOP_ON_FAILURE, BIST_FAIL,
      BIST_FAIL_CYCLE, BIST_EXPECTED, BIST_RECEIVED, BIST_SIGNATURE, EX = Value

  val REG_WIDTH = LinkedHashMap(
    ADDR -> 13,
    DIN -> 32,
    MASK -> 32,
    WE -> 1,
    SRAM_ID -> 4,
    SRAM_SEL -> 1,
    SAE_CTL -> 7,
    SAE_SEL -> 2,
    DOUT -> 32,
    TDC -> 252,
    DONE -> 1,
    BIST_RAND_SEED -> 78,
    BIST_SIG_SEED -> 32,
    BIST_MAX_ROW_ADDR -> 10,
    BIST_MAX_COL_ADDR -> 3,
    BIST_INNER_DIM -> 1,
    BIST_ELEMENT_SEQUENCE -> 552,
    BIST_PATTERN_TABLE -> 256,
    BIST_MAX_ELEMENT_IDX -> 6,
    BIST_CYCLE_LIMIT -> 32,
    BIST_STOP_ON_FAILURE -> 1,
    BIST_FAIL -> 1,
    BIST_FAIL_CYCLE -> 32,
    BIST_EXPECTED -> 32,
    BIST_RECEIVED -> 32,
    BIST_SIGNATURE -> 32
  )
  val TOTAL_REG_WIDTH = REG_WIDTH.values.sum

  val SCAN_CHAIN_OFFSET =
    REG_WIDTH.keys.zip(REG_WIDTH.values.scanLeft(0)(_ + _).dropRight(1)).toMap

  val SCAN_OUT_OFFSET =
    REG_WIDTH.keys.zip(REG_WIDTH.values.scanRight(0)(_ + _).drop(1)).toMap

  val REGMAP_OFFSET =
    (REG_WIDTH.keys ++ Iterator(EX))
      .zip(
        REG_WIDTH.values.scanLeft(0)((acc, n) => acc + ((n - 1) / 64 + 1) * 8)
      )
      .toMap
}
