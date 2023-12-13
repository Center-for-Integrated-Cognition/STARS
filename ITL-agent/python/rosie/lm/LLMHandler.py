""" Used in the Rosie project to handle LM requests from the Rosie Agent

    Handles lm-requests commands on the output link
"""
from argparse import Action
from audioop import reverse
from cmath import nan
from pickle import GLOBAL
import sys
import math
import operator
import time
from string import digits

import time, json

from operator import attrgetter
from turtle import TurtleScreen

import openai
import numpy as np

#import APIKey

from .LMReponse import ObjectDescription, LMEvaluation, LMGoal, LMMetadata, LMPrompt, SentenceProbability, LMPromptExamples, LMResponse, LMSettings, LMTaskStep, LMTaskStepCompletion, LMSelection
from .APIKey import APIKey
#import APIKey
import os
import warnings
#warnings.filterwarnings("ignore")

# Load your API key from an environment variable or secret management service
#openai.api_key = os.getenv("OPENAI_API_KEY")
openai.api_key = APIKey.OPENAI_API_KEY

class LMOutput:
    def __init__(self, result, probability):
        self.result = result
        self.probability = probability
 
    def __repr__(self):
        return '(' + self.result + ', ' + "{:.4f}".format(self.probability) + ')'


def get_str_from_file(filename):
	lines = []
	with open(filename) as f:
		lines = f.read().splitlines()
	output = ""
	for line in lines:
		output += line + "\n"
	return output;

#best to store prompts/responses and then write at end

class LLMHandler:

    def __init__(self):
        self.messages = list()
        self.lm_responses = [] #can find here?
        self.current_response = None
        self.dupes = []
        self.next_message = None
        self.update_node = None
        self.safe_limit = 0
        #Plan to get from agent
        self.rosie_actions = ["Pick", "Put", "Go", "Pour", "Approach", "Open", "Close", "Place", "Turn", "pick", "put", "go", "pour", "approach", "open", "close", "place", "turn"]


    #right now not considering different subsets of examples for experiment
    def construct_prompt_example(self, num_examples):
        rosie_home = os.environ["ROSIE_HOME"]
        dir = rosie_home + "/python/rosie/lm/"
        prompt_example = "(EXAMPLES)"    
        for x in range(1, num_examples+1):
            filename = dir + "example_" + str(x) + ".txt"
            #goal only version
            #filename = dir + "goal_example_" + str(x) + ".txt"
            #print(filename)
            prompt_example += "(TASK)"
            prompt_example += get_str_from_file(filename)
            prompt_example += "(END TASK)\n"
        prompt_example += "(END EXAMPLES)\n"
        return prompt_example;

    def get_template(self):
        prompt_template ="(TASK)Task name: ?goal. Task context: I am in ?location. Aware of ?object.\n"
        prompt_template +="(RESULT)"
        return prompt_template;

    def get_selection_example(self):
        example = ""
        rosie_home = os.environ["ROSIE_HOME"]
        dir = rosie_home + "/python/rosie/lm/examples/corrections/"
        filename = dir + "selection.txt"
        example += get_str_from_file(filename)
        return example

    def get_selection_example_GPT4(self):
        example = ""
        rosie_home = os.environ["ROSIE_HOME"]
        dir = rosie_home + "/python/rosie/lm/examples/corrections/"
        filename = dir + "selection_GPT4.txt"
        example += get_str_from_file(filename)
        return example

    def get_task_template(self, goal, location, obj_desc):
        temp ="(TASK)Task name: ?goal. Task context: I am in ?location. Aware of ?object.\n"
        temp = temp.replace("?object", obj_desc)
        temp = temp.replace("?goal", goal)
        temp = temp.replace("?location", location)
        return temp;

    def get_task_template_GPT4(self, goal, location, obj_desc):
        temp ="Task name: ?goal. Task context: I am in ?location. Aware of ?object.\n"
        temp = temp.replace("?object", obj_desc)
        temp = temp.replace("?goal", goal)
        temp = temp.replace("?location", location)
        return temp;


    def construct_prompt(self, object, template, goal, location):
        prompt = ""
        
        prompt += self.construct_prompt_example(2)
        temp = self.get_template()
        obj_descriptions =""


        article = "a "
        if object.object[0] in ('a', 'e', 'i', 'o', 'u'):
            article = "an "

        obj_descriptions= object.object + " " + object.preposition + " " + object.location
        #obj_descriptions= article + object.object + " " + object.preposition + " the " + object.location

        temp = temp.replace("?object", obj_descriptions)
        temp = temp.replace("?goal", goal)
        temp = temp.replace("?location", location)
        prompt += temp

        return prompt;
    

    def get_LM_Goal_temp0_G4(self, input_prompt, obj_desc_article, object, object_handle):
        text = input_prompt

        message = [
            {"role": "user", "content": text}
        ]
        response = openai.ChatCompletion.create(
            #engine="text-davinci-002",
            model="gpt-4",
            messages = message,
            max_tokens=50, #10
            top_p=1,
            temperature=0.0,
            stop=["(END RESULT)", ".", "Steps:", "\n"])

        out = response.choices[0]
        out2 = out['message']
        output = out2['content'].strip()
        
        output =output.replace(".", "")
        #logprob = out['logprobs']

        #no log prob, using 0.95 for stand in
        true_prob = 0.95 #elf.get_goal_probability(logprob)
        print("True prob of 0 temp: " + str(true_prob))

        return LMGoal(output, LMMetadata(true_prob, 0.0), obj_desc_article, object, object_handle)

    def get_LM_Goal_temp0(self, input_prompt, obj_desc_article, object, object_handle):
        text = input_prompt
        print("Prompt: (temp 0)")
        print(text)
        response = openai.Completion.create(
            engine="text-davinci-002",
            prompt=text,
            max_tokens=50, #10
            top_p=1,
            temperature=0.0,
            logprobs=1,
            stop=["(END RESULT)", ".", "Steps:", "\n"])
        
        
        out = response.choices[0]
        output = out['text'].strip()

        prompt_type = "initial_prompt"

        self.print_token_cost(out, response, object_handle, prompt_type)
        
        #can get log probs of whole thing?
        output =output.replace(".", "")
        logprob = out['logprobs']

        true_prob = self.get_goal_probability(logprob)
        print("True prob of 0 temp: " + str(true_prob))
  
        #result_list = output.split('\n')
        #Add parent pointer?
        #return LMGoal(output, LMMetadata(true_prob, 0.0), obj_desc_article, object, object_handle)
        
        return LMGoal(output, LMMetadata(true_prob, 0.0), obj_desc_article, object, object_handle, prompt_type)
        
        #return LMOutput(output, true_prob);


    def print_token_cost(self, out, response, object_handle, prompt_type):
        token_stats = response['usage']
        tokens_prompt = token_stats['prompt_tokens']
        tokens_completion = 0
        if out['text'].rstrip():
            tokens_completion = token_stats['completion_tokens']
        tokens_total = token_stats['total_tokens']

        print(prompt_type + ";" + object_handle + ";" + str(tokens_prompt) + ";" + str(tokens_completion) + ";" + str(tokens_total) + "\n")
        return

