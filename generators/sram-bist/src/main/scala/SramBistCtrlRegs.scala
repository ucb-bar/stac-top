package srambist

object SramBistCtrlRegs {
  val ADDR = 0x00
  val DIN = 0x04
  val MASK = 0x08
  val WE = 0x0c
  val SRAM_ID = 0x10
  val SRAM_SEL = 0x14
  val SAE_CTL = 0x18
  val SAE_SEL = 0x1c
  val DOUT = 0x20
  val TDC = 0x24
  val EX = 0x28
  val DONE = 0x2c
  val BIST_RAND_SEED = 0x30
  val BIST_SIG_SEED = 0x34
  val BIST_MAX_ROW_ADDR = 0x38
  val BIST_MAX_COL_ADDR = 0x3c
  val BIST_INNER_DIM = 0x40
  val BIST_ELEMENT_SEQUENCE = 0x44
  val BIST_PATTERN_TABLE = 0x48
  val BIST_MAX_ELEMENT_IDX = 0x48
  val BIST_CYCLE_LIMIT = 0x4c
  val BIST_FAIL = 0x50
  val BIST_FAIL_CYCLE = 0x54
  val BIST_EXPECTED = 0x54
  val BIST_RECEIVED = 0x54
  val BIST_SIGNATURE = 0x54
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
  val BIST_FAIL = 1
  val BIST_FAIL_CYCLE = 32
  val BIST_EXPECTED = 32
  val BIST_RECEIVED = 32
  val BIST_SIGNATURE = 32
  val TOTAL = 763
}
