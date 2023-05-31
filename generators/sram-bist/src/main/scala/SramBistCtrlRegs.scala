package srambist

object SramBistCtrlRegs {
  val ADDR = 0
  val DIN = 13
  val MASK = 45
  val WE = 77
  val SRAM_ID = 78
  val SRAM_SEL = 82
  val SAE_CTL = 83
  val SAE_SEL = 90
  val DOUT = 92
  val TDC = 124
  val DONE = 376
  val BIST_RAND_SEED = 377
  val BIST_SIG_SEED = 454
  val BIST_MAX_ROW_ADDR = 486
  val BIST_MAX_COL_ADDR = 496
  val BIST_INNER_DIM = 499
  val BIST_ELEMENT_SEQUENCE = 500
  val BIST_PATTERN_TABLE = 564
  val BIST_MAX_ELEMENT_IDX = 628
  val BIST_CYCLE_LIMIT = 634
  val BIST_STOP_ON_FAILURE = 666
  val BIST_FAIL = 667
  val BIST_FAIL_CYCLE = 668
  val BIST_EXPECTED = 700
  val BIST_RECEIVED = 732
  val BIST_SIGNATURE = 764
  val EX = 796
}

object SramBistCtrlRegWidths {
  val ADDR = 13
  val DIN = 32
  val MASK = 32
  val WE = 1
  val SRAM_ID = 4
  val SRAM_SEL = 1
  val SAE_CTL = 7
  val SAE_SEL = 2
  val DOUT = 32
  val TDC = 252
  val DONE = 1
  val BIST_RAND_SEED = 77
  val BIST_SIG_SEED = 32
  val BIST_MAX_ROW_ADDR = 10
  val BIST_MAX_COL_ADDR = 3
  val BIST_INNER_DIM = 1
  val BIST_ELEMENT_SEQUENCE = 64
  val BIST_PATTERN_TABLE = 64
  val BIST_MAX_ELEMENT_IDX = 6
  val BIST_CYCLE_LIMIT = 32
  val BIST_STOP_ON_FAILURE = 1
  val BIST_FAIL = 1
  val BIST_FAIL_CYCLE = 32
  val BIST_EXPECTED = 32
  val BIST_RECEIVED = 32
  val BIST_SIGNATURE = 32
  val TOTAL = 796
}
