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
  uint128_t result, ref, mask;
  mask.lo = 0xffffffff;

  for (int i = 0; i < 512; i++) {
    ref.lo = 0x80000000 & i;

    srambist_write(i, ref, mask, 8);
  }
  for (int i = 0; i < 512; i++) {
    ref.lo = 0x80000000 & i;

    result = srambist_read(0, 0);

    if (result.lo != ref.lo) {
        printf("Hardware result %d does not match first reference value %d\n", result.lo, ref.lo);
        return 1;
    }
  }

  printf("Test passed!\n");
  return 0;
}
