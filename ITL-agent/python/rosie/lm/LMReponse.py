""" Data structures for LM request and response

    Classes for storing LM response
"""
#from asyncio.windows_events import NULL
#import sys

from string import digits
from pysoarlib import WMInterface

from rosie.language.LanguageConnector import Message
#AgentConnector

# from operator import attrgetter
# from tkinter import E

# import numpy as np

# import os
# import warnings
# #warnings.filterwarnings("ignore")


class ObjectDescription:
    def __init__(self, object, location, preposition, object_handle):
        self.object = object
        self.location = location
        self.preposition = preposition
        self.handle = object_handle

    def __lt__(self, other):
        return self.object < other.object


class LMEvaluation:
    def __init__(self, interpreted, type, object, affordance, word, relation):
        self.interpreted = interpreted
        self.object = object
        self.type = type
        self.affordance = affordance
        self.word = word
        self.relation = relation


class SentenceProbability:
    def __init__(self, sentence, probability):
        self.sentence = sentence
        self.probability = probability

    def __lt__(self, other):
        return self.probability < other.probability


class LMMetadata(WMInterface):
    # def __init__(self, agent_order, prob_order, count_ungrounded, parsed):
    #     self.agent_order = agent_order
    #     self.prob_order = prob_order
    #     self.count_ungrounded = count_ungrounded
    #     self.parsed = parsed
    #     self.identifier = None

    def __init__(self, probability, temperature, source = "LM"):
        WMInterface.__init__(self)
        self.probability = probability
        self.temperature = temperature
        self.identifier = None
        self.source = source

    def __lt__(self, other):
        return self.probability < other.probability

    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("metadata")
        self.identifier.CreateFloatWME("probability", self.probability)
        self.identifier.CreateFloatWME("temperature", self.temperature)
        self.identifier.CreateStringWME("source", self.source)
        #self.identifier.CreateIntWME("prob-order", self.prob_order)
        #self.identifier.CreateIntWME("agent-order", self.agent_order)
        #self.identifier.CreateStringWME("parsed", self.parsed)
    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None
        

