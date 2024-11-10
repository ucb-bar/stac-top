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
  uint128_t result, ref;

  for (int id = 0; id < NUM_SRAMS; id++) {
    printf("Testing SRAM %d\n", id);
    sram_params_t params = SRAMS[id];
    uint128_t mask = {0xffffffffffffffff, 0xffffffffffffffff};
    for (int i = 0; i < params.num_words; i+=params.num_words - 1) {
        ref.lo = 0xabababababababab + NUM_SRAMS * i + id;
        ref.hi = 0xcdcdcdcdcdcdcdcd;

        srambist_write(i, ref, mask, id);
    }
    uint128_t data_mask = create_mask(id);
    for (int i = 0; i < params.num_words; i+=params.num_words - 1) {
        ref.lo = 0xabababababababab + NUM_SRAMS * i + id;
        ref.hi = 0xcdcdcdcdcdcdcdcd;

        result = srambist_read(i, id);
        ref.hi = ref.hi & data_mask.hi;
        ref.lo = ref.lo & data_mask.lo;
        result.hi = result.hi & data_mask.hi;
        result.lo = result.lo & data_mask.lo;

        if (!eq128(result, ref)) {
            printf("(SRAM %d, ADDR %d) Hardware result 0x%llx%llx does not match reference value 0x%llx%llx\n", id, i, result.hi, result.lo, ref.hi, ref.lo);
            return 1;
        }
    }
    printf("Finished testing SRAM %d\n", id);
  }

  printf("Test passed!\n");
  return 0;


  printf("Test passed!\n");
  return 0;
}
