#!/bin/bash

PYTHON=${PYTHON:-python}

export PYTHONPATH="$(dirname ${BASH_SOURCE[0]})/python-zephyr/"
$PYTHON -m zephyrserver.xmlrpc "${1:-localhost}" ${2}
