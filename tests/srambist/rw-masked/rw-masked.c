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
  uint32_t result, ref1, ref2, ref3;
  ref1 = 2147483647;

  srambist_write(0, ref1, 0xffffffff, 0);
  result = srambist_read(0, 0);

  if (result != ref1) {
    printf("Hardware result %d does not match first reference value %d\n", result, ref1);
    return 1;
  }

  ref2 = 1230057832;

  srambist_write(0, ref2, 0xfffffffa, 0); // Assumes a *x32m*w8 SRAM
  result = srambist_read(0, 0);

  ref3 = ((ref2 & 0xff00ff00) | (ref1 & 0x00ff00ff));
  if (result != ref3) {
    printf("Hardware result %d does not match expected masked write value %d\n", result, ref3);
    return 1;
  }

  printf("Test passed!\n", result);
  return 0;
}
