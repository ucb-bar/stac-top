make redo-par-to-lvs && make redo-lvs \
    && cp build/chipyard.TestHarness.STACConfig-ChipTop/par-rundir/ChipTop.gds ChipTopv0.gds \
    && cp build/chipyard.TestHarness.STACConfig-ChipTop/par-rundir/ChipTop_drc_lvs.gds ChipTopv0_drc_lvs.gds \
    && cp build/chipyard.TestHarness.STACConfig-ChipTop/par-rundir/ChipTop.lvs.v ChipTopv0.lvs.v \
    && cp build/chipyard.TestHarness.STACConfig-ChipTop/lvs-rundir/ChipTop.lvs.sp ChipTopv0.lvs.sp \
    && cp build/chipyard.TestHarness.STACConfig-ChipTop/lvs-rundir/ChipTop.include.sp ChipTopv0.include.sp 
