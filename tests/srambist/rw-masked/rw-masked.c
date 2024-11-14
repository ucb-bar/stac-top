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

  srambist_write(0, ref1, mask, 1);
  result = srambist_read(0, 1);

  if (result.lo != ref1.lo) {
    printf("Hardware result %d does not match first reference value %d\n", result.lo, ref1.lo);
    return 1;
  }

  ref2.lo = 1230057832;
  mask.lo = 0xfffffffa;

  srambist_write(0, ref2, mask, 1); // Assumes a *x32m*w8 SRAM
  result = srambist_read(0, 1);

  ref3.lo = ((ref2.lo & 0xff00ff00) | (ref1.lo & 0x00ff00ff));
  if (result.lo != ref3.lo) {
    printf("Hardware result %d does not match expected masked write value %d\n", result.lo, ref3.lo);
    return 1;
  }

  ref1.lo = 0xab;
  mask.lo = 0xffffffff;

  srambist_write(0, ref1, mask, 5);
  result = srambist_read(0, 5);

  if (result.lo != ref1.lo) {
    printf("Hardware result %d does not match first reference value %d\n", result.lo, ref1.lo);
    return 1;
  }

  ref2.lo = 0x78;
  mask.lo = 0xffffffaa;

  srambist_write(0, ref2, mask, 5); // Assumes a *x8m*w1 SRAM
  result = srambist_read(0, 5);

  ref3.lo = ((ref2.lo & 0xaa) | (ref1.lo & 0x55));
  if (result.lo != ref3.lo) {
    printf("Hardware result %d does not match expected masked write value %d\n", result.lo, ref3.lo);
    return 1;
  }

  ref1.lo = 0x0123456789abcdef;
  ref1.hi = 0x0123456789abcdef;
  mask.lo = 0xffffffff;

  srambist_write(0, ref1, mask, 9);
  result = srambist_read(0, 9);

  if (!eq128(result, ref1)) {
    printf("Hardware result %d does not match first reference value %d\n", result.lo, ref1.lo);
    return 1;
  }

  ref2.lo = 0xfedcba9876543210;
  ref2.hi = 0xfedcba9876543210;
  mask.lo = 0xffffaaaa;

  srambist_write(0, ref2, mask, 9); // Assumes a *x8m*w1 SRAM
  result = srambist_read(0, 9);

  ref3.lo = ((ref2.lo & 0xff00ff00ff00ff00) | (ref1.lo & 0x00ff00ff00ff00ff));
  ref3.hi = ((ref2.hi & 0xff00ff00ff00ff00) | (ref1.hi & 0x00ff00ff00ff00ff));
  if (!eq128(result, ref3)) {
    printf("Hardware result %d does not match expected masked write value %d\n", result.lo, ref3.lo);
    return 1;
  }

  printf("Test passed!\n");
  return 0;
}
