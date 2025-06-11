import sys, os
rel_lib_path = os.environ["VERDI_HOME"] + "/share/NPI/python"
sys.path.append(os.path.abspath(rel_lib_path))
from pynpi import npisys
from pynpi import waveform

SYSTEM_PATH = "TestDriver.testHarness.chiptop0.system.uciephy"
PLL_INTERFACE = """
   inout vdd,
   inout vdd_dig,
   inout vss,
   input vclk_ref,
   input vclk_refb,
   input dref_low_0,
   input dref_low_1,
   input dref_low_2,
   input dref_low_3,
   input dref_low_4,
   input dref_low_5,
   input dref_low_6,
   input dref_high_0,
   input dref_high_1,
   input dref_high_2,
   input dref_high_3,
   input dref_high_4,
   input dref_high_5,
   input dref_high_6,
   input vrdac_ref,
   input dcoarse_0,
   input dcoarse_1,
   input dcoarse_2,
   input dcoarse_3,
   input dcoarse_4,
   input dcoarse_5,
   input dcoarse_6,
   input dcoarse_7,
   input dvco_reset,
   input dvco_resetn,
   output vp_out,
   output vn_out,
   output vsar_ref_low,
   output vsar_ref_high,
   output vdig_clk,
   output dfine_0,
   output dfine_1,
   output dfine_2,
   output dfine_3,
   output dfine_4,
   output dfine_5,
   output dfine_6,
   output dfine_7,
   output d_fcw_debug_0,
   output d_fcw_debug_1,
   output d_fcw_debug_2,
   output d_fcw_debug_3,
   output d_fcw_debug_4,
   output d_fcw_debug_5,
   output d_fcw_debug_6,
   output d_fcw_debug_7,
   output d_sar_debug_0,
   output d_sar_debug_1,
   output d_sar_debug_2,
   output d_sar_debug_3,
   output d_sar_debug_4,
   output d_sar_debug_5,
   output d_sar_debug_6,
   output d_sar_debug_7,
   input d_digital_reset,
   input d_kp_0,
   input d_kp_1,
   input d_kp_2,
   input d_kp_3,
   input d_kp_4,
   input d_kp_5,
   input d_kp_6,
   input d_kp_7,
   input d_kp_8,
   input d_kp_9,
   input d_kp_10,
   input d_kp_11,
   input d_kp_12,
   input d_kp_13,
   input d_kp_14,
   input d_kp_15,
   input d_ki_0,
   input d_ki_1,
   input d_ki_2,
   input d_ki_3,
   input d_ki_4,
   input d_ki_5,
   input d_ki_6,
   input d_ki_7,
   input d_ki_8,
   input d_ki_9,
   input d_ki_10,
   input d_ki_11,
   input d_ki_12,
   input d_ki_13,
   input d_ki_14,
   input d_ki_15,
   input d_clol,
   input d_ol_fcw_0,
   input d_ol_fcw_1,
   input d_ol_fcw_2,
   input d_ol_fcw_3,
   input d_ol_fcw_4,
   input d_ol_fcw_5,
   input d_ol_fcw_6,
   input d_ol_fcw_7,
   input d_accumulator_reset_0,
   input d_accumulator_reset_1,
   input d_accumulator_reset_2,
   input d_accumulator_reset_3,
   input d_accumulator_reset_4,
   input d_accumulator_reset_5,
   input d_accumulator_reset_6,
   input d_accumulator_reset_7,
   input d_accumulator_reset_8,
   input d_accumulator_reset_9,
   input d_accumulator_reset_10,
   input d_accumulator_reset_11,
   input d_accumulator_reset_12,
   input d_accumulator_reset_13,
   input d_accumulator_reset_14,
   input d_accumulator_reset_15,
   input d_accumulator_reset_16,
   input d_accumulator_reset_17,
   input d_accumulator_reset_18,
   input d_accumulator_reset_19,
   input d_accumulator_reset_20,
   input d_accumulator_reset_21,
   input d_accumulator_reset_22,
   input d_accumulator_reset_23,
   input d_accumulator_reset_24,
   input d_accumulator_reset_25,
   input d_accumulator_reset_26,
   input d_accumulator_reset_27,
   input d_accumulator_reset_28,
   input d_accumulator_reset_29,
   input d_accumulator_reset_30,
   input d_accumulator_reset_31
"""
TX_LANE_INTERFACE = """
  inout vdd,
  inout vss,
  input dll_reset,
  input dll_resetb,
  input ser_resetb,
  input clkp,
  input clkn,
  input din_0,
  input din_1,
  input din_2,
  input din_3,
  input din_4,
  input din_5,
  input din_6,
  input din_7,
  input din_8,
  input din_9,
  input din_10,
  input din_11,
  input din_12,
  input din_13,
  input din_14,
  input din_15,
  input din_16,
  input din_17,
  input din_18,
  input din_19,
  input din_20,
  input din_21,
  input din_22,
  input din_23,
  input din_24,
  input din_25,
  input din_26,
  input din_27,
  input din_28,
  input din_29,
  input din_30,
  input din_31,
  output dout,
  input pu_ctl_0,
  input pu_ctl_1,
  input pu_ctl_2,
  input pu_ctl_3,
  input pu_ctl_4,
  input pu_ctl_5,
  input pu_ctl_6,
  input pu_ctl_7,
  input pu_ctl_8,
  input pu_ctl_9,
  input pu_ctl_10,
  input pu_ctl_11,
  input pu_ctl_12,
  input pu_ctl_13,
  input pu_ctl_14,
  input pu_ctl_15,
  input pu_ctl_16,
  input pu_ctl_17,
  input pu_ctl_18,
  input pu_ctl_19,
  input pu_ctl_20,
  input pu_ctl_21,
  input pu_ctl_22,
  input pu_ctl_23,
  input pu_ctl_24,
  input pu_ctl_25,
  input pu_ctl_26,
  input pu_ctl_27,
  input pu_ctl_28,
  input pu_ctl_29,
  input pu_ctl_30,
  input pu_ctl_31,
  input pu_ctl_32,
  input pu_ctl_33,
  input pu_ctl_34,
  input pu_ctl_35,
  input pu_ctl_36,
  input pu_ctl_37,
  input pu_ctl_38,
  input pu_ctl_39,
  input pd_ctlb_0,
  input pd_ctlb_1,
  input pd_ctlb_2,
  input pd_ctlb_3,
  input pd_ctlb_4,
  input pd_ctlb_5,
  input pd_ctlb_6,
  input pd_ctlb_7,
  input pd_ctlb_8,
  input pd_ctlb_9,
  input pd_ctlb_10,
  input pd_ctlb_11,
  input pd_ctlb_12,
  input pd_ctlb_13,
  input pd_ctlb_14,
  input pd_ctlb_15,
  input pd_ctlb_16,
  input pd_ctlb_17,
  input pd_ctlb_18,
  input pd_ctlb_19,
  input pd_ctlb_20,
  input pd_ctlb_21,
  input pd_ctlb_22,
  input pd_ctlb_23,
  input pd_ctlb_24,
  input pd_ctlb_25,
  input pd_ctlb_26,
  input pd_ctlb_27,
  input pd_ctlb_28,
  input pd_ctlb_29,
  input pd_ctlb_30,
  input pd_ctlb_31,
  input pd_ctlb_32,
  input pd_ctlb_33,
  input pd_ctlb_34,
  input pd_ctlb_35,
  input pd_ctlb_36,
  input pd_ctlb_37,
  input pd_ctlb_38,
  input pd_ctlb_39,
  input driver_en,
  input driver_en_b,
  input dll_en,
  input ocl,
  input delay_0,
  input delay_1,
  input delay_2,
  input delay_3,
  input delay_4,
  input delayb_0,
  input delayb_1,
  input delayb_2,
  input delayb_3,
  input delayb_4,
  input mux_en_0,
  input mux_en_1,
  input mux_en_2,
  input mux_en_3,
  input mux_en_4,
  input mux_en_5,
  input mux_en_6,
  input mux_en_7,
  input mux_enb_0,
  input mux_enb_1,
  input mux_enb_2,
  input mux_enb_3,
  input mux_enb_4,
  input mux_enb_5,
  input mux_enb_6,
  input mux_enb_7,
  input band_ctrl_0,
  input band_ctrl_1,
  input band_ctrlb_0,
  input band_ctrlb_1,
  input mix_en_0,
  input mix_en_1,
  input mix_en_2,
  input mix_en_3,
  input mix_en_4,
  input mix_en_5,
  input mix_en_6,
  input mix_en_7,
  input mix_en_8,
  input mix_en_9,
  input mix_en_10,
  input mix_en_11,
  input mix_en_12,
  input mix_en_13,
  input mix_en_14,
  input mix_en_15,
  input mix_enb_0,
  input mix_enb_1,
  input mix_enb_2,
  input mix_enb_3,
  input mix_enb_4,
  input mix_enb_5,
  input mix_enb_6,
  input mix_enb_7,
  input mix_enb_8,
  input mix_enb_9,
  input mix_enb_10,
  input mix_enb_11,
  input mix_enb_12,
  input mix_enb_13,
  input mix_enb_14,
  input mix_enb_15,
  input nen_out_0,
  input nen_out_1,
  input nen_out_2,
  input nen_out_3,
  input nen_out_4,
  input nen_outb_0,
  input nen_outb_1,
  input nen_outb_2,
  input nen_outb_3,
  input nen_outb_4,
  input pen_out_0,
  input pen_out_1,
  input pen_out_2,
  input pen_out_3,
  input pen_out_4,
  input pen_outb_0,
  input pen_outb_1,
  input pen_outb_2,
  input pen_outb_3,
  input pen_outb_4,
  output dll_code_0,
  output dll_code_1,
  output dll_code_2,
  output dll_code_3,
  output dll_code_4
"""
TX_DRIVER_INTERFACE = """
   input din,
   output dout,
   input en,
   input en_b,
   input pu_ctl_0,
   input pu_ctl_1,
   input pu_ctl_2,
   input pu_ctl_3,
   input pu_ctl_4,
   input pu_ctl_5,
   input pu_ctl_6,
   input pu_ctl_7,
   input pu_ctl_8,
   input pu_ctl_9,
   input pu_ctl_10,
   input pu_ctl_11,
   input pu_ctl_12,
   input pu_ctl_13,
   input pu_ctl_14,
   input pu_ctl_15,
   input pu_ctl_16,
   input pu_ctl_17,
   input pu_ctl_18,
   input pu_ctl_19,
   input pu_ctl_20,
   input pu_ctl_21,
   input pu_ctl_22,
   input pu_ctl_23,
   input pu_ctl_24,
   input pu_ctl_25,
   input pu_ctl_26,
   input pu_ctl_27,
   input pu_ctl_28,
   input pu_ctl_29,
   input pu_ctl_30,
   input pu_ctl_31,
   input pu_ctl_32,
   input pu_ctl_33,
   input pu_ctl_34,
   input pu_ctl_35,
   input pu_ctl_36,
   input pu_ctl_37,
   input pu_ctl_38,
   input pu_ctl_39,
   input pd_ctlb_0,
   input pd_ctlb_1,
   input pd_ctlb_2,
   input pd_ctlb_3,
   input pd_ctlb_4,
   input pd_ctlb_5,
   input pd_ctlb_6,
   input pd_ctlb_7,
   input pd_ctlb_8,
   input pd_ctlb_9,
   input pd_ctlb_10,
   input pd_ctlb_11,
   input pd_ctlb_12,
   input pd_ctlb_13,
   input pd_ctlb_14,
   input pd_ctlb_15,
   input pd_ctlb_16,
   input pd_ctlb_17,
   input pd_ctlb_18,
   input pd_ctlb_19,
   input pd_ctlb_20,
   input pd_ctlb_21,
   input pd_ctlb_22,
   input pd_ctlb_23,
   input pd_ctlb_24,
   input pd_ctlb_25,
   input pd_ctlb_26,
   input pd_ctlb_27,
   input pd_ctlb_28,
   input pd_ctlb_29,
   input pd_ctlb_30,
   input pd_ctlb_31,
   input pd_ctlb_32,
   input pd_ctlb_33,
   input pd_ctlb_34,
   input pd_ctlb_35,
   input pd_ctlb_36,
   input pd_ctlb_37,
   input pd_ctlb_38,
   input pd_ctlb_39,
   inout vdd,
   inout vss
"""
RX_DATA_LANE_INTERFACE = """
   inout vdd,
   inout vss,
   input din,
   output dout_0,
   output dout_1,
   output dout_2,
   output dout_3,
   output dout_4,
   output dout_5,
   output dout_6,
   output dout_7,
   output dout_8,
   output dout_9,
   output dout_10,
   output dout_11,
   output dout_12,
   output dout_13,
   output dout_14,
   output dout_15,
   output dout_16,
   output dout_17,
   output dout_18,
   output dout_19,
   output dout_20,
   output dout_21,
   output dout_22,
   output dout_23,
   output dout_24,
   output dout_25,
   output dout_26,
   output dout_27,
   output dout_28,
   output dout_29,
   output dout_30,
   output dout_31,
   input clk,
   input rstb,
   input zen,
   input zctl_0,
   input zctl_1,
   input zctl_2,
   input zctl_3,
   input zctl_4,
   input zctl_5,
   input zctl_6,
   input zctl_7,
   input zctl_8,
   input zctl_9,
   input zctl_10,
   input zctl_11,
   input zctl_12,
   input zctl_13,
   input zctl_14,
   input zctl_15,
   input zctl_16,
   input zctl_17,
   input zctl_18,
   input zctl_19,
   input a_en,
   input a_pc,
   input b_en,
   input b_pc,
   input sel_a,
   input vref_sel_0,
   input vref_sel_1,
   input vref_sel_2,
   input vref_sel_3,
   input vref_sel_4,
   input vref_sel_5,
   input vref_sel_6
"""
RX_CLOCK_LANE_INTERFACE = """
   inout vdd,
   inout vss,
   input clkin,
   output clkout,
   input zen,
   input zctl_0,
   input zctl_1,
   input zctl_2,
   input zctl_3,
   input zctl_4,
   input zctl_5,
   input zctl_6,
   input zctl_7,
   input zctl_8,
   input zctl_9,
   input zctl_10,
   input zctl_11,
   input zctl_12,
   input zctl_13,
   input zctl_14,
   input zctl_15,
   input zctl_16,
   input zctl_17,
   input zctl_18,
   input zctl_19,
   input a_en,
   input a_pc,
   input b_en,
   input b_pc,
   input sel_a,
   input vref_sel_0,
   input vref_sel_1,
   input vref_sel_2,
   input vref_sel_3,
   input vref_sel_4,
   input vref_sel_5,
   input vref_sel_6
"""
CLK_MUX_INTERFACE = """
   input in0, in1,
   input mux0_en_0, mux0_en_1,
   input mux1_en_0, mux1_en_1,
   output out, outb
"""
PLL_REG_INFO = [
    [
        {
            "name": "dref_low",
            "width": 7,
        }
    ],
    [
        {
            "name": "dref_high",
            "width": 7,
        }
    ],
    [
        {
            "name": "dcoarse",
            "width": 8,
        }
    ],
    [
        {
            "name": "d_kp",
            "width": 16,
        }
    ],
    [
        {
            "name": "d_ki",
            "width": 16,
        }
    ],
    [
        {
            "name": "d_clol",
            "width": None,
        }
    ],
    [
        {
            "name": "d_ol_fcw",
            "width": 8,
        }
    ],
    [
        {
            "name": "d_accumulator_reset",
            "width": 32,
        }
    ],
    [
        {
            "name": "dvco_reset",
            "width": None,
        },
        {
            "name": "dvco_resetn",
            "width": None,
        }
    ],
    [
        {
            "name": "d_digital_reset",
            "width": None,
        }
    ]
]
TX_DATA_REG_INFO = {
}

