PYTHON=${PYTHON:-python}
export PYTHONUSERBASE=${ZMOBILE_PREFIX:-/mit/zmobile}
export PATH=/mit/zmobile/bin:$PATH

cd "$(dirname ${BASH_SOURCE[0]})"

(
    $PYTHON setup.py install --user --optimize=1
)

(
    cd python-zephyr
    $PYTHON setup.py install --user --optimize=1
)

(
    easy_install -U -O1 --user argparse
)

# A saner pam module that has been updated within the last 10 years
(
    easy_install --user http://atlee.ca/software/pam/dist/0.1.3/pam-0.1.3.tar.gz
)