class LMSelection(WMInterface):
    def __init__(self, response, choice, metadata, object):
        WMInterface.__init__(self)
        self.object = object
        #self.id = None
        self.response = response
        self.choice = choice
        self.metadata = metadata
        self.message = Message(self.choice, 1500)

    def __str__(self):
        return self.choice

    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("selection-response")
        #self.identifier.CreateIntWME("id", self.id)
        self.identifier.CreateStringWME("selection-sentence", self.choice)
        self.identifier.CreateIntWME("selection-number", self.response)
        self.identifier.CreateStringWME("object-handle", self.object.handle)
        self.metadata.add_to_wm(self.identifier)
        self.message.add_to_wm(self.identifier)

    # def _update_wm_impl(self):
    #     for step in self.steps:
    #         step.add_to_wm(self.identifier)

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMTaskStepCompletion(WMInterface):
    def __init__(self, sentence, step_number, object, metadata = None, complete_prompt = "", prob_order =  0, steps = []):
        WMInterface.__init__(self)
        new_sentence = self.replace_object(sentence, object.object, object.object)
        self.sentence = new_sentence #sentence
        self.prob_order = prob_order
        self.metadata = metadata
        self.steps = steps
        self.step_number = step_number
        self.selected = False
        self.new_retry = False
        self.identifier = None
        self.object = object
        self.id = None
        self.complete_prompt_text = complete_prompt
        self.message = Message(self.sentence, 1100) # fix id of message...

    def __lt__(self, other):
        return self.prob_order < other.prob_order

    def __str__(self):
        return self.sentence

    def replace_object(self, sentence, object, swap):
        #how to handle "box-of-aluminum-foil"...
        #really same difficult if using hyphens or not... but more consitent in using them I think...
        #some cases cannot handle generally
        #e.g. for pepsi-can, pepsi might be acceptable but for metal-can metal is not...
        #add spaces at end to prevent replacing other words...
        #cupboard-> objectboard
        #garbage can -> garbage metal-can
        object = object + " "
        swap = swap + " "

        new_sentence = sentence
        object_variant_1 = object.replace("-", " ")
        object_variant_of1 = ""
        object_variant_of2 = ""
        if " of " in object_variant_1:
            object_of_parts = object_variant_1.split(" of ")
            object_variant_of1 = object_of_parts[0] + " "
            object_variant_of2 = object_of_parts[1]
        object_parts = object.split("-")
        length = len(object_parts)
        object_variant_2 = object_parts[-1] #last word
        object_variant_3 = ""
        object_variant_4 = ""
        if length == 3 and " of " not in object_variant_1 and " and " not in object_variant_1:
            object_variant_3 = object_parts[0] + " " + object_parts[-1]
            object_variant_4 = object_parts[1] + " " + object_parts[-1]
        object_variant_5 = ""
        if "box " == object_parts[-1]:
            object_variant_5 = object_variant_1.replace(" box" ,"")


        if object in sentence: #could still be an issue if get the goal is that the pepsi is in the garbage can and the can is closed.
            new_sentence = sentence.replace(object, swap, 1)
        elif object_variant_1 in sentence:
            new_sentence = sentence.replace(object_variant_1, swap, 1)
        elif object_variant_of1 and object_variant_of1 in sentence:
            new_sentence = sentence.replace(object_variant_of1, swap, 1)
        elif object_variant_of2 and object_variant_of2 in sentence:
            new_sentence = sentence.replace(object_variant_of2, swap, 1)
        elif object_variant_3 and object_variant_3 in sentence:
            new_sentence = sentence.replace(object_variant_3, swap, 1)
        elif object_variant_4 and object_variant_4 in sentence:
            new_sentence = sentence.replace(object_variant_4, swap, 1)
        elif object_variant_5 and object_variant_5 in sentence:
            new_sentence = sentence.replace(object_variant_5, swap, 1)
        elif object_variant_2 in sentence: #do this one last..
            new_sentence = sentence.replace(object_variant_2, swap, 1)
        return new_sentence
    
    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("completion")
        #self.id = self.identifier.GetValueAsString()
        self.identifier.CreateIntWME("id", self.id)
        self.identifier.CreateIntWME("prob-order", self.prob_order)
        if self.new_retry:
            self.identifier.CreateStringWME("retry-attempt", "true")
        else:
            self.identifier.CreateStringWME("retry-attempt", "false")
        self.identifier.CreateStringWME("complete-sentence", self.sentence)
        self.identifier.CreateIntWME("step-number", self.step_number)
        self.identifier.CreateStringWME("object-handle", self.object.handle)
        self.metadata.add_to_wm(self.identifier)
        self.message.add_to_wm(self.identifier)
        for step in self.steps:
            step.add_to_wm(self.identifier)

    def _update_wm_impl(self):
        for step in self.steps:
            step.add_to_wm(self.identifier)

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None