def get_inputs(interface):
    inputs = []
    for line in interface.split("\n"):
        line = line.strip()
        if line.startswith("input"):
            for input in line[6:].rstrip(",").split(","):
                inputs.append(input.strip())
    return inputs

def assert_path_transition(fileHandle, reset_off_time, path):
    signal = fileHandle.sig_by_name(path)
    vct = signal.create_vct()
    ret = vct.goto_time(reset_off_time)
    prev = None
    transition = None
    while ret:
        val = vct.value(waveform.VctFormat_e.DecStrVal)
        if prev is not None and prev in "01" and val in "01" and prev != val:
            transition = vct.time()
            break
        prev = val
        ret = vct.goto_next()
    if transition is None:
        print(path, "does not transition!")
    vct.release()
    vct = None

def get_names(signal_info):
    signal_name = signal_info["name"]
    if signal_info["width"] is None:
        names = [signal_name]
    else:
        names = [f"{signal_name}_{i}" for i in range(signal_info["width"])]
    return names

def find_reset_off_time(reset_path):
    reset = fileHandle.sig_by_name(reset_path)
    vct = reset.create_vct()
    ret = vct.goto_first()
    prev = None
    reset_off_time = None
    while ret:
        val = vct.value(waveform.VctFormat_e.DecStrVal)
        if prev == "1" and val == "0":
            reset_off_time = vct.time()
        prev = val
        ret = vct.goto_next()
    return reset_off_time

