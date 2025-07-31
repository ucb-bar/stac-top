#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

int main() {
    uint128_t wmask = {0x5f, 0};
    uint128_t mask = create_wmask_mask(8, wmask);
    printf("0x%llx%llx\n", mask.hi, mask.lo);
}
