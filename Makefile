# Pure-JDK build for the Gun Mayhem demo (Swing / Java2D + TCP sockets).
# No external frameworks or dependencies — just javac + java.

SRC_DIR = src
OUT_DIR = out
MAIN    = gunmayhem.Main
SOURCES = $(shell find $(SRC_DIR) -name '*.java')

# Host/port for `make run-client` (override on the LAN, e.g. HOST=192.168.1.20).
HOST ?= localhost
PORT ?= 5010

.PHONY: build run run-server run-client clean

build:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(SOURCES)

# Local hot-seat: two players share one keyboard.
run: build
	java -cp $(OUT_DIR) $(MAIN)

# Authoritative server. Prints the LAN address clients should connect to.
run-server: build
	java -cp $(OUT_DIR) $(MAIN) server $(PORT)

# One networked client window. Run twice (two terminals) to get two players.
run-client: build
	java -cp $(OUT_DIR) $(MAIN) client $(HOST) $(PORT)

clean:
	rm -rf $(OUT_DIR)