npisys.init(sys.argv)
fileName = os.path.join(
  os.path.dirname(os.path.abspath(__file__)),
  "../sims/vcs/output/chipyard.harness.TestHarness.KodiakSimChipConfig/ucie-mmio.fsdb"
)
fileHandle = waveform.open(fileName)

reset_off_time = find_reset_off_time(f"{SYSTEM_PATH}.auto_clock_in_reset")
for pll in ["pll", "testPll"]:
    for reg in PLL_REG_INFO:
        for signal in reg:
            for name in get_names(signal):
                # TODO: Check if order of transitions corresponds with register order.
                pass

    # Checks that all inputs transition.
    for name in get_inputs(PLL_INTERFACE):
        if name in ["vrdac_ref"]:
            continue
        signal_path = f"{SYSTEM_PATH}.phy.{pll}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
for txlane in [f"txdata{i}" for i in range(16)] + ["txclkp", "txclkn", "txvalid", "txtrack", "txLoopbackLane"]:
    # Checks that all inputs transition.
    for name in get_inputs(TX_LANE_INTERFACE):
        if name.startswith("din") or name in ["ser_resetb"]:
            continue
        signal_path = f"{SYSTEM_PATH}.phy.{txlane}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
for rxlane in [f"rxdata{i}" for i in range(16)] + ["rxvalid", "rxtrack", "rxLoopbackLane"]:
    # Checks that all inputs transition.
    for name in get_inputs(RX_DATA_LANE_INTERFACE):
        if name in ["din", "clk", "rstb"]:
            continue
        signal_path = f"{SYSTEM_PATH}.phy.{rxlane}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
