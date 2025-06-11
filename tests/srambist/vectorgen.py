import random
SRAMS = [
    (8, 64, 24),
    (8, 64, 32),
    (8, 128, 16),
    (8, 128, 24),
    (8, 128, 32),
    (1, 256, 8),
    (8, 256, 16),
    (8, 256, 32),
    (8, 256, 64),
    (8, 256, 128),
    (1, 512, 8),
    (8, 512, 32),
    (8, 512, 64),
    (8, 512, 128),
    (1, 1024, 8),
    (8, 1024, 32),
    (8, 1024, 64),
    (1, 2048, 8),
    (8, 2048, 32),
    (1, 4096, 8),
    (8, 4096, 32),
    (8, 8192, 32),
];

def rand_uint128_t():
    x = random.getrandbits(128)
    return f"{{{(x >> 64):#018x}, {(x & 0xffffffffffffffff):#018x}}}"
def rand_array(n):
    elems = [f"    {rand_uint128_t()}" for _ in range(n)]
    return ",\n".join(elems)

for wmask_granularity, num_words, data_width in SRAMS:
    print(f"const uint128_t DATA_{num_words}X{data_width}W{wmask_granularity}[{num_words}] = {{")
    print(rand_array(num_words));
    print("};");
    print();
    print(f"const uint128_t WMASK_{num_words}X{data_width}W{wmask_granularity}[{num_words}] = {{")
    print(rand_array(num_words));
    print("};");
print();
print("const sram_test_t SRAM_TESTS[NUM_SRAMS] = {")
elems = [f"    {{DATA_{num_words}X{data_width}W{wmask_granularity}, WMASK_{num_words}X{data_width}W{wmask_granularity}}}" for wmask_granularity, num_words, data_width in SRAMS]
print(",\n".join(elems))
print("};");


