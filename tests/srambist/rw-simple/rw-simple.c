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
