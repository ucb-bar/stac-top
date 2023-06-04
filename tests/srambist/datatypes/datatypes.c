// See LICENSE for license details.

//**************************************************************************
// Data types and helper functions test
//--------------------------------------------------------------------------
//

#include "srambist.h"
#include <stdint.h>
#include <stdio.h>

//--------------------------------------------------------------------------
// Main

int main( int argc, char* argv[] )
{
  uint64_t test = 0;
  
  // Validate bit helper functions.
  uint32_t val = 0xff;
  write_at_bit_offset(&test, 3, &val, 2);
  if (test != 24) {
    printf("Failed to write at bit offset. Expected 24, got %d\n", test);
    return 1;
  }
  val = 0x9;
  write_at_bit_offset(&test, 0, &val, 4);
  if (test != 25) {
    printf("Failed to write at bit offset. Expected 25, got %d\n", test);
    return 1;
  }
  uint32_t rval = read_at_bit_offset(&test, 2, 3);
  if (rval != 6) {
    printf("Failed to read at bit offset. Expected 6, got %d\n", rval);
    return 1;
  }

  // Validate initialization functions.
  operation_t op = srambist_operation_init(OP_TYPE_WRITE, 0, 0, 7, 3, FLIP_TYPE_UNFLIPPED);

  if (op.operation_type != OP_TYPE_WRITE) {
    printf("Failed to initialize operation. Expected OP_TYPE_WRITE, got %d\n", op.operation_type);
    return 1;
  }
  if (op.rand_data != 0) {
    printf("Failed to initialize operation. Expected 0, got %d\n", op.rand_data);
    return 1;
  }
  if (op.rand_mask != 0) {
    printf("Failed to initialize operation. Expected 0, got %d\n", op.rand_mask);
    return 1;
  }
  if (op.data_pattern_idx != 7) {
    printf("Failed to initialize operation. Expected 7, got %d\n", op.data_pattern_idx);
    return 1;
  }
  if (op.mask_pattern_idx != 3) {
    printf("Failed to initialize operation. Expected 3, got %d\n", op.mask_pattern_idx);
    return 1;
  }
  if (op.flipped != FLIP_TYPE_UNFLIPPED) {
    printf("Failed to initialize operation. Expected FLIP_TYPE_UNFLIPPED, got %d\n", op.flipped);
    return 1;
  }

  operation_t ops[3] = {op, op, op};

  element_t op_elem = srambist_operation_element_init(ops, 2, DIRECTION_DOWN, 50);

  for (int i = 0; i < 3; i++) {
    if (op_elem.operation_element.operations[i].operation_type != op.operation_type) {
      printf("Failed to initialize operation element. Expected %d, got %d\n", op_elem.operation_element.operations[i].operation_type, op.operation_type);
      return 1;
    }
    if (op_elem.operation_element.operations[i].data_pattern_idx != op.data_pattern_idx) {
      printf("Failed to initialize operation element. Expected %d, got %d\n", op_elem.operation_element.operations[i].data_pattern_idx, op.data_pattern_idx);
      return 1;
    }
  }
  if (op_elem.operation_element.max_idx != 2) {
    printf("Failed to initialize operation element. Expected 2, got %d\n", op_elem.operation_element.max_idx);
    return 1;
  }
  if (op_elem.operation_element.dir != DIRECTION_DOWN) {
    printf("Failed to initialize operation element. Expected DIRECTION_DOWN, got %d\n", op_elem.operation_element.dir);
    return 1;
  }
  if (op_elem.operation_element.num_addrs != 50) {
    printf("Failed to initialize operation element. Expected 50, got %d\n", op_elem.operation_element.num_addrs);
    return 1;
  }

  element_t wait_elem = srambist_wait_element_init(123);

  if (wait_elem.wait_element.rand_addr != 123) {
    printf("Failed to initialize wait element. Expected 123, got %d\n", wait_elem.wait_element.rand_addr);
    return 1;
  }

  packed_operation_t packed_op = pack_operation(&op);

  // Validate bit positions.
  if (read_at_bit_offset(&packed_op, 0, 2) != OP_TYPE_WRITE) {
    printf("Failed to pack operation. Expected OP_TYPE_WRITE, got %d\n", read_at_bit_offset(&packed_op, 0, 2));
    return 1;
  }
  if (read_at_bit_offset(&packed_op, 2, 1) != 0) {
    printf("Failed to pack operation. Expected 0, got %d\n", read_at_bit_offset(&packed_op, 2, 1));
    return 1;
  }
  if (read_at_bit_offset(&packed_op, 3, 1) != 0) {
    printf("Failed to pack operation. Expected 0, got %d\n", read_at_bit_offset(&packed_op, 3, 1));
    return 1;
  }
  if (read_at_bit_offset(&packed_op, 4, 3) != 7) {
    printf("Failed to pack operation. Expected 7, got %d\n", read_at_bit_offset(&packed_op, 4, 3));
    return 1;
  }
  if (read_at_bit_offset(&packed_op, 7, 3) != 3) {
    printf("Failed to pack operation. Expected 3, got %d\n", read_at_bit_offset(&packed_op, 7, 3));
    return 1;
  }
  if (read_at_bit_offset(&packed_op, 10, 1) != FLIP_TYPE_UNFLIPPED) {
    printf("Failed to pack operation. Expected FLIP_TYPE_UNFLIPPED, got %d\n", read_at_bit_offset(&packed_op, 10, 1));
    return 1;
  }

  packed_element_t packed_op_elem = pack_element(&op_elem);

  for (int i = 0; i < 3; i++) {
    if (read_at_bit_offset(&packed_op_elem, 11 * i, 11) != read_at_bit_offset(&packed_op, 0, 11)) {
      printf("Failed to pack operation element. Expected first 3 operations to match");
      return 1;
    }
  }
  if (read_at_bit_offset(&packed_op_elem, 88, 3) != 2) {
    printf("Failed to pack operation element. Expected 2, got %d\n", read_at_bit_offset(&packed_op_elem, 88, 3));
    return 1;
  }
  if (read_at_bit_offset(&packed_op_elem, 91, 2) != DIRECTION_DOWN) {
    printf("Failed to pack operation element. Expected DIRECTION_DOWN, got %d\n", read_at_bit_offset(&packed_op_elem, 91, 2));
    return 1;
  }
  if (read_at_bit_offset(&packed_op_elem, 93, 14) != 50) { 
    printf("Failed to pack operation element. Expected 50, got %d\n", read_at_bit_offset(&packed_op_elem, 93, 14));
    return 1;
  }
  if (read_at_bit_offset(&packed_op_elem, 121, 1) != ELEMENT_TYPE_RW) {
    printf("Failed to pack operation element. Expected ELEMENT_TYPE_RW, got %d\n", read_at_bit_offset(&packed_op_elem, 121, 1));
    return 1;
  }

  element_t elems[2] = {op_elem, wait_elem};
  packed_element_vec_t packed_elem_vec = pack_element_vec(elems, 1);
  if (read_at_bit_offset(&packed_elem_vec, 93, 14) != 50) {
    printf("Failed to pack element vec. Expected first element to match operation element");
    return 1;
  }
  if (read_at_bit_offset(&packed_elem_vec, 122 + 107, 14) != 123) {
    printf("Failed to pack element vec. Expected second element to match wait element");
    return 1;
  }

  printf("Successfully completed all checks.\n");

  return 0;
}