class LMTaskStep(WMInterface):
    def __init__(self, first, step_number, object, metadata = None, prob_order = None, completions = []):
        WMInterface.__init__(self)
        self.first = first
        self.prob_order = prob_order
        self.metadata = metadata
        self.object = object
        self.completions= completions
        self.step_number = step_number
        self.selected = False
        self.identifier = None

    def __lt__(self, other):
        return self.prob_order < other.prob_order

    def __str__(self):
        return str(self.step_number) + ". " + self.first
    
    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("step")
        self.identifier.CreateIntWME("prob-order", self.prob_order)
        self.identifier.CreateStringWME("first-word", self.first)
        self.identifier.CreateIntWME("step-number", self.step_number)
        self.identifier.CreateStringWME("object-handle", self.object.handle)
        self.metadata.add_to_wm(self.identifier)
        for completion in self.completions:
            completion.add_to_wm(self.identifier)
    
    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMGoal(WMInterface):
    def __init__(self, sentence, metadata, obj_desc, object, handle, prompt_type = "unknown", prob_order = 0, steps = [], retry_goals = []):
        WMInterface.__init__(self)
        #for recovery version
        new_sentence = self.replace_object(sentence.lower(), object.object, object.object)
        self.sentence = new_sentence #sentence
        words = new_sentence.split()
        self.last_word = words[-1]
        location_words = object.location.split()
        self.location = location_words[-1]
        self.prob_order = prob_order
        self.metadata = metadata
        self.steps = steps
        self.identifier = None
        self.selected = False
        self.retrying = False #goal to be retried
        self.new_retry = False #goal that is first retry attempt
        self.second_retry = False #goal that is second retry attempt
        self.obj_desc = obj_desc
        self.object = object
        self.object_handle = handle
        self.prompt_type = prompt_type
        article = "a "
        if self.object.object[0] in ('a', 'e', 'i', 'o', 'u'):
            article = "an "

        goal_descrip = self.replace_object(sentence.lower(), object.object, "object")
        self.if_sentence = "If the object is " + article + object.object + " then " + goal_descrip
        self.id = None
        self.complete_prompt_text = ""
        self.message = Message(self.if_sentence, 1000) # fix id of message...
        self.simple_message = Message(self.sentence, 1010)
        self.retry_goals = retry_goals
        for goal in retry_goals:
             self.add_node(goal)

    def replace_object(self, sentence, object, swap):

        object = object + " "
        swap = swap + " "

        new_sentence = sentence
        object_variant_1 = object.replace("-", " ")
        object_variant_of1 = ""
        object_variant_of2 = ""
        if " of " in object_variant_1:
            object_of_parts = object_variant_1.split(" of ")
            object_variant_of1 = object_of_parts[0] + " "
            object_variant_of2 = object_of_parts[1]
        object_parts = object.split("-")
        length = len(object_parts)
        object_variant_2 = object_parts[-1] #last word
        object_variant_3 = ""
        object_variant_4 = ""
        if length == 3 and " of " not in object_variant_1 and " and " not in object_variant_1:
            object_variant_3 = object_parts[0] + " " + object_parts[-1]
            object_variant_4 = object_parts[1] + " " + object_parts[-1]
        object_variant_5 = ""
        if "box " == object_parts[-1]:
            object_variant_5 = object_variant_1.replace(" box" ,"")


        if object in sentence: #could still be an issue if get the goal is that the pepsi is in the garbage can and the can is closed.
            new_sentence = sentence.replace(object, swap, 1)
        elif object_variant_1 in sentence:
            new_sentence = sentence.replace(object_variant_1, swap, 1)
        elif object_variant_of1 and object_variant_of1 in sentence:
            new_sentence = sentence.replace(object_variant_of1, swap, 1)
        elif object_variant_of2 and object_variant_of2 in sentence:
            new_sentence = sentence.replace(object_variant_of2, swap, 1)
        elif object_variant_3 and object_variant_3 in sentence:
            new_sentence = sentence.replace(object_variant_3, swap, 1)
        elif object_variant_4 and object_variant_4 in sentence:
            new_sentence = sentence.replace(object_variant_4, swap, 1)
        elif object_variant_5 and object_variant_5 in sentence:
            new_sentence = sentence.replace(object_variant_5, swap, 1)
        elif object_variant_2 in sentence: #do this one last..
            new_sentence = sentence.replace(object_variant_2, swap, 1)
        return new_sentence

    def __lt__(self, other):
        return self.metadata < other.metadata


    # def __eq__(self, other):
    #     return self.sentence.lower() == other.lower()
    
    def __str__(self):
        return self.sentence

    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("goal")
        self.identifier.CreateIntWME("id", self.id)
        self.identifier.CreateIntWME("prob-order", self.prob_order)
        if self.new_retry:
            self.identifier.CreateStringWME("retry-attempt", "true")
        else:
            self.identifier.CreateStringWME("retry-attempt", "false")

        if self.second_retry:
            self.identifier.CreateStringWME("second-retry-attempt", "true")
        else:
            self.identifier.CreateStringWME("second-retry-attempt", "false")
        if self.retrying:
            self.identifier.CreateStringWME("retrying", "true")
        self.identifier.CreateStringWME("complete-sentence", self.sentence)
        self.identifier.CreateStringWME("last-word", self.last_word)
        self.identifier.CreateStringWME("object-location", self.location)
        self.identifier.CreateStringWME("if-sentence", self.if_sentence)
        self.identifier.CreateStringWME("object-desc", self.obj_desc)
        self.identifier.CreateStringWME("object-handle", self.object_handle)
        self.identifier.CreateStringWME("prompt-type", self.prompt_type)
        self.metadata.add_to_wm(self.identifier)
        self.message.add_to_wm(self.identifier)
        self.simple_message.add_to_wm(self.identifier)
        for step in self.steps:
            step.add_to_wm(self.identifier)
        for goal in self.retry_goals: #should be here?
            goal.add_to_wm(self.identifier)

    def _update_wm_impl(self):
        for step in self.steps:
            step.add_to_wm(self.identifier)
        for goal in self.retry_goals: #should be here?
            goal.add_to_wm(self.identifier)

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMPromptExamples(WMInterface):
    def __init__(self, num_examples, text, examples = None):
        WMInterface.__init__(self)
        self.num_examples = num_examples
        self.text = text
        self.examples = examples
        self.identifier = None

    def __str__(self):
        return self.text

    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("prompt-examples")
        self.identifier.CreateStringWME("num-examples", self.num_examples)
        #don't add text to WME

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMSettings(WMInterface):
    def __init__(self, style, temperature, best_of, model):
        WMInterface.__init__(self)
        self.style = style
        self.temperature = temperature
        self.best_of = best_of
        self.model = model
        self.identifier = None
    
    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("lm-settings")
        self.identifier.CreateStringWME("temperature", self.temperature)
        self.identifier.CreateStringWME("style", self.text)
        self.identifier.CreateStringWME("best_of", self.best_of)
        self.identifier.CreateStringWME("model", self.model)

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMPrompt(WMInterface):
    def __init__(self, id, text, object, task, location, settings = None, examples = None):
        WMInterface.__init__(self)
        self.id = id
        self.text = text
        self.object = object
        self.task = task
        self.location = location
        self.settings = settings
        self.examples = examples
        self.identifier = None

    def __str__(self):
        return self.text
    
    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("prompt")
        self.identifier.CreateStringWME("id", self.id)
        #self.identifier.CreateStringWME("text", self.text)
        self.identifier.CreateStringWME("object", self.object.object)
        self.identifier.CreateStringWME("task", self.task)
        self.identifier.CreateStringWME("location", self.location)

    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None

