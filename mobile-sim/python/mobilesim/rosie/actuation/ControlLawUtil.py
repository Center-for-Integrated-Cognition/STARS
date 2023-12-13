import time
current_time_us = lambda: int(time.time() * 1000000)

from pysoarlib import *

from mobilesim.lcmtypes import condition_test_t, control_law_t, typed_value_t

class ControlLawUtil:
    # Given a python value type (int, float, str)
    # wrap the value in a typed_value_t
    def make_typed_value(v):
        tv = typed_value_t()
        if v is None:
            tv.type = typed_value_t.TYPE_INT
            tv.value = "0"
        elif type(v) is int:
            tv.type = typed_value_t.TYPE_INT
            tv.value = str(v)
        elif type(v) is float:
            tv.type = typed_value_t.TYPE_DOUBLE
            tv.value = str(v)
        elif type(v) is bool:
            tv.type = typed_value_t.TYPE_BOOL
            tv.value = str(v)
        elif type(v) is str and (v == 'true' or v == 'false'):
            tv.type = typed_value_t.TYPE_BOOL
            tv.value = v
        elif type(v) is str:
            tv.type = typed_value_t.TYPE_STRING
            tv.value = v
        else:
            print("ControlLawUtil.make_typed_value ERROR:")
            print("  Unrecognized value type: " + str(type(v)))
            tv.type = typed_value_t.TYPE_STRING
            tv.value = "ERROR"
        return tv

    # Given a WME, wrap it's value in a typed_value_t, making sure the types match
    # (e.g. int, double, bool, string)
    def make_typed_value_from_wme(wme):
        tv = typed_value_t()
        if wme is None:
            tv.type = typed_value_t.TYPE_INT
            tv.value = "0"
        else:
            val_type = wme.GetValueType()
            if val_type == 'int':
                tv.type = typed_value_t.TYPE_INT
            elif val_type == 'double':
                tv.type = typed_value_t.TYPE_DOUBLE
            elif wme.GetValueAsString().lower() in ['true', 'false']:
                tv.type = typed_value_t.TYPE_BOOL
            else:
                tv.type = typed_value_t.TYPE_STRING
            tv.value = wme.GetValueAsString()
        return tv

    # Creates an empty control law with the given name (no parameters or termination condition)
    def create_empty_control_law(name):
        cl = control_law_t()
        cl.utime = current_time_us()
        cl.id = -1
        cl.name = name
        cl.num_params = 0
        cl.param_names = []
        cl.param_values = []
        cl.termination_condition = ControlLawUtil.parse_condition_test(None)
        return cl

    # Creates a control law with the given name and parameters, and optional condition test
    def create_control_law(name, params, cond_test=None):
        cl = control_law_t()
        cl.utime = current_time_us()
        cl.id = -1
        cl.name = name
        cl.num_params = len(params)
        cl.param_names = []
        cl.param_values = []
        for k, v in params.items():
            cl.param_names.append(k)
            cl.param_values.append(ControlLawUtil.make_typed_value(v))
        cl.termination_condition = cond_test if cond_test is not None else ControlLawUtil.create_empty_condition_test()
        return cl

    # Given the root identifier for a soar output command representing a control law, 
    #   parses the control law and returns an LCM type with the proper info
    def parse_control_law(root_id):
        cl = control_law_t()
        cl.utime = current_time_us()
        cl.id = -1

        # Name of the condition test
        cl.name = root_id.GetChildString("name")
        if cl.name is None:
            print("parse_control_law Error: No ^name attribute")
            return None

        # Parameters of the control law
        params = ControlLawUtil.parse_parameters(root_id, "parameters")
        cl.num_params = len(params)
        cl.param_names = list(params.keys())
        cl.param_values = [ params[name] for name in cl.param_names ]

        # Termination condition - when to stop
        term_id = root_id.GetChildId("termination-condition")
        cl.termination_condition = ControlLawUtil.parse_condition_test(term_id)
        if cl.termination_condition is None:
            print("parse_control_law Error: Invalid termination condition")
            return None

        return cl

    def create_empty_condition_test(name=None):
        ct = condition_test_t()
        ct.name = name if name is not None else "NONE"
        ct.num_params = 0
        ct.param_names = []
        ct.param_values = []
        ct.compare_type = condition_test_t.CMP_EQ
        ct.compared_value = ControlLawUtil.make_typed_value(None)
        return ct


    def parse_condition_test(cond_id):
        ct = condition_test_t()

        # Null condition test
        if cond_id is None:
            return ControlLawUtil.create_empty_condition_test("NONE")

        # Name of the condition test
        ct.name = cond_id.GetChildString("name")
        if ct.name is None:
            print("parse_control_law Error: No ^name attribute on condition test")
            return None

        # Parameters of the condition
        params = ControlLawUtil.parse_parameters(cond_id, "parameters")
        ct.num_params = len(params)
        ct.param_names = list(params.keys())
        ct.param_values = [ params[name] for name in ct.param_names ]

        # compare-type
        #   The type of comparison (gt, gte, eq, lte, lt)

        compare_type = cond_id.GetChildString("compare-type")
        if compare_type is None:
            ct.compare_type = condition_test_t.CMP_GT
        elif compare_type == "gt":
            ct.compare_type = condition_test_t.CMP_GT
        elif compare_type == "gte":
            ct.compare_type = condition_test_t.CMP_GTE
        elif compare_type == "eq":
            ct.compare_type = condition_test_t.CMP_EQ
        elif compare_type == "lte":
            ct.compare_type = condition_test_t.CMP_LTE
        elif compare_type == "lt":
            ct.compare_type = condition_test_t.CMP_LT
        else:
            print("Unknown compare-type on condition test")
        

        # compared-value
        #   the value being compared against when evaluating the test
        comp_val_wme = cond_id.FindByAttribute("compared-value", 0)
        if comp_val_wme is not None and len(comp_val_wme.GetValueAsString()) > 0:
            ct.compared_value = ControlLawUtil.make_typed_value_from_wme(comp_val_wme)
        else:
            ct.compared_value = ControlLawUtil.make_typed_value(None)
        return ct

    def parse_parameters(id, att):
        params = {}
        params_id = id.GetChildId(att)
        if params_id is not None:
            for i in range(params_id.GetNumberChildren()):
                wme = params_id.GetChild(i)
                name = wme.GetAttribute()
                params[name] = ControlLawUtil.make_typed_value_from_wme(wme)

        return params
