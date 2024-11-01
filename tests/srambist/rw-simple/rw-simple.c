// See LICENSE for license details.

//**************************************************************************
// MMIO read and write test
//--------------------------------------------------------------------------
//

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

int main( int argc, char* argv[] )
{
  uint128_t result, ref, mask;
  ref.lo = 0xabababab;
  mask.lo = 0xffffffff;

  srambist_write(0, ref, mask, 0);
  result = srambist_read(0, 0);

  if (result.lo != ref.lo) {
    printf("Hardware result %d does not match reference value %d\n", result.lo, ref.lo);
    return 1;
  }
  printf("Hardware result %d is correct for SRAM read\n", result.lo);
  return 0;
}

