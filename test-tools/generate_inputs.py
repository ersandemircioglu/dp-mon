#!/usr/bin/env python3
# encoding: utf-8
'''
generate_inputs -- Generates input files for DPF 

generate_inputs is a description

It defines classes_and_methods

@author:     Ersan Demircioglu

@copyright:  2025 Ersan Demircioglu. All rights reserved.

@license:    Apache License 2.0

'''

import sys
import os

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
from duplicity.tempdir import default
import json
import datetime
import time
import random
import requests
import dataclasses
from dataclasses import dataclass

__all__ = []
__version__ = 0.1
__date__ = '2025-05-01'
__updated__ = '2025-05-01'

_arg_verbose = bool(False)

global_simulation_time = datetime.datetime.now().replace(microsecond=0)
satellites = dict()
archive = dict()


@dataclass
class ProductSummary:
    processing_time: str
    name: str
    filename: str
    isProduct: bool = False
    quality: float = 0
    # inputs: list = None 


class CLIError(Exception):
    '''Generic exception to raise and log different fatal errors.'''

    def __init__(self, msg):
        super(CLIError).__init__(type(self))
        self.msg = "E: %s" % msg

    def __str__(self):
        return self.msg

    def __unicode__(self):
        return self.msg

    
class Satellite:

    def __init__(self, satellite_conf, simulation_start_time):
        self.name = satellite_conf["name"]
        self.orbit = satellite_conf["initial_orbit"]
        self.orbit_duration = satellite_conf["orbit_duration"]
        self.orbit_start_time = simulation_start_time 
            
    def propagate(self, current_simulation_time):
        if self.orbit_start_time + datetime.timedelta(minutes=self.orbit_duration) < current_simulation_time:
            self.orbit_start_time = current_simulation_time
            self.orbit += 1
            log("FlightDynamics", "INFO", f"New Orbit for {self.name}: {self.orbit}")

    def getOrbitNumber(self):
        return self.orbit
        
        
class SatDataTemplate:

    def __init__(self, template_conf, simulation_start_time):
        self.name = template_conf["name"]
        self.filename_template = template_conf["filename_template"]
        self.variables = template_conf["variables"]
        self.frequency = template_conf["frequency"]
        self.first_pass = template_conf["first_pass"]
        self.pass_duration = template_conf["pass_duration"]
        self.segment_duration = template_conf["segment_duration"]
        self.generate_end_file = template_conf["generate_end_file"]
        self.pass_start = simulation_start_time + datetime.timedelta(minutes=self.first_pass)
        self.pass_end = self.pass_start + datetime.timedelta(minutes=self.pass_duration)
        self.last_generation_time = self.pass_start
        self.error_rate = template_conf["error_rate"]
        
    def generate(self, current_simulation_time):
        if self.__is_in_pass(current_simulation_time) \
            and self.last_generation_time + datetime.timedelta(minutes=self.segment_duration) <= current_simulation_time:
            return self.__generate_file(current_simulation_time, False)

    def __generate_file(self, current_simulation_time, is_end_file):
        global archive
        metadata = {}
        for key, value in self.variables.items():
            metadata[key] = eval(value)
        filename = self.filename_template.format(**metadata)
        self.last_generation_time = current_simulation_time
        if is_end_file:
            filename = filename + "_END"
        log("RawDataHandler", "INFO", f"{filename} has been downloaded")
        metadata["filename"] = filename
        metadata["summary"] = ProductSummary(current_simulation_time.isoformat(), self.name, filename)
        archive[self.name] = metadata
        return dataclasses.asdict(metadata["summary"])
        # log("RawDataHandler", "INFO", f"{filename} has been archived")
            
    def __is_in_pass(self, current_simulation_time):
        if self.pass_start <= current_simulation_time and current_simulation_time <= self.pass_end:
            return True
        elif self.pass_end < current_simulation_time:  # pass is ended, new pass must be calculated
            self.pass_start = current_simulation_time + datetime.timedelta(minutes=self.frequency)
            self.pass_end = self.pass_start + datetime.timedelta(minutes=self.pass_duration)
            self.last_generation_time = self.pass_start
            if self.generate_end_file:
                self.__generate_file(current_simulation_time, True)
            log("RawDataHandler", "INFO", f"Next Pass for {self.name}: {self.pass_start}-{self.pass_end}")
        return False


