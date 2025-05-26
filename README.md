# Monitoring System for Data Processing Frameworks (DP-MON)

DP-MON is a monitoring system for payload data processing frameworks. The purpose of the DP-MON is to define a monitoring system agnostic to the monitored system, yet capable of extracting valuable information from log messages in order to identify processing errors that have already occurred or may occur in the near future. The proposed monitoring system designed to address following challenges.  

- **Expert knowledge to interpret Monitoring/Observability data**: COTS Monitoring applications primarily provide metrics for resource utilisation, and this valuable data is mostly used to identify the time period during which the error occurred. After locating the time interval, observibility solutions are used to access log messages and investigate the root cause of the error. This is a labour-intensive process that requires expertise of the monitored system. 
- **Complexity of the distributed systems**: Distributed systems are essential for modern data processing systems due to their nature of being scalable and error-tolerant. However, they bring their own challenges. For example, each component of the system may be developed by a different company, each with its own logging strategy based on its own interests. Heterogeneous log messages make it difficult to relate log messages automatically across the system.    
- **Hidden relations between input and output data**: Especially for payload data processing systems, the relationship between the input and output products may not be obvious. The quality or age of auxiliary data may affect third-level products.

## Concept

- **Agnostic to the monitored system**: The system should be agnostic to the monitored system and able to handle different log message structures.
- **Extract hidden relations**: The system should be able to identify hidden relationships within the monitoring system and report accordingly. If the quality of a product is degraded due to auxiliary data that is not directly inputted into the processor, the system should detect this relationship and issue a report. 
- **Predict error may occur in the near future**: The system should be able to predict any potential degradation in the quality or timeliness of the output by analysing the relationship between the input and output products.

## System Design 

The DP-MON system is composed of following modules:
- Collect log messages and metrics 
- Process and extract features 
- Index and store 
- Analyze  
- Visualize  

![Level 0](./docs/level_0_system.svg)


## Scope

This implementation serves as a proof of concept for the 'Analyze' module. Other modules have not yet been fully implemented, and COTS applications have been used instead.

## How to Run

```


```

## Open Points

- [ ] 