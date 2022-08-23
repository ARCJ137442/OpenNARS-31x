# OpenNARS-3.1.3-dev
OpenNARS 3.1.3 Development Edition

**README for developers**  | Tangrui Li (tuo90515@temple.edu)

<font color="red"> [last edited 22/8/23] </font>

 

# Introduction

 

The most important feature of OpenNARS 3.1.3 is about the extending of buffers which builds a bridge of information influx from the outside world and NARS. If it is correctly implemented, temporal and spatial relationships will be combined. Note that this “spatial” relationship does not referrer to the 3D real world.

Currently, the multi-channel functionality of OpenNARS 3.1.3 is available in a limited scope. Narsese channel is disabled, and all inputs are predefined (pretending connected to the working environment). Functions related to operations are not fully implemented, so spatial relationships cannot be captured.

In the following sections, details about the implementation will be explained.



# Buffer cycle

 

## Compound generation

“Atomic” (not necessarily atomic in syntax) events generated by sensorimotor channels have a priori truth-values and budgets. Take human vision as an example, we recognize the object in the center of the field of view easier, which is about the budget. For computer vision, all objects are detected with accuracy scores, which is about the truth-value.

These atomic events are restored in a priority queue, whose priority values are calculated externally (not the “priority” in the budget). This priority queue is also used to store compounds. 

After having these atomic events, compounds are made by exhaustion. Say a buffer has ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image002.png) events, there will be ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image004.png) combinations, and we just want the top-![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image006.png) of them. ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image008.png), picking this ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image006.png) as a soft margin (e.g., ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image010.png)). These ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image006.png) compounds are called “concurrent compounds”.

After having concurrent compounds, which means the present focus (say we focus on an alarm light and it is currently red), we may generate compounds related to the past. If we have ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image012.png) past time slots, we have ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image012.png) past “highest focus” (previously the alarm light is blue), we need to generate ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image014.png) such temporal compounds (red after blue; ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image016.png) since the highest focus includes two events). These temporal compounds are stored in a different priority queue to separate the episodic and instantaneous cases.

## Local evaluation

Focusing on the whole leads to less attention on the part, so if the compound and its components co-occur in priority queues, components’ priority will be penalized to ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image018.png) of the original and this is why we want to pick ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image020.png) in compound generation.

Anticipations are another component in each time slot, which are expected events made in the past (it is cloudy, so I expect it will be rainy in a short future). They are made by predictions, which are short-term (context related) temporal implications. Each prediction will have a precondition and a predict (if precondition then predict). Preconditions are checked and the prediction will fire (put the anticipation in the correct time slot) if satisfied. It happens before checking anticipation since anticipations may be placed in the present time slot (in the internal experience buffer). 

Achieved anticipations will be revised with the input event if possible. This is useful when the anticipation is strong enough to change the observation (it is sunny, but I see water is dripping outside my window, I don’t believe it is raining). Correspondingly, the prediction who generated this anticipation will have a ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image018.png) priority bonus. Unachieved anticipations will simply be dropped, but the prediction who generated it will have a ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image018.png) priority penalty.

Except for these anticipations, “unexpected observations” as an accident will have a 20% priority bonus, but currently this bonus is applied only on concurrent compounds since there are too many temporal ones.

## Memory-based evaluation

The above steps are basically context-based, say there is a breaking news. But when I am working, I may not pay attention to that since it is “unimportant” subject to me. Therefore, these events will be checked against the memory. If it is in the memory, its priority will be increased by 20% mainly because this term had already built relationships with other terms (e.g., hotdog). It is planned to merge these priorities in some way in the future.

After that, these two priority queues will not change in this cycle, and it is time to get the “highest concurrent compound” and the “highest temporal compound” as the highest focus. All these two compounds are recorded, as two choices (whether inherit the history or not, e.g., “red-blue-red” or “red-blue” and “blue-red”) when building temporal compounds. But by all means there is only one “present attention”, which is the higher one in the highest focus named the “highest compound”.

## Prediction generation

By figuring out the highest compound, since it is the attention, NARS needs to think what makes it worthy attention. Therefore, ![img](file:///C:/Users/TORY/AppData/Local/Temp/msohtmlclip1/01/clip_image012.png) previous highest compounds will be used to generate the predictions, which are stored in a priority queue called “prediction table”. 



# Buffer cycle summary

 

In summary, a buffer has several slots, some are for the past and the future, one is for the present time. In addition to that, it also has a prediction table. Each slot has two priority queues, one for concurrent compounds, another for temporal compounds. Among them, we have the highest concurrent compound and the highest temporal compound which are used for recording as two choices. Among them, the higher one is the present focus, which is used for recording and forwarding.
