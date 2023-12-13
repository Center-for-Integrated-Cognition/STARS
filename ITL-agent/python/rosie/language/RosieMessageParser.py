""" Used in the Rosie project to parse messages from the Rosie Agent

    Handles send-message commands on the output link
"""
import sys

from string import digits
from pysoarlib.util import extract_wm_graph
from rosie.tools import object_to_str

strip_digits = lambda s: s.translate(str.maketrans('', '', digits))

task_handle = lambda fields: strip_digits(fields['task-handle'])
world_obj = lambda fields: object_to_str(fields['object'].id)
obj_handle = lambda fields: object_handle_to_str(fields['object-handle'])

def object_handle_to_str(handle):
    parts = handle.split("_")
    result = parts[0].strip("1")
    return result

class RosieMessageParser:
    def parse_message(root_id, message_type):
        msg_graph = extract_wm_graph(root_id, 4)
        fields = msg_graph['fields']
        message_parser = RosieMessageParser.message_parser_map.get(message_type)
        if message_parser is not None and callable(message_parser):
            return message_parser(fields)
        return message_type

    message_parser_map = {
        "ok": lambda fields: "Ok",
        "unable-to-satisfy": lambda fields: "I couldn't do that",
        "unable-to-interpret-message": lambda fields: "I don't understand.",
        "missing-object": lambda fields: "I lost the object I was using. Can you help me find it?",
        "index-object-failure": lambda fields: "I couldn't find the referenced object",
        "no-proposed-action": lambda fields: "I couldn't do that",
        "missing-argument": lambda fields: "I need more information to do that action",
        "learn-location-failure": lambda fields: "I don't know where I am.",
        "attempt-lm-goal": lambda fields: "[LM] For " + fields['object'] + " is " + fields['sentence'].lower() + "?",
        "attempt-lm-step": lambda fields: "[LM] For the " + obj_handle(fields) + " should I '" + fields['sentence'] + "'?",
        "get-next-goal": lambda fields: "What is the next goal or subtask of " + task_handle(fields) + "?",
        "get-next-goal-LM": lambda fields: "[Constructing & sending prompt to GPT-3 for " + task_handle(fields) + " task steps/goal]\n[Receiving task goal from GPT-3 response]",
        "get-goal-alternative": lambda fields: "What should I do for " + task_handle(fields) + " when other steps don't apply?",
        "no-action-context-for-goal": lambda fields: "I don't know what action that goal is for",
        "get-next-task": lambda fields: "I'm ready for a new task",
        "get-next-subaction": lambda fields: "What do I do next for " + task_handle(fields) + " " + world_obj(fields) + "?",
        "get-next-subaction-LM": lambda fields: "[Retrieving next step from GPT-3 response]",
        "confirm-pick-up": lambda fields: "I have picked up the object.",
        "confirm-put-down": lambda fields: "I have put down the object.",
        "searching": lambda fields: "Searching...",
        "stop-leading": lambda fields: "You can stop following me",
        "retrospective-learning-failure": lambda fields: "I was unable to learn the task policy",
        "report-successful-training": lambda fields: "Ok",
        "task-execution-failure": lambda fields: "The " + task_handle(fields) + " task failed.",
        "maintenance-goal-achieved": lambda fields: "I have achieved the goal of " + task_handle(fields) + ".",
        
        #added for games and puzzles
        "unknown-defined-word": lambda fields: "Please define the meaning of " + fields['word'] + " in this context.",
        "unknown-word": lambda fields: "Please define the meaning of " + fields['word'] + " in this context.",
        "learned-unknown-word": lambda fields: "I have learned the meaning of " + fields['word'] + ".",
        "learned-one-meaning": lambda fields: "I have learned one meaning of " + fields['word'] + ". What is another?",
        "your-turn": lambda fields: "Your turn.",
        "i-win": lambda fields: "I win!",
        "i-lose": lambda fields: "I lose.",
        "easy": lambda fields: "That was easy!",
        "describe-game": lambda fields: "Please setup the game.",
        "describe-puzzle": lambda fields: "Please setup the puzzle.",
        "setup-goal": lambda fields: "Please setup the goal state.",
        "tell-me-go": lambda fields: "Ok: tell me when to go.",
        "setup-failure": lambda fields: "Please setup the failure condition.",
        "define-actions": lambda fields: "Please describe the actions, goals, and failure conditions.",
        "describe-action": lambda fields: "What are the conditions of the action.",
        "describe-goal": lambda fields: "Please describe or demonstrate the goal.",
        "describe-failure": lambda fields: "Please describe the failure condition.",
        "learned-goal": lambda fields: "I have learned the goal.",
        "learned-action": lambda fields: "I have learned the action.",
        "learned-failure": lambda fields: "I have learned the failure condition.",
        "learned-heuristic": lambda fields: "I have learned the heuristic.",
        "already-learned-goal": lambda fields: "I know that goal and can recognize it.",
        "already-learned-action": lambda fields: "I know that action and can recognize it.",
        "already-learned-failure": lambda fields: "I know that failure condition and can recognize it.",
        "gotit": lambda fields: "I've found a solution.",
        "learned-game": lambda fields: "I've learned " + fields['game'] + ". Should I solve?",

        "single-word-message": lambda fields: fields['word'],
        "say-sentence": lambda fields: fields['sentence'],
        "agent-object-description": lambda fields: world_obj(fields),
        "cant-find-object": lambda fields: "I can't find " + world_obj(fields) + \
            ", can you help?"
    }

