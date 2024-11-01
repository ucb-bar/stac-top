// See LICENSE for license details.

//**************************************************************************
// MMIO read and write test
//--------------------------------------------------------------------------
//

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

//--------------------------------------------------------------------------
// Main

int main( int argc, char* argv[] )
{
  uint128_t result, ref1, ref2, ref3, mask;
  ref1.lo = 2147483647;
  mask.lo = 0xffffffff;

  srambist_write(0, ref1, mask, 0);
  result = srambist_read(0, 0);

  if (result.lo != ref1.lo) {
    printf("Hardware result %d does not match first reference value %d\n", result.lo, ref1.lo);
    return 1;
  }

  ref2.lo = 1230057832;
  mask.lo = 0xfffffffa;

  srambist_write(0, ref2, mask, 0); // Assumes a *x32m*w8 SRAM
  result = srambist_read(0, 0);

  ref3.lo = ((ref2.lo & 0xff00ff00) | (ref1.lo & 0x00ff00ff));
  if (result.lo != ref3.lo) {
    printf("Hardware result %d does not match expected masked write value %d\n", result.lo, ref3.lo);
    return 1;
  }

  printf("Test passed!\n");
  return 0;
}
