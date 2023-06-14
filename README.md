# SRAM22 Timing Analysis Chip - Chipyard repo

Toplevel for the STAC tapeout, based on [Chipyard](https://github.com/ucb-bar/chipyard)

[Chip docs](docs/test_chip.md)

First-time setup:
- Source Conda install / base environment
- `bsub -Is scripts/build-setup.sh -s 4 -s 6 -s 7 -s 8 -s 9 -f`
- `source ./env.sh`
- `scripts/init-vlsi.sh sky130` (add `openroad` if you don't have access to hammer-mentor-plugins)