class LMResponse(WMInterface):
    def __init__(self, id, prompt, task_handle, object_desc, object, object_handle, goals = []):
        WMInterface.__init__(self)
        self.id = id
        self.prompt = prompt
        #prompt is string or class? (class probably better, put settings, object, task in)
        self.goals = goals
        self.identifier = None
        self.nodes = []
        self.dupes = []
        self.task_handle = task_handle
        self.obj_desc = object_desc
        self.object_handle = object_handle
        self.next_node_id = 1
        self.object = object
        for goal in goals:
             self.add_node(goal)

    def add_node(self, node):
        node.id = self.next_node_id
        self.next_node_id += 1
        self.nodes.append(node)
        return

    def _add_to_wm_impl(self, parent_id):
        self.identifier = parent_id.CreateIdWME("response")
        self.identifier.CreateStringWME("id", self.id)
        self.identifier.CreateStringWME("object-handle", self.object_handle)
        self.identifier.CreateStringWME("task-handle", self.task_handle)
        for goal in self.goals:
            #goal.add_to_wm_impl(self.identifier)
            goal.add_to_wm(self.identifier)
            #self.nodes.append(goal)
        
        self.prompt.add_to_wm(self.identifier)
    
    def _remove_from_wm_impl(self):
        self.identifier.DestroyWME()
        self.identifier = None