###################################
# Code for using Search Tree strategy (Paper Section 4.1)
#######################################
    def get_LM_Goal_tree_temp0(self, input_prompt, num_results, obj_desc_article, object, object_handle, repair, recurse_count, current_output, prob_list):
        text = input_prompt
        print("Entering goal retrieval tree with recurse level " + str(recurse_count))
        print("Prompt:")
        print(text)
        if self.safe_limit > 1500:
            print("safety limit reached")
            return []
        self.safe_limit = self.safe_limit +1
        
        response = openai.Completion.create(
            engine="text-davinci-002",
            prompt=text,
            max_tokens=50, #10
            top_p=1,
            temperature=0.0,
            logprobs=5,
            stop=["(END RESULT)", ".", "Steps:", "\n"])
        
        out = response.choices[0]

        token_stats = response['usage']
        tokens_prompt = token_stats['prompt_tokens']
        tokens_completion = 0
        if out['text'].rstrip():
            tokens_completion = token_stats['completion_tokens']
        tokens_total = token_stats['total_tokens']

        prompt_type = "initial_prompt"
        if repair:
            if recurse_count > 1:
                prompt_type = "repair_recurse_prompt"
            else:
                prompt_type = "repair_prompt"
        elif recurse_count > 0:
            prompt_type = "recurse_prompt"
        
        print(prompt_type + ";" + object_handle + ";" + str(tokens_prompt) + ";" + str(tokens_completion) + ";" + str(tokens_total) + "\n")

        output = out['text'].rstrip()	
        output =output.replace(".", "")
        if current_output:
            output = current_output + output

        logprob = out['logprobs']

        result_prob_list = []

        index = self.get_goal_end_index(logprob)
        top_probs = logprob['top_logprobs'][0:index] #might not need/want this...

        new_log_prob = self.get_goal_probability_list(logprob)
        if prob_list:
            new_log_prob = prob_list + new_log_prob

        true_prob= np.mean(new_log_prob)

        output1 = self.replace_object(output.lower(), object.object, object.object)
        if self.duplicate_with_prior(output1) or output1 in self.dupes:
            print("Duplicate detected for : " + output)
        else:
            self.dupes.append(output1)
            root_goal = LMGoal(output, LMMetadata(true_prob, 0.0), obj_desc_article, object, object_handle, prompt_type)
            result_prob_list.append(root_goal)
            #return result_prob_list #prevent tree beam search

        if recurse_count > 1:#self.recurse_limit
            print("Recursion limit")
            return result_prob_list

        i = 0
        new_prompt_starts = []
        #use token to split on output for completion
        partial_response = ""
        partial_prob_list = []
        while (i < index):
            token_prob = top_probs[i] #dictionary, top_probs is array

            #want to sort so can just ask of top
            sorted_tokens = sorted(token_prob.items(), key=operator.itemgetter(1), reverse=True)
            
            first_token = sorted_tokens[0][0]
            first_token_prob = np.exp(float(sorted_tokens[0][1]))

            second_token = sorted_tokens[1][0]
            second_token_prob = np.exp(float(sorted_tokens[1][1]))

            #change so take lowest?
            if first_token_prob < 0.90: #generate temp0 tree
                second_token = second_token.lower().strip() 
                if second_token == first_token.lower().strip() or first_token == "-" or second_token == "-":
                    i=i+1
                    partial_response += first_token
                    partial_prob_list.append(first_token_prob)
                    continue #ignore break
                print("Branch on token " + first_token + " (probability: " + str(first_token_prob) + ")")
                
                j = 1 #already have completion for first version
                while j < len(sorted_tokens):

                    curr_token = sorted_tokens[j]
                    curr_prob = np.exp(float(curr_token[1]))
                    string_token = curr_token[0]
                    if string_token.strip() == ",":
                        j=j+1
                        continue
                    if curr_prob > 0.05: #0.085 #0.07#low percent for now... #want to be weighted by average?
                        print(" Selecting secondary branching token for :" + string_token +" (probability: " + str(curr_prob) + ")")
                        new_completion = partial_response + string_token.lower()
                        new_completion_prob_list = prob_list + partial_prob_list
                        new_completion_prob_list.append(curr_prob)
                        new_prompt_starts.append((new_completion,new_completion_prob_list))
                    else:
                        print(" Not selecting secondary branching token for:" + string_token +" (probability: " + str(curr_prob) + ")")
                    j=j+1

            partial_response += first_token
            partial_prob_list.append(first_token_prob)
            i=i+1
        
        for prompt_tuple in new_prompt_starts:
            prompt = prompt_tuple[0]
            #check if branching token is newline or . = end of sentence, do not need to continue completion
            if prompt[-1] == '\n' or prompt[-1] == '.' or prompt[-1] == '(':
                prompt =prompt.replace(".", "")
                prompt =prompt.replace("\n", "")
                prompt =prompt.replace("(", "")
                
                new_out = current_output + prompt
                new_out = new_out.strip()
                output1 = self.replace_object(new_out.lower(), object.object, object.object)
                if self.duplicate_with_prior(output1) or output1 in self.dupes:
                    print("Prior Duplicate detected! for : " + output)
                else:
                    #need mean of any past tokens, plus new ones, and finally new token...
                    print("Response completion:" + prompt)
                    new_prob= np.mean(prompt_tuple[1])
                    new_output = current_output + prompt
                    self.dupes.append(output1)
                    new_prompt_type = prompt_type + "_end_branch"
                    result_prob_list.append(LMGoal(new_output.strip(), LMMetadata(new_prob, 0.0), obj_desc_article, object, object_handle, new_prompt_type))
                continue
            
            sofar_prob= np.mean(prompt_tuple[1])
            curr = current_output + prompt
            if recurse_count > 0 and sofar_prob < 0.85:#self.recurse_limit
                print("New recursion limit for :" + curr  + " (probability: " + str(sofar_prob) + ")")
                continue
            #otherwise recurse
            new_prompt = input_prompt + prompt
            
            #add curr to dupes to prevent recursing on duplicate response
            if curr in self.dupes:
                #print("duplicate in recursion\n")
                continue
            self.dupes.append(curr)
            print("Recursing (level " + str(recurse_count) + " ) on " + curr  + " (probability " + str(sofar_prob) + ")")
            time.sleep(0.5)
            results = self.get_LM_Goal_tree_temp0(new_prompt, num_results, obj_desc_article, object, object_handle, repair, recurse_count+1, current_output + prompt, prompt_tuple[1])
            for result in results:
                result_prob_list.append(result)

        #passing prob list that include current!
        #and appending it onto prob list in new goal prob 
        return result_prob_list;


    def get_LM_Action_temp0(self, input_prompt, task_step, step_number, object):
        text = input_prompt + str(step_number) + ". " + task_step.first
        response = openai.Completion.create(
            engine="text-davinci-002",
            prompt=text,
            max_tokens=100, #10
            top_p=1,
            temperature=0.0,
            logprobs=1,
            stop=["\n"])
        
        out = response.choices[0]
        output = out['text'].strip()	
        output =output.replace(".", "")
        logprob = out['logprobs']
        true_prob = self.get_goal_probability(logprob)
        print("True prob of 0 temp: " + str(true_prob))
        sentence = task_step.first + " " + output
        complete_prompt = text + " " + output + "\n"
        return LMTaskStepCompletion(sentence, step_number, object, LMMetadata(true_prob, 0.0), complete_prompt)


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


        if object in sentence: 
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
    
    def get_GPT3_completion(self, input_prompt, task_step, step_number, num_results, temp, object):
        text = input_prompt + str(step_number) + ". " + task_step.first
        best_n = num_results +1 
        
        completion_list = []
        temp0_result = self.get_LM_Action_temp0(input_prompt, task_step, step_number, object)
        self.current_response.add_node(temp0_result)
        completion_list.append(temp0_result)
     
        print("Completion Prompt:")
        print(text)
        response = openai.Completion.create(
            engine="text-davinci-002", 
            prompt=text,
            max_tokens=130,
            temperature=temp,
            top_p=1,
            n=best_n,
		    best_of=best_n+2,
            logprobs=1,
            stop=["\n"])

        i = 0

        total_count = 0
        while (i < best_n and total_count < (best_n-2)):
            out = response.choices[i]
            output = out['text'].strip()
            output =output.replace(".", "")
            logprob = out['logprobs']

            true_prob = self.get_goal_probability(logprob)

            sentence = task_step.first + " " + output

            duplicate_detected = False
            for result in completion_list:
                output1 = self.replace_object(sentence.lower(), object.object, object.object)
                if output1 == result.sentence.lower():
                    duplicate_detected = True
                    break;
            if duplicate_detected:
                print("Duplicate detected! for : " + sentence)
                i +=1
                continue;

            complete_prompt = text + " " + output + "\n"

            completion = LMTaskStepCompletion(sentence, step_number, object, LMMetadata(true_prob, temp), complete_prompt)
            self.current_response.add_node(completion)
            completion_list.append(completion)
            total_count += 1
            i +=1

        #sort
        completion_list.sort(key=attrgetter('metadata'), reverse=True)

        order = 1
        print("Completions:")
        for res in completion_list:
            print(res)
            res.metadata.prob_order = order
            res.prob_order = order
            order += 1

        return completion_list;

    def run_experiment_GPT3_iterative_expand(self, input_prompt, task_step_list, step_number, temp, object):
        for res in task_step_list:
            res.completions = self.get_GPT3_completion(input_prompt, res, step_number, 2, temp, object)

        return;

    def run_experiment_GPT3_iterative_initial(self, input_prompt, step_number, object):
        #text = input_prompt
        text = input_prompt + str(step_number) + "."
        print("Step Prompt:")
        print(text)
        response = openai.Completion.create(
            engine="text-davinci-002", 
            prompt=text, 
            max_tokens=4, 
            top_p=1,
            temperature=0.0,
            logprobs=5,
            stop=[" "])
        result = response.choices[0]._previous
        logprob = result['logprobs'] #qqqqqq
        top = logprob['top_logprobs'][0]
        result_prob_list = []
        #result_list = []

        for item in top:
            prob = top[item]
            output = item.strip()
            true_prob = np.exp(float(prob))
            if (true_prob > 0.50) or (output in self.rosie_actions and true_prob > 0.09):
                step = LMTaskStep(output, step_number, object, LMMetadata(true_prob, 0.0))
                result_prob_list.append(step)

        result_prob_list.sort(key=attrgetter('metadata'), reverse=True)
        #change to make prob order...

        order = 1
        for res in result_prob_list:
            print(res)
            res.metadata.prob_order = order
            res.prob_order = order
            order += 1

        return result_prob_list;

    def get_LM_Actions(self, input_prompt, expand, step_number, temp, num_results):
        text = input_prompt
        new_result_prob_list = []
        final_output = ""
        result_list = []

        object = expand.object
        task_step_list = self.run_experiment_GPT3_iterative_initial(input_prompt, step_number, object)

        self.run_experiment_GPT3_iterative_expand(input_prompt, task_step_list, step_number, temp, object)

        print("Finished completions of steps...")
        return task_step_list

    def get_problist_goal(self, logprob):
        #debugging
        print(logprob['tokens'])
        
        if " (" in logprob['tokens']:
            index = logprob['tokens'].index(" (")
            print("Index: " + str(index))
            print(logprob['tokens'][0:index])
            return logprob['token_logprobs'][0:index]
        elif "(" in logprob['tokens']:
            index = logprob['tokens'].index("(")
            print("Index: " + str(index))
            print(logprob['tokens'][0:index])
            return logprob['token_logprobs'][0:index]
        elif ".(" in logprob['tokens']:
            index = logprob['tokens'].index(".(")
            print("Index: " + str(index))
            print(logprob['tokens'][0:index])
            return logprob['token_logprobs'][0:index]
        elif "\n" in logprob['tokens']:
            index = logprob['tokens'].index("\n")
            print("Didn't find index for goal (")
            print("Index: " + str(index))
            print(logprob['tokens'][0:index])
            return logprob['token_logprobs'][0:index]
        else:
            print("Didn't find index for goal newline")
            print("Index: last")
            return logprob['token_logprobs']


    def get_goal_end_index(self, logprob):
        index = len(logprob['tokens']) #use as default
        
        if " (" in logprob['tokens']:
            index = logprob['tokens'].index(" (")
        if "(" in logprob['tokens']:
            index2 = logprob['tokens'].index("(")
            if index2 < index:
                index = index2
        if ".(" in logprob['tokens']:
            index2 = logprob['tokens'].index(".(")
            if index2 < index:
                index = index2
        if "\n" in logprob['tokens']:
            index2 = logprob['tokens'].index("\n")
            if index2 < index:
                index = index2
        if "." in logprob['tokens']:
            index2 = logprob['tokens'].index(".")
            if index2 < index:
                index = index2
        return index

    def get_goal_probability_list(self, logprob):
        problist = []
        index = self.get_goal_end_index(logprob)
        print("Response:")
        print(logprob['tokens'][0:index])
        problist = logprob['token_logprobs'][0:index]
        
        #true_prob = np.exp(float(np.mean(problist)))
        #print("Normal prob: " + str(true_prob))
        exp_prob_list = []
        for p in problist:
            exp_prob_list.append(np.exp(float(p)))
        
        return exp_prob_list

    def get_goal_probability(self, logprob):
        exp_prob_list = self.get_goal_probability_list(logprob)
        true_prob= np.mean(exp_prob_list)
        #print(exp_prob_list)
        print("\n")
        print("Prob: " + str(true_prob))
        
        return true_prob


    def get_LM_Goal_G4(self, input_prompt, temp, num_results, obj_desc_article, object, object_handle):
        text = input_prompt
        best_n = num_results +1 

        handle = object.object
        temp0_result = self.get_LM_Goal_temp0_G4(input_prompt, obj_desc_article, object, object_handle)
        result_prob_list = []
        if not self.duplicate_with_prior(temp0_result.sentence):
            result_prob_list.append(temp0_result)

        message = [
            {"role": "user", "content": text}
        ]
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages = message,
            max_tokens=50, #10
            top_p=1,
            temperature=temp,
            n=best_n,
            stop=["(END RESULT)", ".", "Steps:", "\n"])
        
        i = 0
        total_count = 0
        while (i < best_n and total_count < (best_n-2)):
            out = response.choices[i]
            out2 = out['message']
            output = out2['content'].strip()
            output =output.replace(".", "")

            true_prob = 0.8
            if math.isnan(true_prob):
                i +=1
                continue;
            output1 = self.replace_object(output.lower(), handle, handle)
            duplicate_detected = False
            for result in result_prob_list:
                if output1 == result.sentence.lower():
                    duplicate_detected = True
            if duplicate_detected:
                print("Duplicate detected! for : " + output)
                i +=1
                continue;
            if self.duplicate_with_prior(output1):
                print("Prior Duplicate detected! for : " + output)
                i +=1
                continue;

            total_count += 1
            result_prob_list.append(LMGoal(output, LMMetadata(true_prob, temp), obj_desc_article, object, object_handle))
            i +=1
        
        print("number of versions: " + str(total_count +1))
        high_temp_results = num_results -1
        if (total_count < high_temp_results): #still under desired number of 3 total resulets
            extra_count = 0 #try another 5 times..
            while ((extra_count < 4) and total_count < high_temp_results):
                extra_count += 1
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages = message, 
                    max_tokens=50,
                    top_p=1,
                    temperature=1.0,
                    stop=["(END RESULT)", ".", "Steps:", "\n"])
                out = response.choices[0]
                out2 = out['message']
                output = out2['content'].strip()
                #logprob = out['logprobs']
                output =output.replace(".", "")
                true_prob = 0.7#STANDIN cannot get mean probability from GPT-4
                if math.isnan(true_prob):
                    i +=1
                    continue;
                duplicate_detected = False
                output1 = self.replace_object(output.lower(), handle, handle)
                for result in result_prob_list:
                    if output1 == result.sentence.lower():
                        duplicate_detected = True
                        break;
                if duplicate_detected:
                    print("Still Duplicate detected! for : " + output)
                    i +=1
                    continue;
                if self.duplicate_with_prior(output1):
                    print("Prior Duplicate detected! for : " + output)
                    i +=1
                    continue;
                total_count += 1
                result_prob_list.append(LMGoal(output, LMMetadata(true_prob, 1.0), obj_desc_article, object, object_handle))

        return result_prob_list;

    def get_LM_Goal(self, input_prompt, temp, num_results, obj_desc_article, object, object_handle):
        text = input_prompt
        best_n = num_results +1 

        handle = object.object
        temp0_result = self.get_LM_Goal_temp0(input_prompt, obj_desc_article, object, object_handle)
        result_prob_list = []
        if not self.duplicate_with_prior(temp0_result.sentence):
            result_prob_list.append(temp0_result)

        prompt_type = "initial_prompt"
        print(handle)
        print("Prompt: (temp " + str(temp) + ")")
        print(text)
        response = openai.Completion.create(
            engine="text-davinci-002", 
            prompt=text, 
            max_tokens=50,
            top_p=1,
            temperature=temp,
            n=best_n,
		    best_of=best_n+2,
            logprobs=1,
            stop=["(END RESULT)", ".", "Steps:", "\n"])
        
        i = 0
        
        total_count = 0
        out = response.choices[0]
        self.print_token_cost(out, response, object_handle, prompt_type)
        while (i < best_n and total_count < (best_n-2)):
            out = response.choices[i]
            output = out['text'].strip()
            logprob = out['logprobs']
            output =output.replace(".", "")
            
            true_prob = self.get_goal_probability(logprob)
            if math.isnan(true_prob):
                i +=1
                continue;
            #result_prob_list.append(LMOutput(output,true_prob))
            output1 = self.replace_object(output.lower(), handle, handle)
            duplicate_detected = False
            for result in result_prob_list:
                if output1 == result.sentence.lower():
                    duplicate_detected = True
            if duplicate_detected:
                print("Duplicate detected! for : " + output)
                i +=1
                continue;
            if self.duplicate_with_prior(output1):
                print("Prior Duplicate detected! for : " + output)
                #What happens if empty due to duplicates with prior?
                i +=1
                continue;

            total_count += 1
            result_prob_list.append(LMGoal(output, LMMetadata(true_prob, temp), obj_desc_article, object, object_handle, prompt_type))
            i +=1
        
        print("number of versions: " + str(total_count +1))
        high_temp_results = num_results -1
        if (total_count < high_temp_results): #still under desired number of 3 total resulets
            extra_count = 0 #try another 5 times..
            while ((extra_count < 4) and total_count < high_temp_results):
                extra_count += 1
                print("Prompt: (temp 1.0)")
                print(text)
                response = openai.Completion.create(
                    engine="text-davinci-002", 
                    prompt=text, 
                    max_tokens=50,
                    top_p=1,
                    temperature=1.0,
                    logprobs=1,
                    stop=["(END RESULT)", ".", "Steps:", "\n"])
                out = response.choices[0]
                self.print_token_cost(out, response, object_handle, prompt_type)
                output = out['text'].strip()
                logprob = out['logprobs']
                output =output.replace(".", "")
                true_prob = self.get_goal_probability(logprob)
                if math.isnan(true_prob):
                    i +=1
                    continue;
                duplicate_detected = False
                output1 = self.replace_object(output.lower(), handle, handle)
                for result in result_prob_list:
                    if output1 == result.sentence.lower():
                        duplicate_detected = True
                        break;
                if duplicate_detected:
                    print("Still Duplicate detected! for : " + output)
                    i +=1
                    continue;
                if self.duplicate_with_prior(output1):
                    print("Prior Duplicate detected! for : " + output)
                    i +=1
                    continue;
                total_count += 1
                result_prob_list.append(LMGoal(output, LMMetadata(true_prob, 1.0), obj_desc_article, object, object_handle, prompt_type))

        #do sort here? no after
        return result_prob_list;
    
    def has_response(self):
        if self.messages:
            return True;
        return False

    def get_next_response(self):
        if self.messages:
            message = self.messages.pop(0)
            return message
        return "";

