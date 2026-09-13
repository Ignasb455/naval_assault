# Pure-JDK build for the Gun Mayhem demo (Swing / Java2D + TCP sockets).
# No external frameworks or dependencies — just javac + java.

SRC_DIR = src
OUT_DIR = out
MAIN    = gunmayhem.Main
SOURCES = $(shell find $(SRC_DIR) -name '*.java')

# Host/port/nickname for `make run-client` (override on the LAN, e.g. HOST=192.168.1.20).
HOST ?= localhost
PORT ?= 5010
NICK ?=

.PHONY: build run run-server run-client clean

build:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(SOURCES)

# A client that asks for the server address and your nickname on a join screen.
run: build
	java -cp $(OUT_DIR) $(MAIN)

# Authoritative server. Prints the LAN address clients should connect to.
run-server: build
	java -cp $(OUT_DIR) $(MAIN) server $(PORT)

# One client window. Pass NICK=<name> to skip the join screen.
run-client: build
	java -cp $(OUT_DIR) $(MAIN) client $(HOST) $(PORT) $(NICK)

clean:
	rm -rf $(OUT_DIR)
