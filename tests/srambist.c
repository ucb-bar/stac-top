#include "mmio.h"

#define SRAMBIST_ADDR 0x1000
#define SRAMBIST_DIN 0x1008
#define SRAMBIST_MASK 0x1010
#define SRAMBIST_WE 0x1018
#define SRAMBIST_SRAM_ID 0x1020
#define SRAMBIST_SRAM_SEL 0x1028
#define SRAMBIST_SAE_CTL 0x1030
#define SRAMBIST_SAE_SEL 0x1038
#define SRAMBIST_DOUT 0x1040
#define SRAMBIST_TDC 0x1048
#define SRAMBIST_DONE 0x1068
#define SRAMBIST_BIST_RAND_SEED 0x1070
#define SRAMBIST_BIST_SIG_SEED 0x1080
#define SRAMBIST_BIST_MAX_ROW_ADDR 0x1088
#define SRAMBIST_BIST_MAX_COL_ADDR 0x1090
#define SRAMBIST_BIST_INNER_DIM 0x1098
#define SRAMBIST_BIST_ELEMENT_SEQUENCE 0x10A0
#define SRAMBIST_BIST_PATTERN_TABLE 0x10E8
#define SRAMBIST_BIST_MAX_ELEMENT_IDX 0x1108
#define SRAMBIST_BIST_CYCLE_LIMIT 0x1110
#define SRAMBIST_BIST_STOP_ON_FAILURE 0x1118
#define SRAMBIST_BIST_FAIL 0x1120
#define SRAMBIST_BIST_FAIL_CYCLE 0x1128
#define SRAMBIST_BIST_EXPECTED 0x1130
#define SRAMBIST_BIST_RECEIVED 0x1138
#define SRAMBIST_BIST_SIGNATURE 0x1140
#define SRAMBIST_BIST_EX 0x1148

#define SRAM_BIST_SRAM_SEL_BIST 0
#define SRAM_BIST_SRAM_SEL_MMIO 1

// DOC include start: SRAM BIST test
int main(void)
{
  uint32_t result, ref;

  reg_write32(SRAMBIST_ADDR, 0);
  reg_write32(SRAMBIS_DIN, 25);
  reg_write32(SRAMBIST_MASK, 0xffffffff);
  reg_write8(SRAMBIST_WE, 0xff);
  reg_write8(SRAMBIST_SRAM_ID, 0);
  reg_write8(SRAMBIST_SRAM_SEL, 0xff);
  reg_write8(SRAMBIST_EX, 0xff);

  // wait for peripheral to complete
  while ((reg_read8(SRAMBIST_DONE) & 0x1) == 0) ;

  reg_write32(SRAMBIST_ADDR, 0);
  reg_write32(SRAMBIST_DIN, 0);
  reg_write32(SRAMBIST_MASK, 0);
  reg_write8(SRAMBIST_WE, 0);
  reg_write8(SRAMBIST_SRAM_ID, 0);
  reg_write8(SRAMBIST_SRAM_SEL, 0xff);
  reg_write8(SRAMBIST_EX, 0xff);

  // wait for peripheral to complete
  while ((reg_read8(SRAMBIST_DONE) & 0x1) == 0) ;

  result = reg_read32(SRAMBIST_DOUT);
  ref = 25;

  if (result != ref) {
    printf("Hardware result %d does not match reference value %d\n", result, ref);
    return 1;
  }
  printf("Hardware result %d is correct for SRAM read\n", result);
  return 0;
}
// DOC include end: SRAM BIST test