for rxlane in ["rxClkP", "rxClkN"]:
    # Checks that all inputs transition.
    for name in get_inputs(RX_CLOCK_LANE_INTERFACE):
        if name in ["clkin"]:
            continue
        signal_path = f"{SYSTEM_PATH}.phy.{rxlane}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
for clkmux in ["clkMuxP", "clkMuxN"]:
    # Checks that all inputs transition.
    for name in get_inputs(CLK_MUX_INTERFACE):
        if name in ["mux1_en_0", "mux1_en_1", "in0"]:
            continue
        signal_path = f"{SYSTEM_PATH}.phy.{clkmux}.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)

for txdriver in ["testPllClkPDriver", "testPllClkNDriver", "pllClkPDriver", "pllClkNDriver", "rxClkDriver", "rxClkDivDriver"]:
    # Checks that all inputs transition.
    for name in get_inputs(TX_DRIVER_INTERFACE):
        if name in ["din"]:
            continue
        signal_path = f"{SYSTEM_PATH}.common.{txdriver}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
for txlane in ["txLane"]:
    # Checks that all inputs transition.
    for name in get_inputs(TX_LANE_INTERFACE):
        if name.startswith("din") or name in ["ser_resetb"]:
            continue
        signal_path = f"{SYSTEM_PATH}.common.{txlane}.verilogBlackBox.{name}"
        assert_path_transition(fileHandle, reset_off_time, signal_path)
    assert_path_transition(fileHandle, reset_off_time, f"{SYSTEM_PATH}.common.{txlane}.verilogBlackBox.dout")
print(flush = True)
fileHandle.unload_vc()

waveform.close(fileHandle)
npisys.end()
