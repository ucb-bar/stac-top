// See LICENSE for license details.

//**************************************************************************
// SRAM BIST (c version)
//--------------------------------------------------------------------------

#include "mmio.h"
#include <stdio.h>

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
#define SRAMBIST_EX 0x1148

#define SRAM_BIST_SRAM_SEL_BIST 0
#define SRAM_BIST_SRAM_SEL_MMIO 1

void srambist_write(uint32_t addr, uint32_t din, uint32_t mask, uint8_t sram_id) {
  reg_write32(SRAMBIST_ADDR, addr);
  reg_write32(SRAMBIST_DIN, din);
  reg_write32(SRAMBIST_MASK, mask);
  reg_write8(SRAMBIST_WE, 1);
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_BIST_SRAM_SEL_MMIO);
  reg_write8(SRAMBIST_EX, 1);

  while ((reg_read8(SRAMBIST_DONE) & 0x1) == 0);
}

uint32_t srambist_read(uint32_t addr, uint8_t sram_id) {
  reg_write32(SRAMBIST_ADDR, addr);
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_WE, 0);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_BIST_SRAM_SEL_MMIO);
  reg_write8(SRAMBIST_EX, 1);

  while ((reg_read8(SRAMBIST_DONE) & 0x1) == 0);

  return reg_read32(SRAMBIST_DOUT);
}

int srambist()
{
  uint32_t result, ref;
  ref = 2147483647;

  srambist_write(0, ref, 0xffffffff, 0);
  result = srambist_read(0, 0);

  if (result != ref) {
    printf("Hardware result %d does not match reference value %d\n", result, ref);
    return 1;
  }
  printf("Hardware result %d is correct for SRAM read\n", result);
  return 0;
}
