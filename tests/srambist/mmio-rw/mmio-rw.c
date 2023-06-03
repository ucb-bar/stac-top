// See LICENSE for license details.

//**************************************************************************
// SRAM BIST benchmark
//--------------------------------------------------------------------------
//
// This benchmark tests the SRAM BIST peripheral.

#include "mmio.h"
#include "srambist.h"

//--------------------------------------------------------------------------
// Main

int main( int argc, char* argv[] )
{
  return srambist();
}
