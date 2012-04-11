#!/bin/bash

export PYTHONPATH="$(dirname ${BASH_SOURCE[0]})/python-zephyr/"
python -m zephyrserver.xmlrpc "${1:-localhost}" ${2}
