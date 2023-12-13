""" Used by the Rosie Agent to print information about the tasks Rosie is performing """
import sys
import traceback

from string import digits
from rosie.tools import task_to_str
from pysoarlib import AgentConnector

from .LLMHandler import LLMHandler
from .LMReponse import ObjectDescription, LMEvaluation, SentenceProbability
#from rosie.events import LMMessageSent
from rosie.tools import object_to_simple_str
from rosie.tools import object_to_str

class LMConnector(AgentConnector):
    def __init__(self, client):
        AgentConnector.__init__(self, client)
        #self.client.add_agent_param("report-tasks-to-output-link", "true")
        self.add_output_command("lm-request")
        self.add_output_command("lm-request-step")
        self.add_output_command("lm-request-retry")
        self.add_output_command("lm-selection-request")
        self.lm = LLMHandler()
        self.current_response = None
        self.old_responses = []
        self.update_node = None
        self.update_id = None
        #self.next_message_id = 1
        self.lm_id = None
        self.tried = False
        self.requires_update = True
        self.failsafe = 0

        self.responses_to_remove = set()

    #def on_init_soar(self):
    #    pass

    def on_init_soar(self):
        if self.current_response != None:
            self.current_response.remove_from_wm()
            self.current_response = None
        if self.lm_id != None:
            self.lm_id.DestroyWME()
            self.lm_id = None
        if self.update_node != None:
            self.update_node.remove_from_wm()
            self.update_node = None

    # def on_input_phase(self, input_link):
    #     pass

    def on_input_phase(self, input_link):
        if self.lm_id == None:
            self.lm_id = input_link.CreateIdWME("language-model")

        if self.current_response != None and not self.current_response.is_added():
            self.current_response.add_to_wm(self.lm_id)


        if self.update_node != None and self.requires_update: # and not self.update_node.is_added():
            # if self.update_node.is_added():
            #     print("was already added")
            self.requires_update = False

            self.update_id = self.lm_id.CreateIdWME("update-node")
            self.update_node._add_to_wm_impl(self.update_id)
            
            #self.update_node.update_wm(self.lm_id)
            #self.update_node.add_to_wm_(self.lm_id)
            #self.update_id = self.lm_id.CreateSharedIdWME("update-node", self.update_node.identifier)
            #self.update_node.update_wm(self.update_id)
            # for step in self.update_node.steps:
            #     for completion in step.completions:
            #         self.current_response.nodes.append(completion)

        for rsp in self.responses_to_remove:
            rsp.remove_from_wm()
        if len(self.responses_to_remove) > 0:
            self.responses_to_remove = set()


    def on_output_event(self, command_name, root_id):
        if command_name == "lm-request":
            if self.current_response != None:
                #JK2023 fix here for repeat actions..
                print("Cleaning output!")
                #gets deleted here...
                print(self.current_response)
                self.current_response.remove_from_wm() #want to keep around? #JK2023 removed
                #self.old_responses.append(self.current_response) #Make this configurable? (may just always do this...)
                self.current_response = None
            if self.update_node != None:
                #JK2023 fix here for repeat actions..
                print(self.update_node)
                print("Cleaning output update-node!")
                self.update_node.remove_from_wm() #want to keep around? #removed for repeat actions #put back if not storing..
                self.update_node = None
                self.requires_update = True            
            if self.update_id != None:
               print("Cleaning update id!")
               #self.update_id.remove_from_wm()
               self.update_id.DestroyWME()
               self.update_id = None
            self.process_lm_request(root_id)
        if command_name == "lm-request-step":
            self.process_lm_request_step(root_id)
        if command_name == "lm-request-retry":
            # expand_wme = root_id.FindByAttribute("current-node", 0)  
            # if expand_wme:
            #     next_id = expand_wme.ConvertToIdentifier()
            #     retry_attempt = next_id.FindByAttribute("retry-attempt", 0).GetValueAsString()
            #     if retry_attempt == "false" and self.update_node != None:
            #         print(self.update_node)
            #         print("Cleaning output update-node in retry!")
            #         self.update_node.remove_from_wm() #want to keep around? #JK2023 removed for repeat actions #put back if not storing..
            #         self.update_node = None
            #         self.requires_update = True
            #     if retry_attempt == "false" and self.update_id != None:
            #         print("Cleaning update id in retry")
            #         self.update_id.DestroyWME()
            #         self.update_id = None
            self.process_lm_request_retry(root_id)
        if command_name == "lm-selection-request":
            if self.update_node != None:
                #JK2023 fix here for repeat actions..
                print(self.update_node)
                print("Cleaning output update-node in selection!")
                self.update_node.remove_from_wm() #want to keep around? #JK2023 removed for repeat actions #put back if not storing..
                self.update_node = None
                self.requires_update = True            
            if self.update_id != None:
               print("Cleaning update id in selection")
               self.update_id.DestroyWME()
               self.update_id = None
            self.process_lm_selection_request(root_id)

    def get_evaluation(self, next_id):
        affordance = ""
        relation = ""
        object = ""
        type = ""
        word = ""
        interpreted = True
        #print(next_id)
        eval_id = next_id.GetChildId("evaluation")

        #0. inccorect 
        unknown_wme = eval_id.FindByAttribute("human-input", 0)
       
        if unknown_wme:
            word = unknown_wme.GetValueAsString()
            type = "incorrect"
            return LMEvaluation(interpreted, type, object, affordance, word, relation)

        #1. Uninterpreted
        if eval_id.FindByAttribute("interpreted", 0).GetValueAsString() == "false":
            interpreted = False
            type = "uninterpreted"
            return LMEvaluation(interpreted, type, object, affordance, word, relation)

        #2. Unknown word
        unknown_wme = eval_id.FindByAttribute("unknown-word", 0)
       
        if unknown_wme:
            word = unknown_wme.GetValueAsString()
            type = "unknown-word"
            return LMEvaluation(interpreted, type, object, affordance, word, relation)

        #3. Ungrounded object
        unground_id = eval_id.GetChildId("ungrounded-object")
        if unground_id:
            #print(object_to_str(unground_id))
            #print("\n")
            object = object_to_simple_str(unground_id)
            #print(object)
            #object = unground_id.FindByAttribute("root-category", 0).GetValueAsString().strip("1")
            type = "ungrounded-obj1"
            return LMEvaluation(interpreted, type, object, affordance, word, relation)

        #4. Mismatched affordance
        afford_id = eval_id.GetChildId("affordance-mismatch")
        if afford_id:
            object = afford_id.FindByAttribute("obj-desc", 0).GetValueAsString().strip("1")
            type = afford_id.FindByAttribute("type", 0).GetValueAsString()
            relation_wme =afford_id.FindByAttribute("relation", 0)
            if relation_wme:
                relation = relation_wme.GetValueAsString()
            property_wme = afford_id.FindByAttribute("property", 0)
            if property_wme:
                affordance = property_wme.GetValueAsString()

        evaluation = LMEvaluation(interpreted, type, object, affordance, word, relation)

        return evaluation
 
    def process_lm_request_retry(self, root_id):
        message_type = root_id.FindByAttribute("type", 0).GetValueAsString()
        if not message_type:
            root_id.CreateStringWME("status", "error")
            root_id.CreateStringWME("error-info", "lm-request-retry has no type")
            self.client.print_handler("LMConnector: Error - lm-request-retry has no type")
            return

        if self.failsafe > 1500:
            print("HIT FAIL SAFE stop running language model")
            root_id.CreateStringWME("status", "complete")
            return
        self.failsafe += 1
        print("PROCESSING LM-RETRY-REQUEST")

        expand_wme = root_id.FindByAttribute("current-node", 0)

        #clean this up
        if not expand_wme or expand_wme is None:
            #current node not found
            print("lm-request-retry node not found")
            root_id.CreateStringWME("status", "complete")
            return

        #print(expand_wme.GetValueAsString())
        next_id = None   
        if expand_wme:
            next_id = expand_wme.ConvertToIdentifier()
        
        wme_id = next_id.FindByAttribute("id", 0)

        node_id = wme_id.ConvertToIntElement().GetValue()
        #print(node_id)

        evaluation = self.get_evaluation(next_id)

        #for goals
        if message_type == "retry-goal":
            self.update_node = self.lm.retry_lm_query(next_id, node_id, message_type, evaluation)
            #self.update_node = self.lm.expand_lm_query(next_id, node_id)
        self.requires_update = True
        root_id.CreateStringWME("status", "complete")
        return

    def process_lm_request_step(self, root_id):
        message_type = root_id.FindByAttribute("type", 0).GetValueAsString()
        if not message_type:
            root_id.CreateStringWME("status", "error")
            root_id.CreateStringWME("error-info", "lm-request-step has no type")
            self.client.print_handler("LMConnector: Error - lm-request-step has no type")
            return

        task_handle = ""
        task = root_id.FindByAttribute("task-handle", 0)
        if task:
            task_handle = task.GetValueAsString()

        if self.failsafe > 1500:
            print("HIT FAIL SAFE stop running language model")
            root_id.CreateStringWME("status", "complete")
            return
        self.failsafe += 1
        print("PROCESSING LM-STEP-REQUEST")
        #template = root_id.FindByAttribute("template", 0).GetValueAsString()
        expand_wme = root_id.FindByAttribute("current-step", 0)

        post_proc = root_id.FindByAttribute("post-processing", 0)
        obj_handle = root_id.FindByAttribute("object-handle", 0).GetValueAsString()
        if post_proc:
            if self.current_response.object_handle != obj_handle:
                #need to reset current-response and update node
                print("updating to old node...")
                print(obj_handle)
                for resp in self.old_responses:
                    if resp.object_handle == obj_handle:
                        self.old_responses.append(self.current_response)
                        self.current_response = resp #restore old one..
                        self.update_node = None
                        self.requires_update = True
                        self.lm.current_response = self.current_response
                        break
            #need to find 
        if self.current_response.object_handle != obj_handle:
            print("FATAL error current object does not match!!")
        print("updated to " + self.current_response.object_handle)
        #clean this up
        if not expand_wme or expand_wme is None: #deal with this second...
            expand_wme = root_id.FindByAttribute("user-step", 0)
            if expand_wme:
                print("After instructor input") #do this second...
                next_id = expand_wme.ConvertToIdentifier()
                type = next_id.FindByAttribute("type", 0).GetValueAsString()
                self.update_node = self.lm.expand_lm_query_user(next_id, type)
                self.requires_update = True
            root_id.CreateStringWME("status", "complete")
            return

        #print(expand_wme.GetValueAsString())
        next_id = None   
        if expand_wme:
            next_id = expand_wme.ConvertToIdentifier()
        #print(next_id.GetValueAsString())

        wme_id = next_id.FindByAttribute("id", 0)

        node_id = wme_id.ConvertToIntElement().GetValue()
        print(node_id)
        self.update_node = self.lm.expand_lm_query(next_id, node_id)
        self.requires_update = True
        root_id.CreateStringWME("status", "complete")
        return


    def process_lm_selection_request(self, root_id):
        if self.failsafe > 1500:
            print("HIT FAIL SAFE stop running language model")
            return
        self.failsafe += 1

        #root_wme = root_id.GetValueAsString()
        goal = root_id.FindByAttribute("task-goal", 0).GetValueAsString()
        goal = goal.replace('.', '')
        goal = goal.lower()

        selection_options = []
        options_id = root_id.GetChildId("options")
        opt_ids = options_id.GetAllChildIds('option')
        for opt_id in opt_ids: #may want to order by probability or atleast alphanumeric? (so consistent results...)
            sent = opt_id.GetChildString('sentence')
            prob = opt_id.GetChildFloat('probability')
            sentprob = SentenceProbability(sent,prob)
            selection_options.append(sentprob)
        
        selection_options.sort()

        #key=attrgetter('probability'), reverse=True)
        #selection_options = self.get_selection_options(options)

        
        location = root_id.GetChildId("robot-location")
        
        location_desc = object_to_simple_str(location)

        object = self.current_response.object

        #self.update_node = self.lm.parse_selection_request(selection_options, goal, location_desc, object)# task_handle, object_handle)
        self.update_node = self.lm.parse_selection_request_GPT4(selection_options, goal, location_desc, object)# task_handle, object_handle)
        self.requires_update = True
        root_id.CreateStringWME("status", "complete")
        return

    def process_lm_request(self, root_id):
        message_type = root_id.FindByAttribute("type", 0).GetValueAsString()
        if not message_type:
            root_id.CreateStringWME("status", "error")
            root_id.CreateStringWME("error-info", "lm-request has no type")
            self.client.print_handler("LMConnector: Error - lm-request has no type")
            return
        
        task_handle = ""
        task = root_id.FindByAttribute("task-handle", 0)
        if task:
            task_handle = task.GetValueAsString()

        print("task_handle: " + task_handle + "\n")
        print("message_type: " + message_type + "\n")

        if message_type == "get-next-goal": #change not to just directly piggy back o this message type?
            print("PROCESSING LM-REQUEST")
            
            #print("Root id:" + root_id.GetValueAsString())
            root_wme = root_id.GetValueAsString()
            template = root_id.FindByAttribute("template", 0).GetValueAsString()
            goal = root_id.FindByAttribute("task-goal", 0).GetValueAsString()
            goal = goal.replace('.', '')
            goal = goal.lower()
            
            location = root_id.GetChildId("robot-location")
            
            location_desc = object_to_simple_str(location)
            obj = root_id.GetChildId("object")
            #object = objects.GetAllChildIds("object")

            obj_wme = obj.GetChildId("obj")
            obj_on = obj.GetChildId("obj-on")
            object_handle = root_id.FindByAttribute("object-handle", 0).GetValueAsString()
            
            #obj_description = object_to_simple_str(obj_wme)
            #for IJCAI submission use root-category which contains hyphenated name
            obj_description = obj_wme.FindByAttribute("root-category", 0).GetValueAsString()
            obj_description  = obj_description.replace("1", "")
            
            prep = obj.FindByAttribute("prep", 0).GetValueAsString()
            prep = prep.replace("1", "")
            if obj_on:
                obj_on_desc = object_to_simple_str(obj_on)
            else:
                obj_on_desc = ""
            object = ObjectDescription(obj_description,obj_on_desc, prep, object_handle)

            print(goal + ", " + template)

            if self.failsafe > 1500:
                print("HIT FAIL SAFETY limit stop running language model")
                return
            self.failsafe += 1
            
            self.current_response = self.lm.parse_request(root_wme, template, goal, location_desc, object, task_handle, object_handle)

            root_id.CreateStringWME("status", "complete")
            return
