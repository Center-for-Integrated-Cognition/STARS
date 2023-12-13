Installation directions for ITL agent (extended with STARS), Soar, and supporting software. These instructions are targeted toward Ubuntu Linux.
Also contains instruction for running the experiments and pointers to relevant code.

#### Python

You'll need to have Python installed with TK support built-in.

* Ubuntu: `apt-get install python3-tk`
* Mac: `brew install python python-tk`

Additionally, you need to install the required packages:

    pip3 -r python/requirements.txt

#### Java

The simplest way to manage Java and Java tool versions on your machine is to install [sdkman](https://sdkman.io/). Once installed, you should install Java (recommend 17-temurin) and ant. For example:

```shell
sdk install java 17.0.7-tem
sdk install ant
```

You can list available versions of software with `sdk list <java|ant|gradle|maven...>`.

#### Soar
First need to have Soar installed. Note that as of Soar 9.6.1, the SoarSuite distribution comes with python 3.10 support, though the SoarTutorial distribution does not. The library cannot be used with 3.11 or 3.9, etc. (3.10.11 is fine). If you want to use a different version of Python, you will need to build from source.

##### Pre-built Binaries

You can download a copy from [here](https://soar.eecs.umich.edu/Downloads). Extract it to the desired location, then set up the following environment variables:

```shell
# (Make sure that SOAR_HOME is set to the bin directory)
export SOAR_HOME=$HOME/path/to/soar/bin
export PYTHONPATH=$PYTHONPATH:$SOAR_HOME
# on Mac would be DYLD_LIBRARY_PATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SOAR_HOME
export CLASSPATH=$CLASSPATH:$SOAR_HOME/java/sml.jar:$SOAR_HOME/SoarJavaDebugger.jar
```

##### Compiling from Source

Clone the Soar repository and follow the instructions for installing pre-requisites. You'll need at least Python 3, Java 17, swig, and C/C++ build tools installed.

```
git clone https://github.com/soargroup/soar
cd soar
python3 scons/scons.py all
```

Set the same environment variables as above, except that `SOAR_HOME` will be set to `soar/out`.

#### PySoarlib

If you want to use the python interface, you will need the [pysoarlib library](https://github.com/amininger/pysoarlib).

You can clone `https://github.com/amininger/pysoarlib` and add the parent directory to the `$PYTHONPATH`

To install with pip:
Because the main branch of PySoarLib is not currently pip-installable, you'll have to install from the PR branch:

    pip3 install git+https://github.com/amininger/pysoarlib.git@refs/pull/7/head

Note that it requires Soar to be built with the `sml_python` target against your (major) version of python 3 and `SOAR_HOME` to be in the `PYTHONPATH`.

#### ITL Agent

**Environment Variables:**

If you source the `source-rosie-env.sh` file in your bash profile, the following variables will be added to your environment: `ROSIE_HOME`, `ROSIE_AGENT`, `ROSIE_TESTS`. Your `CLASSPATH` and `PYTHONPATH` will also be updated, and some useful commands and command completions will be added to your terminal. These environment variables will be added automatically. 

```
export CLASSPATH=$CLASSPATH:$ROSIE_HOME/java/rosie.jar
export CLASSPATH=$CLASSPATH:$ROSIE_HOME/tools/java/rosie-tools.jar
export CLASSPATH=$CLASSPATH:$ROSIE_HOME/tools/antlr-4.5-complete.jar
export PYTHONPATH=$PYTHONPATH:$ROSIE_HOME/python
```

This directory contains a .env file as well, which will load these automatically when you `cd` into the directory if you have [autoenv](https://github.com/hyperupcall/autoenv) or a similar tool installed.

**Building the agent:**

Make sure you have `source-rosie-env.sh` (see above) sourced, then run `build_rosie` or `build_rosie_tools` (from any working directory). As long as you have the environment variables set, you can also simply run `ant` in either `java` or `tools/java`.

#### GPT
You will need to add a APIKey.py file to `python/rosie/lm/` with your key in order to access GPT/OPENAI.
The format of the file should be:
class APIKey:
    OPENAI_API_KEY =

### To run the experiments you must install MobileSim first (see README in mobile-sim)

### Running the experiments

cd to ITL-agent/examples/tidy-kitchen for the tidy kitchen task. The other tasks are location in organize-office and store-groceries (commands below are the same).

To run the STARS condition simple run the script: sh run-STARS.sh.
There are similar scripts for the other conditions (STAR,ST,STS, etc.)
For the non oversight conditions, the simple user interaction required is scripted, for oversight enter text into the chat window. The buttons on the right contain the text relevant to teaching the task.

You should see the Agent interaction window and the mobile simulator window appear. Simply hit run to start.
Once completed (signified by the message I'm ready for a new task) simply exit the window (relevant data from the run will automatically be saved).

To post process the data run sh post-process-data.sh STARS (using the appropriate condition name that was just run).
This will process and copy the data into data/STARS/.

Data from the paper for each task experiment and condition can be seen in paper-data/


Look to rosie.config for other desired configuration changes, such as turning on the Soar debugger.

# Code relevant to STARS (scientific contributions)
The code relevant to STARS mostly resides in two locations.
1. ITL-agent/python/rosie/lm/ (python code)
Which contains the code for interacting with the language model, constructing prompts, and returning responses.

2. ITL-agent/agent/satisfy-purpose/dialog-event/language-model (soar code)
Which contains the cogntive agent code for controlling the processing of STARS, performing the agent analysis of responses, and directing repair and selection.