class AuxDataTemplate:

    def __init__(self, template_conf, simulation_start_time):
        self.name = template_conf["name"]
        self.filename_template = template_conf["filename_template"]
        self.variables = template_conf["variables"]
        self.frequency = template_conf["frequency"]
        self.first_generation = template_conf["first_generation"]
        self.next_generation_time = simulation_start_time + datetime.timedelta(minutes=self.first_generation)
        self.error_rate = template_conf["error_rate"]
        
    def generate(self, current_simulation_time):
        global archive
        if self.next_generation_time <= current_simulation_time:
            metadata = {}
            for key, value in self.variables.items():
                metadata[key] = eval(value)
            filename = self.filename_template.format(**metadata)
            self.next_generation_time = current_simulation_time + datetime.timedelta(minutes=self.frequency)
            log("AuxDataHandler", "INFO", f"{filename} has been received")
            
            metadata["filename"] = filename
            metadata["summary"] = ProductSummary(current_simulation_time.isoformat(), self.name, filename)
            if not check_error(self.error_rate):
                archive[self.name] = metadata
                return dataclasses.asdict(metadata["summary"])
            else:
                return None
            # log("AuxDataHandler", "INFO", f"{filename} has been archived")


class ProdTemplate:

    def __init__(self, template_conf, simulation_start_time):
        self.name = template_conf["name"]
        self.filename_template = template_conf["filename_template"]
        self.variables = template_conf["variables"]
        self.trigger_file = template_conf["trigger_file"]
        self.input_files = template_conf["input_files"]
        self.processing_duration = template_conf["processing_duration"]
        self.next_processing_duration = self.processing_duration
        self.timeliness = template_conf["timeliness"]
        self.error_rate = template_conf["error_rate"]
        self.quality_estimation = template_conf["quality_estimation"]
        
    def generate(self, current_simulation_time):
        global archive
        if self.trigger_file in archive:
            trigger_variables = archive[self.trigger_file]
            if trigger_variables["sensing_stop"] + datetime.timedelta(minutes=self.next_processing_duration) <= current_simulation_time:
                inputs = {}
                inputs_summary = []
                inputs[self.trigger_file] = archive[self.trigger_file]
                inputs_summary.append(archive[self.trigger_file]["summary"])
                for key in self.input_files:
                    inputs[key] = archive[key]
                    inputs_summary.append(archive[key]["summary"])
                metadata = {}
                for key, value in self.variables.items():
                    metadata[key] = eval(value)
                filename = self.filename_template.format(**metadata)
                log(f"Processor.{self.name}", "INFO", f"{filename} has been generated")
                
                metadata["filename"] = filename
                
                quality = self.__calculate_quality(inputs, metadata)
                log(f"Processor.{self.name}", "INFO", f"{filename} quality is {quality}")
                
                metadata["summary"] = ProductSummary(current_simulation_time.isoformat(), self.name, filename, True, quality)
                
                archive[self.name] = metadata
                archive.pop(self.trigger_file, None)
                if not check_error(self.error_rate):
                    self.next_processing_duration = int(random.random() * self.timeliness)
                else:
                    self.next_processing_duration = int(random.random() * self.timeliness * 10)
                # log(f"Processor.{self.name}", "INFO", f"{filename} has been archived")
                # return dataclasses.asdict(ProductSummary(current_simulation_time.isoformat(), self.name, filename, True, quality, inputs_summary))
                return dataclasses.asdict(metadata["summary"])
                
    def __calculate_quality(self, inputs, metadata):
        total = 0
        num_of_item = 0.0
        for exp in self.quality_estimation:
            v = eval(exp)
            # print(v)
            total += v
            num_of_item += 1.0
        return num_of_item / total

    
def check_error(error_rate):
    r = random.random()
    # print(r)
    return (r * 100) < error_rate

    
def to_minutes(td):
    return td.total_seconds() / 60

    
def log(process, level, message):
    if _arg_verbose:
        print(f'{simulation_time().isoformat()} {process} {level} {message}')

    
def orbit(sat):
    satellite = satellites[sat]
    return satellite.getOrbitNumber()


def simulation_time():
    return global_simulation_time


def parse_conf(conf_path):
    conf_file = open(conf_path)
    conf = json.load(conf_file)
    conf_file.close()
    return conf


def injectHistoricData(data):
    print_json(data)
    data_json = json.dumps(data)
    headers = {'Content-type': 'application/json'}
    response = requests.post("http://localhost:8080/inject", data=data_json, headers=headers)
    print_json(response.json())

    
