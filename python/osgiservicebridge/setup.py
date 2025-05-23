'''
Created on Jul 10, 2017

@author: slewis
'''
from setuptools import setup, find_packages
# To use a consistent encoding
from codecs import open
from os import path

here = path.abspath(path.dirname(__file__))

import os

VERSION_PATH = os.path.join("src", "osgiservicebridge", "version.py")
# For Python 3 compatibility, we can't use execfile; this is 2to3's conversion:
exec(compile(open(VERSION_PATH).read(),
     VERSION_PATH, "exec"))
VERSION = __version__  # noqa

setup(
    name='osgiservicebridge',
    packages=find_packages(where='src'),
    package_dir={"": "src"},

    # Versions should comply with PEP440.  For a discussion on single-sourcing
    # the version across setup.py and the project code, see
    # https://packaging.python.org/en/latest/single_source_version.html
    version=VERSION,

    description='OSGi services implemented in Python',
    long_description='osgiservicebridge provides the Python library '
                     'for OSGi R8 remote services between Java '
                     'and Python.  See the github project '
                     'at https://github.com/ECF/Py4j-RemoteServicesProvider '
                     'for detailed information about OSGi remote services '
                     'and for examples',

    # The project's main homepage.
    url='https://github.com/ECF/Py4j-RemoteServicesProvider',

    # Author details
    author='Scott Lewis',
    author_email='scottslewis@gmail.com',

    # Choose your license
    license='Apache Software License',

    # See https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[
        # How mature is this project? Common values are
        #   3 - Alpha
        #   4 - Beta
        #   5 - Production/Stable
        'Development Status :: 5 - Production/Stable',

        # Indicate who your project is intended for
        'Intended Audience :: Developers',
        'Topic :: Software Development :: Build Tools',

        # Pick your license as you wish (should match "license" above)
        'License :: OSI Approved :: Apache Software License',

        # Specify the Python versions you support here. In particular, ensure
        'Programming Language :: Python :: 3.10',
        'Programming Language :: Python :: 3.11',
        'Programming Language :: Python :: 3.12',
        'Programming Language :: Python :: 3.13',
       "Programming Language :: Java",
        "Topic :: Software Development :: Libraries",
        "Topic :: Software Development :: Object Brokering",
    ],

    # What does your project relate to?
    keywords='Java Python OSGi development',

    # You can just specify the packages manually here if your project is
    # simple. Or you can use find_packages().
    # packages=find_packages(exclude=['contrib', 'docs', 'tests']),

    # Alternatively, if you want to distribute just a my_module.py, uncomment
    # this:
    #   py_modules=["my_module"],

    # List run-time dependencies here.  These will be installed by pip when
    # your project is installed. For an analysis of "install_requires" vs pip's
    # requirements files see:
    # https://packaging.python.org/en/latest/requirements.html
    install_requires=['py4j>=0.10.9.7', 'protobuf>=5.29.4'],

    # List additional groups of dependencies here (e.g. development
    # dependencies). You can install these using the following syntax,
    # for example:
    # $ pip install -e .[dev,test]
    # extras_require={
    #    'dev': ['check-manifest'],
    #    'test': ['coverage'],
    # },

    # If there are data files included in your packages that need to be
    # installed, specify them here.  If using Python 2.6 or less, then these
    # have to be included in MANIFEST.in as well.
    # package_data={
    #    'sample': ['package_data.dat'],
    # },

    # Although 'package_data' is the preferred approach, in some case you may
    # need to place data files outside of your packages. See:
    # http://docs.python.org/3.4/distutils/setupscript.html#installing-additional-files # noqa
    # In this case, 'data_file' will be installed into '<sys.prefix>/my_data'
    # data_files=[('java', [JAVA_FILE])],

    # To provide executable scripts, use entry points in preference to the
    # "scripts" keyword. Entry points provide cross-platform support and allow
    # pip to create the appropriate form of executable for the target platform.
    # entry_points={
    #    'console_scripts': [
    #        'sample=sample:main',
    #    ],
    # },
)