#new function
    def get_ordered_LM_Goals(self, prompt, temperature, num_results, obj_desc_article, object, object_handle, initial_recurse, repair):
        #For template-based prompting (high temperature results) #TBP Section 3 of paper
        #results = self.get_LM_Goal(prompt, temperature, num_results, obj_desc_article, object, object_handle)
        
        results = self.get_LM_Goal_tree_temp0(prompt, num_results, obj_desc_article, object, object_handle, repair, initial_recurse, "", [])
        results.sort(key=attrgetter('metadata'), reverse=True)
        print("Final ordered output for " + obj_desc_article + ":")
        order = 1
        for res in results:
            prob = res.metadata.probability
            print(str(res) + " (" + str(prob) + ")")
            res.metadata.prob_order = order
            res.prob_order = order
            res.complete_prompt_text = prompt + res.sentence + "(END RESULT)\nResponse: Ok.\nSteps:\n"
            #res.complete_prompt_text = prompt + res.sentence + "(END RESULT)\nSteps:\n" 
            order += 1
        
        return results

    def parse_request(self, id, template, goal, location, object, task_handle, object_handle):

        prompt = self.construct_prompt(object, template, goal, location)

        lmid = id + "LMP1"
        lm_prompt = LMPrompt(lmid, prompt, object, goal, location)

        article = "a "
        if object.object[0] in ('a', 'e', 'i', 'o', 'u'):
            article = "an "
        article = ""
        obj_desc_article = article + object.object + " " + object.preposition + " the " + object.location
        print("task object :" + obj_desc_article)

        temperature = 0.9
        num_results = 3
        results = self.get_ordered_LM_Goals(prompt, temperature, num_results, obj_desc_article, object, object_handle, 0, False)

        lmr = LMResponse(id,lm_prompt, task_handle, obj_desc_article, object, object_handle, results)
        self.lm_responses.append(lmr)
        self.current_response = lmr
        self.dupes = []

        return lmr;

    def expand_step(self, expand):
        new_prompt = expand.complete_prompt_text
        step_number = 1
        if isinstance(expand, LMTaskStepCompletion):
            step_number = expand.step_number + 1
        expand.steps = self.get_LM_Actions(new_prompt, expand, step_number, 0.9, 2)
        return

    def get_evaluation_response(self, evaluation):
        response = "Response: No. "
        type = evaluation.type
        #Eventually support multiple issues in single response
        
        if type == "incorrect":
            # Response: No. Incorrect.
            response += "That goal is wrong."#"Incorrect."
        elif type == "affordance2":
            # Response: No. Bin cannot be closed./on/opened/ [or not closable openable...]
            #look at clause/state?
            response += evaluation.object.capitalize() + " cannot be " + evaluation.affordance + "."
        elif type == "affordance1":
            # Response: No. Bin is not closable./grabbable/receptacle?
            response += evaluation.object.capitalize() + " is not a " + evaluation.affordance + "."
        elif type == "affordance3":
            # Response: No. Bin is not closable./grabbable/receptacle?
            response += evaluation.object.capitalize() + " is not " + evaluation.affordance + "."
        #seperate rules for in/onrelation [instead of receptacle grabbable...]
        elif type == "affordance-relation1":
            # Response: No. Trash cannot be in something./on something/etc...
            #alt Response: No. Trash is not a grabbable object" 
            response += evaluation.object.capitalize() + " cannot be " + evaluation.relation + " in something."
        elif type == "affordance-relation2":
            # Response: No. Something cannot be in a counter./grabbable/receptacle?
            #alt Response: No. Counter is not a surface/container" 
            response += "Something cannot be " + evaluation.relation + " a " + evaluation.object.capitalize() + "."
        elif type == "unknown-word":
            # Response: No. Unknown word trash.
            response += "Unknown word " + evaluation.word + "."
        elif type == "ungrounded-obj1":
            # Response: No. Cannot see a trash.
            response += "Cannot see a " + evaluation.object + "."
        elif type == "uninterpreted":
            # Response: No. Cannot see a trash.
            response += "Could not understand. Rephrase."
        return response

    def get_retry_example(self, type):
        rosie_home = os.environ["ROSIE_HOME"]
        dir = rosie_home + "/python/rosie/lm/examples/"
        filename = dir + "example_" + type + ".txt"
        #goal only version
        #filename = dir + "goal_example_" + type + ".txt"
        prompt_example = "TASK)"
        prompt_example += get_str_from_file(filename)
        prompt_example += "(END TASK)"

        return prompt_example
    def retry_node(self, expand, evaluation):
        new_prompt = ""

        #if this is a second consecutive retry, don't include past correction
        if expand.new_retry:

            #Recreate prompt from scratch (using parents)?
            result_split = expand.complete_prompt_text.split("(RESULT)")
            length = len(result_split)
            #print(length)
            start = length -2
            end = length -1
            first_part= ""
            second_part =""
            first_part = first_part.join(result_split[0:start])
            second_part = second_part.join(result_split[end:length])
            new_prompt =  first_part + "(RESULT)" + second_part
            #print("NEW PROMPT:::::\n")
            #print(new_prompt)
        else:
            new_prompt = expand.complete_prompt_text


        #stitch in new example
        prompt_split = new_prompt.split("TASK)", 2)
        #replacing first example (...(END

        new_example = self.get_retry_example(evaluation.type)

        prompt = prompt_split[0] + new_example + prompt_split[2]
        prompt = prompt.rsplit("Response: Ok.\nSteps:", 1)[0]
        
        response = self.get_evaluation_response(evaluation)

        step_number = 0
        type = ""
        if isinstance(expand, LMTaskStepCompletion):
            step_number = expand.step_number
            type = "step"
            #print("Retrying action")
            expand.steps = self.get_LM_Actions(prompt, expand, step_number, 0.9, 2)
        if isinstance(expand, LMGoal):
            type = "goal"
            prompt += response + "\n(RESULT)"

            #print("Retrying goal\n")
            #print(prompt)
            temperature = 0.9
            num_results = 3
            expand.retrying = True
            expand.retry_goals = self.get_ordered_LM_Goals(prompt, temperature, num_results, expand.obj_desc, expand.object, expand.object_handle, 1, True)
            for goal in expand.retry_goals:
                goal.new_retry = True
                if expand.new_retry:
                    #this is a second attempty at retry
                    goal.second_retry = True
                self.current_response.add_node(goal)

        return

    def get_node(self, node_id):
        #print("num nodes:" + str(len(self.current_response.nodes)))
        for node in self.current_response.nodes:
            #print(node)
            #print(node.id)
            if node.id == node_id:
                return node
        print("Node not found!!!!!")
        return None

    def duplicate_with_prior(self, output1):
        if not self.current_response:
            return False
        for node in self.current_response.nodes:
            if node.sentence.lower() == output1:
                return True

        return False


    def parse_selection_request(self, selection_options, goal, location_desc, object):

        #prompt = self.construct_prompt(object, template, goal, location)
        obj_descriptions= object.object + " " + object.preposition + " " + object.location
        template = self.get_selection_example()

        template += self.get_task_template(goal, location_desc, obj_descriptions)
        #could use current response vs. rebuilding?
        template += "Question: Which goal is the most reasonable for " + obj_descriptions + "?\n"
        i = 1
        print("Selection probabilities (ordered):")
        for option in selection_options:
            opt = option.sentence
            optprob = option.probability
            print(opt + " " + str(optprob))

            template += str(i) + ". " + opt.capitalize() + ".\n"
            i += 1
        template += "Answer:"
        print("Selection Prompt:")
        print(template)

        response = openai.Completion.create(
            engine="text-davinci-002",
            prompt=template,
            max_tokens=1,
            top_p=1,
            temperature=0.0,
            logprobs=2,
            stop=["(TASK)", ".", "\n"])
        
        out = response.choices[0]
    
        output = out['text'].strip()	
        #can get log probs of whole thing?
        output =output.replace(".", "")
        logprob = out['logprobs']
        prob = logprob['token_logprobs'][0]
        #zzzzzzzzzzzzz
        token_prob = logprob['top_logprobs'][0] #might not need/want this...
        sorted_tokens = sorted(token_prob.items(), key=operator.itemgetter(1), reverse=True)
        first_token = sorted_tokens[0][0]
        first_token_prob = np.exp(float(sorted_tokens[0][1]))
        second_token = sorted_tokens[1][0]
        second_token_prob = np.exp(float(sorted_tokens[1][1]))
        #print(prob)
        print("Selection Response:")
        print(output)

        token_stats = response['usage']
        tokens_prompt = token_stats['prompt_tokens']
        tokens_completion = token_stats['completion_tokens']
        tokens_total = token_stats['total_tokens']
        print("selection_prompt;" + object.handle + ";" + str(tokens_prompt) + ";" + str(tokens_completion) + ";" + str(tokens_total) + "\n")

        true_prob = np.exp(float(prob))
        print("Prob of selection: " + str(true_prob))
        print(first_token + " with " + str(first_token_prob))
        print(second_token + " with " + str(second_token_prob))
        response = int(output)
        choice = selection_options[response-1]
        sent_choice = choice.sentence
        print(sent_choice)
        node = LMSelection(response, sent_choice, LMMetadata(true_prob, 0.0, "lm"), object)
        return node


