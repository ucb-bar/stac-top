#!/usr/bin/env python3
from __future__ import annotations


from typing import Callable, TextIO, cast
from dataclasses import dataclass
from itertools import groupby, chain
import sys
import json

import yaml
from pydantic import BaseModel

PinData = str



class IOMap(BaseModel):
    pins: dict[str, PinData]
    extra_insts: dict[str, list[dict[str, str | int | float]]] = {}

class DesignInfoInst(BaseModel):
    name: str

class DesignInfo(BaseModel):
    __root__: list[DesignInfoInst]


cell_locations: dict[str, int | float] = {
    "N1": 381,
    "N2": 638,
    "N3": 895,
    "N4": 1152,
    "N5": 1410,
    "N6": 1667,
    "N7": 1919,
    "N8": 2364,
    "N9": 2621,
    "N10": 2878,
    "N11": 3130,

    "E20": 580,
    "E19": 806,
    "E18": 1031,
    "E17": 1257,
    "E16": 1482,
    "E15": 1707,
    "E14": 1933,
    "E13": 2153,
    "E12": 2374,
    "E11": 2594,
    "E10": 2819,
    "E9": 3045,
    "E8": 3270,
    "E7": 3496,
    "E6": 3721,
    "E5": 3946,
    "E4": 4167,
    "E3": 4392,
    "E2": 4613,
    "E1": 4838,

    "S1": 469,
    "S2": 738,
    "S3": 1012,
    "S4": 1281,
    "S5": 1555,
    "S6": 1829,
    "S7": 2103,
    "S8": 2377,
    "S9": 2651,
    "S10": 2920,
    "S11": 3189,

    "W22": 340,
    "W21": 551,
    # W20 does not exist
    "W19": 908,
    "W18": 1124,
    "W17": 1340,
    "W16": 1556,
    "W15": 1772,
    "W14": 1988,
    "W13": 2204,
    "W12": 2415,
    "W11": 2626,
    "W10": 2842,
    "W9": 3058,
    "W8": 3274,
    "W7": 3490,
    "W6": 3706,
    "W5": 3922,
    "W4": 4138,
    "W3": 4349,
    "W2": 4560,
    "W1": 4771,
}


clamp_pads: dict[str, str] = {
    "N6": "VSSIO",
    "N10": "VSSA",

    "E2": "VCCD",
    "E4": "VDDA",
    "E11": "VDDA",
    "E12": "VSSD",
    "E13": "VSSA",

    "S1": "VSSA",
    "S4": "VSSD",
    "S10": "VSSIO",
    "S11": "VDDA",

    "W2": "VCCD",
    "W3": "VDDIO",
    "W4": "VSSA",
    "W12": "VDDA",
    "W13": "VSSD",
    "W21": "VDDIO",
    "W22": "VCCD",
}

domain_classes: dict[str, str] = {
    "VDDA": "HV",
    "VSSA": "HV",
    "VDDIO": "HV",
    "VSSIO": "HV",
    "VCCD": "LV",
    "VSSD": "LV",
}

clamp_cells: dict[str, str | Callable[[str], str]] = {
    pg_net:
        f"sky130_ef_io__{pg_net.lower()}_{domain_class.lower()}c_clamped_pad"
        if domain_class != "LV" else
        (lambda pg_net, domain_class: lambda side, name: f"sky130_ef_io__{pg_net.lower()}_{domain_class.lower()}c_clamped{'3' if (side in ['left', 'right'] and name not in  ['W22']) else ''}_pad")(pg_net, domain_class)
        # I hate python scoping.
    for pg_net, domain_class in domain_classes.items()
} | {
}


def get_side_for_name(name: str) -> str:
    match name[0]:
        case "E": return "right"
        case "N": return "top"
        case "W": return "left"
        case "S": return "bottom"
        case _:
            raise ValueError(f"'{name}' is not a valid name")

ALL_SIDES = ["top", "right", "bottom", "left"]


@dataclass
class LitStr:
    s: str


IOFileModel = list["IOFileElement"] | dict[str, "IOFileModelInner"]
IOFileModelInner = IOFileModel | dict[str, "IOFileAttr"]

IOFileAttr = str | LitStr | int | float | list["IOFileElement"]

@dataclass
class IOFileElement:
    name: str
    contents: IOFileModelInner

E = IOFileElement

SIDE_ORIENTATIONS = {
    "top": LitStr("R0"),
    "right": LitStr("R270"),
    "bottom": LitStr("R180"),
    "left": LitStr("R90"),
}

skeleton: IOFileModel = {
    "global": {
        "version": 3,
        "io_order": "default",
    },
    "row_margin": {
        side: [E("io_row", {"ring_number": 1, "margin": 0})]
        for side in ALL_SIDES
    },
    "iopad": {
        corner: [
            E("locals", {"ring_number": 1}),
            E("inst", {
                "name": f"corner_{corner}",
                "orientation": LitStr("R" + str(((1 - i) * 90 + 360*2) % 360)),
                "cell": "sky130_ef_io__corner_pad",
          }),
        ]
        for i, corner in enumerate(["topleft", "topright", "bottomright", "bottomleft"])
    },
}


