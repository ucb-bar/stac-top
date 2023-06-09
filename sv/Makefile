
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

ifeq ($(VERILATOR_ROOT),)
VERILATOR = verilator
else
export VERILATOR_ROOT
VERILATOR = $(VERILATOR_ROOT)/bin/verilator
endif

SVDIR = src
SVSRC = $(wildcard $(SVDIR)/*.sv)
TBDIR = tb
TBSRC = $(wildcard $(TBDIR)/*.sv)
TOP = top
VERILATOR_FLAGS = --binary -j 0 --trace --assert -sv -I$(SVDIR) -I$(TBDIR)
VERIBLE_VERILOG_FORMAT_FLAGS = --inplace

clean:
	rm -rf obj_dir
	rm -f verible.filelist

%: tb/%.sv $(SVSRC)
	@echo "-- Running $@"
	@echo "-- Building"
	$(VERILATOR) $(VERILATOR_FLAGS) --top-module $@ $^ 
	@echo "-- Simulating"
	cd obj_dir && ./V$@ && cd -
	@echo "-- Done"

lint: $(SVSRC)
	verible-verilog-lint $^ $(TBSRC)
	verilator --lint-only $^

format: $(SVSRC) $(TBSRC)
	verible-verilog-format $(VERIBLE_VERILOG_FORMAT_FLAGS) $^

verible.filelist: $(SVSRC) $(TBSRC)
	echo "$^" | tr ' ' '\n' > $@

.PHONY: clean lint format
