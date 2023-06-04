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
  uint32_t result, ref1, ref2;
  ref1 = 2147483647;

  srambist_write(0, ref1, 0xffffffff, 0);
  result = srambist_read(0, 0);

  if (result != ref1) {
    printf("Hardware result %d does not match first reference value %d\n", result, ref1);
    return 1;
  }

  ref2 = 1230057832;

  srambist_write(0, ref2, 0xffffffff, 1);
  result = srambist_read(0, 1);

  if (result != ref2) {
    printf("Hardware result %d does not match second reference value %d\n", result, ref2);
    return 1;
  }

  result = srambist_read(0, 0);

  if (result != ref1) {
    printf("Hardware result %d does not match original reference value %d after writing to second SRAM\n", result, ref1);
    return 1;
  }


  printf("Test passed!\n", result);
  return 0;
}
