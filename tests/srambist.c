#include "mmio.h"

#define SRAMBIST 0x1000

// DOC include start: SRAM BIST test
int main(void)
{
  uint32_t result, ref;

  reg_write32(SRAMBIST, 0);
  reg_write32(SRAMBIST + 13, 25);
  reg_write32(SRAMBIST + 45, 0xffffffff);
  reg_write8(SRAMBIST + 77, 0xff);
  reg_write8(SRAMBIST + 78, 0);
  reg_write8(SRAMBIST + 82, 0xff);
  reg_write8(SRAMBIST + 796, 0xff);

  // wait for peripheral to complete
  while ((reg_read8(SRAMBIST + 376) & 0x1) == 0) ;

  reg_write32(SRAMBIST, 0);
  reg_write32(SRAMBIST + 13, 25);
  reg_write32(SRAMBIST + 45, 0xffffffff);
  reg_write8(SRAMBIST + 77, 0);
  reg_write8(SRAMBIST + 78, 0);
  reg_write8(SRAMBIST + 82, 0xff);
  reg_write8(SRAMBIST + 796, 0xff);

  // wait for peripheral to complete
  while ((reg_read8(SRAMBIST + 376) & 0x1) == 0) ;

  result = reg_read32(SRAMBIST + 92);
  ref = 25;

  if (result != ref) {
    printf("Hardware result %d does not match reference value %d\n", result, ref);
    return 1;
  }
  printf("Hardware result %d is correct for SRAM read\n", result);
  return 0;
}
// DOC include end: SRAM BIST test
