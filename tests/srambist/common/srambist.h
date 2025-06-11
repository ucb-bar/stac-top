#ifndef __SRAMBIST_H__
#define __SRAMBIST_H__
#include <stdint.h>

#define SRAMBIST_ADDR 0x1000
#define SRAMBIST_DIN 0x1008
#define SRAMBIST_MASK 0x1018
#define SRAMBIST_WE 0x1028
#define SRAMBIST_SRAM_ID 0x1030
#define SRAMBIST_SRAM_SEL 0x1038
#define SRAMBIST_DOUT 0x1040
#define SRAMBIST_DONE 0x1050
#define SRAMBIST_BIST_RAND_SEED 0x1058
#define SRAMBIST_BIST_SIG_SEED 0x1080
#define SRAMBIST_BIST_MAX_ROW_ADDR 0x1090
#define SRAMBIST_BIST_MAX_COL_ADDR 0x1098
#define SRAMBIST_BIST_INNER_DIM 0x10A0
#define SRAMBIST_BIST_ELEMENT_SEQUENCE 0x10A8
#define SRAMBIST_BIST_PATTERN_TABLE 0x1128
#define SRAMBIST_BIST_MAX_ELEMENT_IDX 0x11A8
#define SRAMBIST_BIST_CYCLE_LIMIT 0x11B0
#define SRAMBIST_BIST_STOP_ON_FAILURE 0x11B8
#define SRAMBIST_BIST_FAIL 0x11C0
#define SRAMBIST_BIST_FAIL_CYCLE 0x11C8
#define SRAMBIST_BIST_EXPECTED 0x11D0
#define SRAMBIST_BIST_RECEIVED 0x11E0
#define SRAMBIST_BIST_SIGNATURE 0x11F0
#define SRAMBIST_EX 0x1200

#define SRAMBIST_PATTERN_TABLE_LENGTH_LOG2 3
#define SRAMBIST_OPERATIONS_PER_ELEMENT 8
#define SRAMBIST_OPERATIONS_PER_ELEMENT_LOG2 3
#define SRAMBIST_RAND_ADDR_WIDTH 14
#define SRAMBIST_ELEMENT_TABLE_LENGTH 8

typedef struct {
    int wmask_granularity;
    int mux_ratio;
    int num_words;
    int data_width;
} sram_params_t;

typedef struct {
    uint128_t* data;
    uint128_t* wmask;
} sram_test_t;

#define NUM_SRAMS 22
extern const sram_params_t SRAMS[NUM_SRAMS];

typedef enum {
  SRAM_SEL_MMIO = 0,
  SRAM_SEL_BIST = 1,
} sram_sel_t;

typedef enum {
  OP_TYPE_READ = 0,
  OP_TYPE_WRITE = 1,
  OP_TYPE_RAND = 2,
} operation_type_t;

typedef enum {
  FLIP_TYPE_FLIPPED = 0,
  FLIP_TYPE_UNFLIPPED = 1,
} flip_type_t;

typedef enum {
  DIRECTION_UP = 0,
  DIRECTION_DOWN = 1,
  DIRECTION_RAND = 2,
} direction_t;

typedef enum {
  ELEMENT_TYPE_WAIT = 0,
  ELEMENT_TYPE_RW = 1,
} element_type_t;

typedef enum {
  DIMENSION_ROW = 0,
  DIMENSION_COL = 1,
} dimension_t;

typedef struct {
  operation_type_t operation_type;
  int rand_data;
  int rand_mask;
  uint8_t data_pattern_idx;
  uint8_t mask_pattern_idx;
  flip_type_t flipped;
} operation_t;

typedef struct {
  uint8_t inner[2];
} packed_operation_t;

typedef struct {
  operation_t** operations;
  uint8_t max_idx;
  direction_t dir;
  uint16_t num_addrs;
} operation_element_t;

typedef struct {
  uint8_t inner[14];
} packed_operation_element_t;

typedef struct {
  uint16_t rand_addr;
} wait_element_t;

typedef struct {
  operation_element_t* operation_element;
  wait_element_t wait_element;
  element_type_t element_type;
} element_t;

typedef struct {
  uint8_t inner[16];
} packed_element_t;

typedef struct {
  uint64_t inner[16];
} packed_element_vec_t;

typedef struct {
  uint128_t patterns[8];
} pattern_table_t;

typedef struct {
  int fail;
  uint64_t fail_cycle;
  uint128_t expected;
  uint128_t received;
  uint128_t signature;
} bist_result_t;

uint128_t create_mask(uint8_t sram_id);
uint128_t create_wmask_mask(uint8_t sram_id, uint128_t wmask);

uint32_t read_at_bit_offset(void* x, int bit_offset, uint8_t num_bits);
void write_at_bit_offset(void* x, int bit_offset, void* val, uint8_t num_bits);

void srambist_operation_init(
    operation_t* op,
    operation_type_t operation_type,
    int rand_data,
    int rand_mask,
    uint32_t data_pattern_idx,
    uint32_t mask_pattern_idx,
    flip_type_t flipped
  );

void srambist_operation_element_init(
  element_t* elem,
  operation_element_t* op_elem,
  operation_t** operations,
  uint32_t max_idx,
  direction_t dir,
  uint32_t num_addrs
);

void srambist_wait_element_init(
  element_t* elem,
  uint32_t rand_addr
);

void pack_operation(packed_operation_t* packed_op, operation_t* op);
void pack_operation_element(packed_operation_element_t* packed_op_elem, operation_element_t* op_elem);
void pack_element(packed_element_t* packed_elem, element_t* elem);
void pack_element_vec(packed_element_vec_t* packed_elem_vec, element_t** elems, uint8_t max_idx);

void srambist_write(uint16_t addr, uint128_t din, uint128_t mask, uint8_t sram_id);
uint128_t srambist_read(uint16_t addr, uint8_t sram_id);

bist_result_t srambist_run_bist(
    uint8_t sram_id,
    uint64_t rand_seed, // TODO: Support all 271 bits.
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    element_t** elems,
    uint8_t max_elem_idx,
    pattern_table_t* pattern_table, 
    uint64_t cycle_limit,
    int stop_on_failure
);

bist_result_t srambist_run_bist_with_packed_elements(
    uint8_t sram_id,
    uint64_t rand_seed, // TODO: Support all 271 bits.
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    packed_element_vec_t* elems,
    uint8_t max_elem_idx,
    pattern_table_t* pattern_table, 
    uint32_t cycle_limit,
    int stop_on_failure
);

void srambist_write_packed_elements(
    packed_element_vec_t* packed_elem_vec,
    pattern_table_t* pattern_table
);

bist_result_t srambist_run_bist_with_existing_patterns(
    uint8_t sram_id,
    uint64_t rand_seed,
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    uint8_t max_elem_idx,
    uint32_t cycle_limit,
    int stop_on_failure
);

#endif
