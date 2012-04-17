#!/usr/bin/env python
# encoding: utf-8
from distutils.core import setup
from zserv import VERSION

setup(name='ZServ',
      version=str(VERSION),
      description='A server for storing, filtering, and sending zephyrs over xmlrpc.',
      author='Steven Allen',
      author_email='steb@mit.edu',
      url='http://zmobile.mit.edu/',
      packages=['zserv'],
      scripts=["bin/zserv", "bin/zserv-bootstrap", "bin/zserv-dtach"],
     )
