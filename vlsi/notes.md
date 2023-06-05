# HV DEF routing workflow

- block power connections as shape `BLOCKWIRE`
- porb_h routed as shape `RING`
- all status `ROUTED`

## Exporting

```tcl
select_routes -nets {VDD VSS VDDIO VSSIO} -type special -shapes blockwire -obj_type {via wire}
select_routes -nets porb_h -obj_type {via wire}
select_obj {inst:por inst:iocell_reset/levelShifter}
write_def -selected -routing stac-hv-routing.def
```