###################################
# Code for using Selection strategy  (Paper Section 4.3)
#######################################
    def parse_selection_request_GPT4(self, selection_options, goal, location_desc, object):
        obj_descriptions= object.object + " " + object.preposition + " " + object.location
        template = self.get_selection_example_GPT4()

        template += self.get_task_template_GPT4(goal, location_desc, obj_descriptions)
        #could use current response vs. rebuilding?
        template += "Question: Which is the most reasonable goal for " + obj_descriptions + "?\n"
        i = 1
        print("Selection probabilities (ordered):")
        for option in selection_options:
            opt = option.sentence
            optprob = option.probability
            print(opt + " " + str(optprob))

            template += str(i) + ". " + opt.capitalize() + ".\n"
            i += 1
        template += "Answer:"
        #time.sleep(1)
        print("Selection Prompt:")
        print(template)
        #STARS* giving extra context
        # message = [
        #     {"role": "system", "content": "Assume that dishware on the table or counter are dirty. Assume that bottles and cans are empty. Non-perishable food belongs in the pantry. Doors should be closed."},
        #     {"role": "user", "content": template}
        # ]
        # message = [
        #     {"role": "user", "content": template}
        # ]
        message = [
            {"role": "system", "content": "Respond with a single integer."},
            {"role": "user", "content": template}
        ]
        no_success = True
        try_count = 1
        while(no_success and try_count < 10):
            try:
                response = openai.ChatCompletion.create(
                    model="gpt-4",
                    messages = message,
                    max_tokens=1,
                    top_p=1,
                    temperature=0.0)
                no_success = False
                try_count += 1
                break
            except:
                print("Error from GPT! Try :" + str(try_count))
                try_count += 1
                time.sleep(try_count)
                no_success = True
                continue
            
        out = response.choices[0]
        out2 = out['message']
        output = out2['content'].strip()

        output =output.replace(".", "")

        print("Selection Response:")
        print(output)

        token_stats = response['usage']
        tokens_prompt = token_stats['prompt_tokens']
        tokens_completion = 0
        if output:
            tokens_completion = token_stats['completion_tokens']
        tokens_total = token_stats['total_tokens']
        print("selection_prompt;" + object.handle + ";" + str(tokens_prompt) + ";" + str(tokens_completion) + ";" + str(tokens_total) + "\n")

        #Defaulting probability to 1 right now, lacking that info for GPt-4
        true_prob = 1.0#np.exp(float(prob))
        
        response = int(output)
        choice = selection_options[response-1]
        sent_choice = choice.sentence
        print(sent_choice)
        node = LMSelection(response, sent_choice, LMMetadata(true_prob, 0.0, "lm"), object)
        return node
    
    def expand_lm_query(self, expand_id, node_id):
        
        node = self.get_node(node_id)
        
        print("Expanding: ")
        print(node)
        if node is None:
            print("Error, trying to expand incremental prompt request")
            return None
        self.expand_step(node)
        self.update_node = node
        return node

    def retry_lm_query(self, expand_id, node_id, message_type, evaluation):
        node = self.get_node(node_id)
        
        print("Retrying: ")
        print(node)
        if node is None:
            print("Error, trying to retry with response incremental prompt request")
            return None
        self.retry_node(node, evaluation)
        self.update_node = node
        return node

    def expand_lm_query_user(self, expand_id, type):

        sentence = expand_id.FindByAttribute("sentence", 0).GetValueAsString()
        sentence = sentence.capitalize()
        node = None
        object = self.current_response.object
        #current response not valid if happening later...[repeat...]
        if (type == "goal"):
            node = LMGoal(sentence, LMMetadata(1.0, 0.0, "human"), self.current_response.obj_desc, self.current_response.object, self.current_response.object_handle)
            #Recreating prompt text here
            node.complete_prompt_text = self.current_response.prompt.text + sentence + "(END RESULT)\nResponse: Ok.\nSteps:\n"
            self.current_response.add_node(node)
            self.current_response.goals.append(node)
            
            #hard to append actions as missing action before step, but maybe not necessary...

        if (type == "step"):
            print("getting to step")
            step_number = 1
            #if after step set step number to increment by 1
            if (self.update_node is not None and isinstance(self.update_node, LMTaskStepCompletion)):
                step_number = self.update_node.step_number + 1
            
            complete_prompt = self.update_node.complete_prompt_text + str(step_number) + ". " + sentence + "\n"
            
            node = LMTaskStepCompletion(sentence, step_number, object, LMMetadata(1.0, 0.0, "human"), complete_prompt)
            #node.complete_prompt_text = self.current_response.prompt.text + sentence + "(END RESULT)\nSteps:\n"
            self.current_response.add_node(node)
            #self.current_response.goals.append(node)
        
        print(node)
        if node is None:
            print("Error, trying to expand incremental prompt request")
            return None
        self.expand_step(node)
        self.update_node = node
        return node