def appendData(data):
    print_json(data)
    data_json = json.dumps(data)
    headers = {'Content-type': 'application/json'}
    response = requests.patch("http://localhost:8080/append", data=data_json, headers=headers)
    print_json(response.json())


def print_json(data):
    print(
        json.dumps(
            data,
            indent=2
        )
    )

    
def start(args):
    global global_simulation_time
    global satellites
    global archive
    global _arg_verbose
    
    _arg_verbose = args.verbose
    
    global_simulation_time = datetime.datetime.now().replace(microsecond=0) - datetime.timedelta(minutes=(7 * 24 * 60))
    conf = parse_conf(args.config)
    
    for satellite_conf in conf["satellites"]:
        satellite = Satellite(satellite_conf, global_simulation_time)
        satellites[satellite.name] = satellite
    
    sat_data_template_list = []
    for template_conf in conf["sat_data_templates"]:
        sat_data_template_list.append(SatDataTemplate(template_conf, global_simulation_time))
    
    aux_data_template_list = []
    for template_conf in conf["aux_data_templates"]:
        aux_data_template_list.append(AuxDataTemplate(template_conf, global_simulation_time))
        
    prod_template_list = []
    for template_conf in conf["prod_templates"]:
        prod_template_list.append(ProdTemplate(template_conf, global_simulation_time))
        
    historic_data = []
    
    for time_tick in range(30 * 24 * 60):
        # print(f"Simulation time : {global_simulation_time}")
        for satellite in satellites.values():
            satellite.propagate(global_simulation_time)
  
        for sat_data_template in sat_data_template_list:
            prodSum = sat_data_template.generate(global_simulation_time)
            if prodSum != None:
                historic_data.append(prodSum)
        
        for aux_data_template in aux_data_template_list:
            prodSum = aux_data_template.generate(global_simulation_time)
            if prodSum != None:
                historic_data.append(prodSum)
            
        for prod_template in prod_template_list:
            prodSum = prod_template.generate(global_simulation_time)
            if prodSum != None:
                historic_data.append(prodSum)
        
        # print(archive)
        global_simulation_time = global_simulation_time + datetime.timedelta(minutes=1)
        
    injectHistoricData(historic_data)
    
    input("Press Enter to continue...")
    
    for time_tick in range(7 * 60 * 24):
        # print(f"Simulation time : {global_simulation_time}")
        for satellite in satellites.values():
            satellite.propagate(global_simulation_time)
  
        for sat_data_template in sat_data_template_list:
            prodSum = sat_data_template.generate(global_simulation_time)
            if prodSum != None:
                appendData(prodSum)
        
        for aux_data_template in aux_data_template_list:
            prodSum = aux_data_template.generate(global_simulation_time)
            if prodSum != None:
                appendData(prodSum)
            
        for prod_template in prod_template_list:
            prodSum = prod_template.generate(global_simulation_time)
            if prodSum != None:
                appendData(prodSum)
        
        # print(archive)
        global_simulation_time = global_simulation_time + datetime.timedelta(minutes=1)
        time.sleep(0.01)

    
def main(argv=None):  # IGNORE:C0111
    '''Command line options.'''

    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)

    program_name = os.path.basename(sys.argv[0])
    program_version = "v%s" % __version__
    program_build_date = str(__updated__)
    program_version_message = '%%(prog)s %s (%s)' % (program_version, program_build_date)
    program_shortdesc = __import__('__main__').__doc__.split("\n")[1]
    program_license = '''%s

  Created by Ersan Demircioglu on %s.
  Copyright 2025. All rights reserved.

  Licensed under the Apache License 2.0
  http://www.apache.org/licenses/LICENSE-2.0

  Distributed on an "AS IS" basis without warranties
  or conditions of any kind, either express or implied.

USAGE
''' % (program_shortdesc, str(__date__))

    try:
        # Setup argument parser
        parser = ArgumentParser(description=program_license, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument("-v", "--verbose", dest="verbose", action='store_true', help="set verbosity level")
        parser.add_argument("-c", "--config", dest="config", help="Configuration JSON file. [default: %(default)s]")
        parser.add_argument('-V', '--version', action='version', version=program_version_message)

        # Process arguments
        args = parser.parse_args()
        start(args)
        return 0
    except KeyboardInterrupt:
        ### handle keyboard interrupt ###
        return 0
    except Exception as e:
        indent = len(program_name) * " "
        sys.stderr.write(program_name + ": " + repr(e) + "\n")
        sys.stderr.write(indent + "  for help use --help")
        return 2


if __name__ == "__main__":
    sys.exit(main())
    