def write_iofile(data: IOFileModel, writer: TextIO, indent_level: int = 0):
    pad = " " * (indent_level * 2)
    def do_elt(elt: IOFileElement):
        writer.write(f"{pad}({elt.name}\n")
        contents = elt.contents
        if isinstance(contents, list):
            write_iofile(contents, writer, indent_level + 1)
        else:
            assert isinstance(contents, dict)
            for k, v in contents.items():
                if isinstance(v, list):
                    writer.write(f"{pad}  ({k}\n")
                    write_iofile(v, writer, indent_level + 2)
                    writer.write(f"{pad}  )\n")
                elif isinstance(v, dict):
                    write_iofile([E(k, v)], writer, indent_level + 1)
                else:
                    if isinstance(v, str):
                        v_str = f'"{v}"'
                    elif isinstance(v, LitStr):
                        v_str = v.s
                    else:
                        v_str = str(v)
                    writer.write(f"{pad}  {k} = {v_str}\n")
        writer.write(f"{pad})\n")
    if isinstance(data, list):
        for elt in data:
            do_elt(elt)
    elif isinstance(data, dict):
        for k, v in data.items():
            do_elt(E(k, v))


def get_inst_path_for_signal(signal_name: str) -> str:
    return f"iocell_{signal_name}/iocell"

def get_inst_path_for_nc(idx: int) -> str:
    return get_inst_path_for_signal(f"nc_{idx}")


def generate_iofile(iomap: IOMap, design_info: DesignInfo | None) -> IOFileModel:
    root = {**skeleton}
    nc_idx = 0
    n_clamps = 0
    n_signals = 0
    cells: dict[str, list[IOFileElement]] = {
        side: [E("locals", {"ring_number": 1})] for side in ALL_SIDES
    }

    has_dupe_signals = False
    for signal, maps_iter in groupby(sorted(iomap.pins.items(), key=lambda x: x[1]), lambda x: x[1]):
        maps = list(maps_iter)
        if len(maps) != 1:
            print(f"Found duplicate signal {signal}: {maps}", file=sys.stderr)
            has_dupe_signals = True
    if has_dupe_signals:
        raise ValueError("Duplicate signals found!")

    for site_name, location in cell_locations.items():
        side = get_side_for_name(site_name)
        def add(attrs: dict[str, IOFileAttr]):
            cells[side].append(E("inst", {
                "orientation": SIDE_ORIENTATIONS[side],
                "offset": location,
            } | attrs))
        if site_name in clamp_pads:
            pg_net = clamp_pads[site_name]
            cell_fn = clamp_cells[pg_net]
            if callable(cell_fn):
                cell = cell_fn(side, site_name)
            elif isinstance(cell_fn, str):
                cell = cell_fn
            else:
                raise ValueError(f"Unexpected cell type {cell_fn}")
            if site_name in iomap.pins:
                raise ValueError(f"Cannot override clamp at {site_name} ({pg_net}, got {iomap.pins[site_name]})")
            add({
                "name": f"clamp_{site_name.lower()}_{pg_net.lower()}",
                "cell": cell,
            })
            n_clamps += 1
        else:
            if site_name in iomap.pins:
                add({"name": get_inst_path_for_signal(iomap.pins[site_name])})
                n_signals += 1
            else:
                add({"name": get_inst_path_for_nc(nc_idx)})
                nc_idx += 1

    n_total = sum(1 for r in cells.values() for x in r if isinstance(x, E) and x.name == "inst")
    assert n_signals + nc_idx + n_clamps == n_total
    assert n_signals == len(iomap.pins)

    if design_info is not None:
        logic_insts = [
            e.contents
            for e in chain(*cells.values())
            if e.name == "inst" and isinstance(e.contents, dict) and "cell" not in e.contents
        ]
        has_dupe_insts = False
        for name, insts_iter in groupby(sorted(logic_insts, key=lambda x: cast(str, x["name"])), lambda x: x["name"]):
            insts = list(insts_iter)
            if len(insts) != 1:
                # this shouldn't happen - it should get caught earlier
                print(f"Found duplicate instance {name}: {insts}", file=sys.stderr)
                has_dupe_insts = True
        if has_dupe_insts:
            raise RuntimeError("Duplicate instances generated")
        logic_insts_set = {cast(str, e["name"]) for e in logic_insts}
        design_insts_set = {get_inst_path_for_signal(e.name) for e in design_info.__root__}
        if logic_insts_set != design_insts_set:
            print(f"Insts in design but not pin-map: {design_insts_set - logic_insts_set}", file=sys.stderr)
            print(f"Insts in pin-map but not design: {logic_insts_set - design_insts_set}", file=sys.stderr)
            raise ValueError("Instances do not match between design and pin config")

    print(f"Generated IO file for {n_signals} signals, {n_clamps} clamps, {nc_idx} NC")
    print(f"  ({n_signals + nc_idx} non-power, {n_total} total)")

    root["iopad"] = {
        **cast(dict[str, IOFileModel], root["iopad"]),
        **cells,
    }
    for side, v in iomap.extra_insts.items():
        entry = root["iopad"][side]
        assert isinstance(entry, list)
        for attrs in v:
            entry.append(E("inst", {
                "orientation": SIDE_ORIENTATIONS[side],
            } | attrs))

    return root


if __name__ == "__main__":
    import argparse
    from pathlib import Path

    parser = argparse.ArgumentParser()
    parser.add_argument("config", type=Path)
    parser.add_argument("--output", "-o", type=Path, required=True)
    parser.add_argument("--design-info", "-d", type=Path)

    args = parser.parse_args()

    with args.config.open("r") as f:
        config = yaml.safe_load(f)

    config = IOMap.parse_obj(config)

    design_info = None
    if args.design_info is not None:
        with args.design_info.open("r") as f:
            design_info = json.load(f)
        design_info = DesignInfo.parse_obj(design_info)

    iof = generate_iofile(config, design_info)
    with args.output.open("w") as f:
        write_iofile(iof, f)
