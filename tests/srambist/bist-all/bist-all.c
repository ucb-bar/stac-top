// See LICENSE for license details.

//**************************************************************************
// MMIO read and write test
//--------------------------------------------------------------------------
//

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

int main(int argc, char* argv[] )
{
  pattern_table_t pattern_table = { 
      { 
          {0x0, 0x0},
          {0xffffffffffffffff, 0xffffffffffffffff},
          {0xc76148fef684be4e, 0x5f1a950d9af236fc},
          {0x389eb701097b41b1, 0xa0e56af2650dc903},
          {0x0, 0x0},
          {0xffffffffffffffff, 0xffffffffffffffff},
          {0x0, 0x0},
          {0x0, 0x0}
      } 
  };
  packed_element_vec_t packed_elem_vec = { { 
    9297163343833858049UL,
    295129881955305867UL,
    3315802314916233216UL,
    4611404405551654UL,
    11004563704935612416UL,
    72053193836744UL,
    2477789317103312896UL,
    1125831153699UL,
    10126778623389650304UL,
    17591111776UL,
    9381602952845239094UL,
    15564440312467295297UL,
    434817922289918604UL,
    3701958893702842417UL,
    14130082461469655434UL,
    1568UL
  } };

  uint128_t expected_sigs[NUM_SRAMS] = {
      {       0x41f384000UL, 0x0000000a4dec8b59UL},
      {       0x41f3a0000UL, 0x000001ca21ec8b59UL},
      { 0x16edc5550000000UL, 0x5bd603f6e03c98e1UL},
      { 0x16edf9b805d4000UL, 0x5bd49446cbac98e1UL},
      { 0x16edf9b80720000UL, 0x5ba49440946c98e1UL},
      { 0xa68ac2b26014061UL, 0xdd7ee8413fd030b2UL},
      {0x2c5f4c50502a48bbUL, 0xde1a000150017ccbUL},
      {0x24106da3f57f6efbUL, 0x8d2c11936ac162fcUL},
      {0x6104704b2b623769UL, 0x8cec64c4576f3a91UL},
      {0x3ad8a26d3a575d1bUL, 0xd1a22b13f55a2d9dUL},
      {0x10228492ee6a2257UL, 0x7c1413a9589813f6UL},
      {0xeb602ce2cc3e32c9UL, 0x301de3dba7986448UL},
      {0x2c21f241390a3e72UL, 0xf2318e1b1ee9155eUL},
      {0xe5a50891b6156b81UL, 0xa6a894b830ab6edcUL},
      {0x533b6248826d81e7UL, 0x441f183d7209bb61UL},
      {0x3abb43d3924b2921UL, 0xa9d653e80700ada2UL},
      {0xa43d9cf23289a6f4UL, 0xe6b17f64cd8c0744UL},
      {0xd62e66806202fd96UL, 0xff83ec87e20de4c5UL},
      {0x7b13d7b8f7ea78a0UL, 0xb07569d9524b2a29UL},
      { 0xfd9b78981107803UL, 0x00d23a3f562f6f58UL},
      {0xf31a611da7e2b35bUL, 0xdf535489dad0bd17UL},
      {0xd01abb8000261250UL, 0x15405ef8735dee1bUL}
  };

  uint128_t bist_sig = {1, 0};
  srambist_write_packed_elements(&packed_elem_vec, &pattern_table);

  for (int id = 0; id < NUM_SRAMS; id++) {
    printf("Testing bist for SRAM %d\n", id);
    sram_params_t params = SRAMS[id];

    int rows = params.num_words / params.mux_ratio;
    int cols = params.mux_ratio;


    bist_result_t result = srambist_run_bist_with_existing_patterns(id, 1, bist_sig, rows - 1, cols - 1, DIMENSION_ROW, 1, 0, 1);
    if (result.fail) {
        printf("(SRAM %d, CYCLE %d) Hardware result 0x%llx%llx does not match reference value 0x%llx%llx\n", id, result.fail_cycle, result.received.hi, result.received.lo, result.expected.hi, result.expected.lo);
        return 1;
    }
    if (!eq128(result.signature, expected_sigs[id])) {
        printf("(SRAM %d) Signature 0x%llx%llx does not match reference value 0x%llx%llx\n", id, result.signature.hi, result.signature.lo, expected_sigs[id].hi, expected_sigs[id].lo);
        return 1;
    }

    printf("Finished testing SRAM %d\n", id);
  }

  printf("Test passed!\n");
  return 0;
}
