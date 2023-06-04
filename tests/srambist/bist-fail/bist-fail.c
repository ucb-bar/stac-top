// See LICENSE for license details.

//**************************************************************************
// MMIO BIST failure test
//--------------------------------------------------------------------------
//

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

//--------------------------------------------------------------------------
// Main

int main( int argc, char* argv[] )
{
  
  pattern_table_t pattern_table = { { 0, 0xffffffff, 0x5f1a950d, 0xa0e56af2, 0, 0xffffffff, 0, 0} };
  packed_element_vec_t packed_elem_vec = { { 
    3604613416985034753UL,
    295129881980453250UL,
    632782836910260224UL,
    4611404405944582UL,
    1739269488736993280UL,
    72053193842884UL,
    1180097590368362496UL,
    1125831153795UL,
    883130153304640896UL,
    17591111778UL,
    9813631697803584310UL,
    15564440312467295297UL,
    441568371429892748UL,
    3701958893702842417UL,
    14130187937237467530UL,
    1568UL
  } };
  bist_result_t result = srambist_run_bist_with_packed_elements(0, 1, 1, 15, 3, DIMENSION_ROW, &packed_elem_vec, 3, &pattern_table, 0, 1);

  if (result.fail == 0) {
    printf("BIST should have failed\n");
    return 1;
  }

  uint32_t expected_fail_cycle = 5;
  if (result.fail_cycle != expected_fail_cycle) {
    printf("BIST should have failed on cycle %d, but failed on cycle %d\n", expected_fail_cycle, result.fail_cycle);
    return 1;
  }

  uint32_t expected_expected = 0xa0e56af2U;
  if (result.expected != expected_expected) {
    printf("BIST expected value should be %d, but was %d\n", expected_expected, result.fail_cycle);
    return 1;
  }

  uint32_t expected_received = 0x5f1a950dU;
  if (result.received != expected_received) {
    printf("BIST received value %d does not match expected %d\n", result.received, expected_received);
    return 1;
  }

  printf("Test passed!\n", result);
  return 0;
}
