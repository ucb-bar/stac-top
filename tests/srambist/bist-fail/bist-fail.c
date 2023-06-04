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
  
  pattern_table_t pattern_table = { { 0, 0xffffffff, 0xcccccccc, 0, 0, 0, 0, 0} };
  operation_t write_op = srambist_operation_init(OP_TYPE_WRITE, 0, 0, 0, 1, FLIP_TYPE_UNFLIPPED);
  operation_t read_op = srambist_operation_init(OP_TYPE_READ, 0, 0, 0, 1, FLIP_TYPE_UNFLIPPED);
  operation_t write_op_flipped = srambist_operation_init(OP_TYPE_WRITE, 0, 0, 0, 1, FLIP_TYPE_FLIPPED);
  operation_t read_op_fail = srambist_operation_init(OP_TYPE_READ, 0, 0, 2, 1, FLIP_TYPE_FLIPPED);

  operation_t ops[4] = {write_op, read_op, write_op_flipped, read_op_fail};
  element_t op_elem = srambist_operation_element_init(ops, 3, DIRECTION_UP, 0);

  element_t elems[2] = {op_elem, op_elem};

  bist_result_t result = srambist_run_bist(0, 1, 55, 15, 7, DIMENSION_COL, elems, 1, pattern_table, 0, 1);

  if (result.fail == 0) {
    printf("BIST should have failed\n");
    return 1;
  }

  uint32_t expected_fail_cycle = 3;
  if (result.fail_cycle != expected_fail_cycle) {
    printf("BIST should have failed on cycle %d, but failed on cycle %d\n", expected_fail_cycle, result.fail_cycle);
    return 1;
  }

  uint32_t expected_expected = 0x;
  if (result.expected != expected_expected) {
    printf("BIST expected value should be %d, but was %d\n", expected_expected, result.fail_cycle);
    return 1;
  }

  uint32_t expected_signature = 3236271003;
  if (result.signature == expected_signature) {
    printf("BIST signature %d does not match expected %d\n", result.signature, expected_signature);
    return 1;
  }

  printf("Hardware result %d is correct for SRAM read\n", result);
  return 0;
}
