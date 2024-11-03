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
  uint128_t result, ref1, mask1, ref2, mask2;
  ref1.lo = 0xabababab;
  mask1.lo = 0xffffffff;

  srambist_write(0, ref1, mask1, 0);
  result = srambist_read(0, 0);

  if (result.lo != ref1.lo) {
    printf("Hardware result 0x%x does not match first reference value 0x%x\n", result.lo, ref1.lo);
    return 1;
  }

  ref2.hi = 0xcdcdcdcdcdcdcdcdULL;
  ref2.lo = 0xefefefefefefefefULL;
  mask2.hi = 0xffffffffffffffffULL;
  mask2.lo = 0xffffffffffffffffULL;

  srambist_write(0, ref2, mask2, 9);
  result = srambist_read(0, 6);

  if (!eq128(result, ref2)) {
    printf("Hardware result 0x%llx%llx does not match second reference value 0x%llx%llx\n", result.hi, result.lo, ref2.hi, ref2.lo);
    return 1;
  }

  result = srambist_read(0, 0);

  if (result.lo != ref1.lo) {
    printf("Hardware result %d does not match original reference value %d after writing to second SRAM\n", result.lo, ref1.lo);
    return 1;
  }


  printf("Test passed!\n");
  return 0;
}
